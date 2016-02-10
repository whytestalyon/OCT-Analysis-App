/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTLine;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.AnalysisMode;
import oct.analysis.application.dat.ImageOperationManager;
import oct.analysis.application.dat.LinePoint;
import oct.analysis.application.dat.OCT;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.OCTMode;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.io.AnalysisSaveState;
import oct.io.TiffReader;
import oct.io.TiffWriter;
import oct.util.ip.BlurOperation;
import oct.util.ip.SharpenOperation;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author Brandon
 */
public class Util {

    public static double parseNumberFromInput(String in) {
        if (in.matches("[0-9]+(\\.[0-9]+)*")) {
            return Double.parseDouble(in);
        } else {
            return -1;
        }
    }

    public static OCT getOCT(BufferedImage octImage, OCTAnalysisUI octAnalysisUI, String octFileName) {
        boolean exit = false;
        //ask the user for the x-scale
        double xscale = 0;
        do {
            String res = JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT X-axis scale (microns per pixel):", "X-Scale input", JOptionPane.QUESTION_MESSAGE);
            if (!(res == null || res.isEmpty())) {
                xscale = Util.parseNumberFromInput(res);
            }
            if (res == null || res.isEmpty() || xscale <= 0) {
                exit = JOptionPane.showConfirmDialog(octAnalysisUI, "Bad scale value. Would you like to enter it again?\nNOTE: OCT won't load without the scale data.", "Input Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION;
            }
        } while (!exit && xscale <= 0);
        if (exit) {
            return null;
        }
        //ask the user for the y-scale
        double yscale = 0;
        do {
            String res = JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT Y-axis scale (microns per pixel):", "Y-Scale input", JOptionPane.QUESTION_MESSAGE);
            if (!(res == null || res.isEmpty())) {
                yscale = Util.parseNumberFromInput(res);
            }
            if (res == null || res.isEmpty() || yscale <= 0) {
                exit = JOptionPane.showConfirmDialog(octAnalysisUI, "Bad scale value. Would you like to enter it again?\nNOTE: OCT won't load without the scale data.", "Input Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION;
            }
        } while (!exit && yscale <= 0);
        if (exit) {
            return null;
        }

        //store values and return OCT object
        OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();
        octMngr.setXscale(xscale);
        octMngr.setYscale(yscale);
        return new OCT(octImage, octFileName);
    }

    public static BufferedImage deepCopyBufferedImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * Get the local maximums from a collection of Points.
     *
     * @param line assumes that the line starts at X = 0
     * @return
     */
    public static LinkedList<LinePoint> getMaximums(List<LinePoint> line) {
        LinkedList<LinePoint> maxPoints = new LinkedList<>();
        ArrayList<LinePoint> pointList = new ArrayList<>(line);
        LinePoint leftPeakPoint = new LinePoint(0, 0);
        int leftPeakPointIndex = 0;
        LinePoint rightPeakPoint = new LinePoint(0, 0);
        int index = -1;
        for (LinePoint point : pointList) {
            index++;
            if (index == 0) {
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                continue;
            }
            if (leftPeakPoint.getY() < point.getY()) {
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                rightPeakPoint = point;
            } else if (leftPeakPoint.getY() == point.getY()) {
                rightPeakPoint = point;
            } else {
                //determine if we are coming down off of a peak by looking two points behind the current point
                if (leftPeakPointIndex > 0) {
                    LinePoint prev = pointList.get(leftPeakPointIndex - 1);
                    //if two points back has a Y value that is less than or equal to the left peak point
                    //then we have found the end of the peak and we can process as such
                    if (prev.getY() <= leftPeakPoint.getY()) {
                        double peakx = (double) rightPeakPoint.getX() - ((double) (rightPeakPoint.getX() - leftPeakPoint.getX()) / 2D);
                        maxPoints.add(new LinePoint((int) Math.round(peakx), leftPeakPoint.getY()));
                    }
                }
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                rightPeakPoint = point;
            }
        }

        return maxPoints;
    }

    /**
     * Get the local maximums from a collection of Points.
     *
     * @param line assumes that the line starts at X = 0
     * @return
     */
    public static LinkedList<LinePoint> getMaximumsWithHiddenPeaks(List<LinePoint> line) {
        LinkedList<LinePoint> maxPoints = new LinkedList<>();

        //convert to x and y coordinate arrays
        double[][] xyline = getXYArraysFromLinePoints(line);

        //use a spline interpolator to converts points into an equation
        UnivariateInterpolator interpolator = new SplineInterpolator();
        UnivariateFunction function = interpolator.interpolate(xyline[0], xyline[1]);

        // create a differentiator using 5 points and 0.01 step
        FiniteDifferencesDifferentiator differentiator
                = new FiniteDifferencesDifferentiator(5, 0.01);

        // create a new function that computes both the value and the derivatives
        // using DerivativeStructure
        UnivariateDifferentiableFunction completeF = differentiator.differentiate(function);

        // now we can compute the value and its derivatives
        // here we decided to display up to second order derivatives,
        // because we feed completeF with order 2 DerivativeStructure instances
        //find local minima in second derivative, these indicate the peaks (and hidden peaks)
        //of the input
        for (double x = xyline[0][0] + 1; x < xyline[0][xyline[0].length - 1] - 1; x += 0.5) {
            DerivativeStructure xDSc = new DerivativeStructure(1, 2, 0, x);
            DerivativeStructure xDSl = new DerivativeStructure(1, 2, 0, x - 0.5);
            DerivativeStructure xDSr = new DerivativeStructure(1, 2, 0, x + 0.5);
            DerivativeStructure yDSc = completeF.value(xDSc);
            DerivativeStructure yDSl = completeF.value(xDSl);
            DerivativeStructure yDSr = completeF.value(xDSr);
            double c2d = yDSc.getPartialDerivative(2);
            if (c2d < yDSl.getPartialDerivative(2) && c2d < yDSr.getPartialDerivative(2)) {
                maxPoints.add(new LinePoint((int) Math.round(x), yDSc.getValue()));
            }
        }

        return maxPoints;
    }

    public static List<LinePoint> findMaxAndMins(List<LinePoint> line) {
        //create list of all positive Y values to get peaks
        ArrayList<LinePoint> convList = new ArrayList<>(line.size());
        line.forEach(p -> {
            convList.add(new LinePoint(p.getX(), Math.abs(p.getY())));
        });
        //find X values of peaks
        List<LinePoint> peaks = getMaximums(convList);
        //collect peak points
        List<LinePoint> ret = line.parallelStream()
                .filter(p -> peaks.stream().anyMatch(pk -> pk.getX() == p.getX()))
                .collect(Collectors.toList());
        //sort by X position
        ret.sort(Comparator.comparingInt(peak -> peak.getX()));
        return ret;
    }

    public static List<LinePoint> findPeaksAndVallies(List<LinePoint> line) {
        //first find peaks
        List<LinePoint> peaks = getMaximums(line);
        //create inverse of line to find vallies
        ArrayList<LinePoint> convList = new ArrayList<>(line.size());
        line.forEach(p -> {
            convList.add(new LinePoint(p.getX(), 0D - p.getY()));
        });
        //find X values of vallies
        List<LinePoint> vallies = getMaximums(convList);
        //collect valley points
        List<LinePoint> ret = line.parallelStream()
                .filter(p -> vallies.stream().anyMatch(pk -> pk.getX() == p.getX()))
                .collect(Collectors.toList());
        //sort by X position
        ret.addAll(peaks);
        ret.sort(Comparator.comparingInt(peak -> peak.getX()));
        return ret;
    }

    public static void graphPoints(List<LinePoint>... pointsList) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        int seriesCntr = 1;
        for (List<LinePoint> points : pointsList) {
            XYSeries data = new XYSeries("Series " + seriesCntr);
            points.forEach((point) -> {
                data.add(point.getX(), point.getY());
            });
            dataset.addSeries(data);
            seriesCntr++;
        }

        JFrame graphFrame = new JFrame("Points graph");

        JPanel chartPanel = createChartPanel("Points graph", dataset);
        graphFrame.add(chartPanel, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> {
            graphFrame.pack();
            graphFrame.setVisible(true);
        });
    }

    public static Line mergeLines(Line... lines) {
        int minx = Arrays.stream(lines).flatMap(l -> l.stream()).mapToInt(p -> p.x).min().getAsInt();
        int maxx = Arrays.stream(lines).flatMap(l -> l.stream()).mapToInt(p -> p.x).max().getAsInt();

        Line mLine = new Line(1024);
        IntStream.rangeClosed(minx, maxx)
                .forEachOrdered(x -> {
                    double avgY = Arrays.stream(lines).flatMap(l -> l.stream()).filter(p -> p.x == x).mapToInt(p -> p.y).average().getAsDouble();
                    mLine.add(new Point(x, (int) Math.round(avgY)));
                });
        return mLine;
    }

    public static void graphLines(List<Line> lines, boolean invert_y, int imgHeight) {
        graphLines(lines, invert_y, imgHeight, "Plotted Lines");
    }

    public static void graphLines(List<Line> lines, boolean invert_y, int imgHeight, String title) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        int seriesCntr = 1;
        for (Line line : lines) {
            XYSeries data = new XYSeries("Series " + seriesCntr);
            line.forEach(point -> {
                if (invert_y) {
                    data.add(point.getX(), imgHeight - point.getY());
                } else {
                    data.add(point.getX(), point.getY());
                }
            });
            dataset.addSeries(data);
            seriesCntr++;
        }

        JFrame graphFrame = new JFrame(title);

        JPanel chartPanel = createChartPanel("Plotted Lines", dataset);
        chartPanel.setPreferredSize(new Dimension(800, 800));
        graphFrame.add(chartPanel, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> {
            graphFrame.pack();
            graphFrame.setVisible(true);
        });
    }

    private static JPanel createChartPanel(String title, XYDataset dataset) {
        String xAxisLabel = "X";
        String yAxisLabel = "Y";

        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset);
        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(640, 480));
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    /**
     * Determine the gray scale value of a pixel based on its RGB value.
     *
     * @param rgb
     * @return
     */
    public static int calculateGrayScaleValue(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        int grayLevel = (r + g + b) / 3;
        return grayLevel;
    }

    /**
     * Determine the RGB value of a pixel based on its gray scale value.
     *
     * @param gsv
     * @return
     */
    public static int calculateRGBValue(int gsv) {
        return (gsv << 16) | (gsv << 8) | gsv;
    }

    /**
     * Convert the supplied image to a 2D pixel array such that an (X,Y) value
     * indexes as array[x][y].
     *
     * Credit for this method goes to Stack Overflow user Mota and their post
     * here:
     * http://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
     * for this implementation.
     *
     * This method will return the red, green and blue values directly for each
     * pixel, and if there is an alpha channel it will add the alpha value.
     * Using this method is harder in terms of calculating indices, but is much
     * faster than using getRGB to build this same array.
     *
     * @param image
     * @return
     */
    public static int[][] convertTo2D(BufferedImage image) {

        final int width = image.getWidth();
        final int height = image.getHeight();

        int[][] result = new int[width][height];
        for (int x = 0; x < result.length; x++) {
            for (int y = 0; y < result[x].length; y++) {
                result[x][y] = image.getRGB(x, y);
            }
        }

        return result;
    }

    public static double[][] getXYArraysFromPoints(List<Point> points) {
        double[] x = new double[points.size()];
        double[] y = new double[points.size()];
        ListIterator<Point> pi = points.listIterator();
        for (int i = 0; pi.hasNext(); i++) {
            Point p = pi.next();
            x[i] = p.getX();
            y[i] = p.getY();
        }
        return new double[][]{x, y};
    }

    /**
     *
     * @param points
     * @return
     */
    public static double[][] getXYArraysFromLinePoints(List<LinePoint> points) {
        double[] x = new double[points.size()];
        double[] y = new double[points.size()];
        ListIterator<LinePoint> pi = points.listIterator();
        for (int i = 0; pi.hasNext(); i++) {
            LinePoint p = pi.next();
            x[i] = p.getX();
            y[i] = p.getY();
        }
        return new double[][]{x, y};
    }

    public static AnalysisSaveState getAnalysisSaveState() {
        OCTAnalysisManager analysisMngr = OCTAnalysisManager.getInstance();
        SelectionLRPManager selMngr = SelectionLRPManager.getInstance();
        ImageOperationManager imageOperationMngr = ImageOperationManager.getInstance();

        //create save object
        AnalysisSaveState saveObj = new AnalysisSaveState();

        //populate the save object
        /*
         Analysis info
         */
        AnalysisMode analysisMode = analysisMngr.getAnalysisMode();
        saveObj.setAnalysisMode(analysisMode);
        /*
         Selection info
         */
        List<OCTLine> lineSegs = selMngr.getSelections()
                .stream()
                .filter(sel -> sel instanceof OCTLine)
                .map(sel -> (OCTLine) sel)
                .collect(Collectors.toList());
        List<OCTSelection> selSegs = selMngr.getSelections()
                .stream()
                .filter(sel -> !(sel instanceof OCTLine))
                .collect(Collectors.toList());
        saveObj.setLineSegs(lineSegs);
        saveObj.setSelSegs(selSegs);
        int selectionWidth = selMngr.getSelectionWidth();
        int lrpSmoothingFactor = selMngr.getLrpSmoothingFactor();
        saveObj.setSelectionWidth(selectionWidth);
        saveObj.setLrpSmoothingFactor(lrpSmoothingFactor);
        /*
         OCT dispaly panel info
         */
        Point drawPoint = analysisMngr.getImgPanel().getDrawPoint();
        LinkedList<List<LinePoint>> linesToDraw = analysisMngr.getImgPanel().getLinesToDraw();
        boolean drawLines = analysisMngr.getImgPanel().isDrawLines();
        boolean drawSelections = analysisMngr.getImgPanel().isDrawSelections();
        boolean drawScaleBars = analysisMngr.getImgPanel().isShowScaleBars();
        saveObj.setDrawPoint(drawPoint);
        saveObj.setLinesToDraw(linesToDraw);
        saveObj.setDrawLines(drawLines);
        saveObj.setDrawScaleBars(drawSelections);
        saveObj.setDrawSelections(drawSelections);
        /*
         OCT and OCT analysis manager data
         */
        double xscale = analysisMngr.getXscale();
        double yscale = analysisMngr.getYscale();
        int micronsBetweenSelections = analysisMngr.getMicronsBetweenSelections();
        OCT oct = analysisMngr.getOct();
        String octFileName = oct.getFileName();
        OCTMode displayMode = analysisMngr.getDisplayMode(); //default display mode of image is assumed to be a Log OCT image
        int foveaCenterXPosition = analysisMngr.getFoveaCenterXPosition();
        saveObj.setXscale(xscale);
        saveObj.setYscale(yscale);
        saveObj.setMicronsBetweenSelections(micronsBetweenSelections);
        saveObj.setLogOCT(TiffWriter.writeTiffImageToByteArray(oct.getLogOctImage()));
        saveObj.setOctFileName(octFileName);
        saveObj.setDisplayMode(displayMode);
        saveObj.setFoveaCenterXPosition(foveaCenterXPosition);
        /*
         image operations
         */
        BlurOperation blur = imageOperationMngr.getBlur();
        SharpenOperation sharp = imageOperationMngr.getSharp();
        saveObj.setBlurFactor(blur.getBlurFactor());
        saveObj.setSharpenSigma(sharp.getSharpenSigma());
        saveObj.setSharpenWeight(sharp.getSharpenWeight());

        return saveObj;

    }

    public static void openSavedAnalysis(OCTAnalysisUI ui, AnalysisSaveState saveObj) throws IOException {
        OCTAnalysisManager analysisMngr = OCTAnalysisManager.getInstance();
        SelectionLRPManager selMngr = SelectionLRPManager.getInstance();
        ImageOperationManager imageOperationMngr = ImageOperationManager.getInstance();
        //clear out any old selection and LRP data
        selMngr.removeSelections(true);

        //restore the analysis and update UI to reflect analysis settings
        /*
         OCT and OCT analysis manager data
         */
        analysisMngr.setXscale(saveObj.getXscale());
        analysisMngr.setYscale(saveObj.getYscale());
        analysisMngr.setMicronsBetweenSelections(saveObj.getMicronsBetweenSelections());
        if (saveObj.getLogOCT() != null) {
            OCT oct = new OCT(TiffReader.readTiffImage(saveObj.getLogOCT()), saveObj.getOctFileName());
            analysisMngr.setOct(oct);
        }
        OCTMode dispmode = saveObj.getDisplayMode();
        analysisMngr.setOCTMode(dispmode);
        analysisMngr.setFoveaCenterXPosition(saveObj.getFoveaCenterXPosition());

        /*
         Analysis info
         */
        analysisMngr.setAnalysisMode(saveObj.getAnalysisMode());
        /*
         Selection info
         */
        selMngr.setLrpSmoothingFactor(saveObj.getLrpSmoothingFactor());
        selMngr.setSelectionWidth(saveObj.getSelectionWidth());
        /*
         OCT dispaly panel info
         */
        analysisMngr.getImgPanel().setDrawPoint(saveObj.getDrawPoint());
        if (saveObj.getLinesToDraw() != null) {
            saveObj.getLinesToDraw().forEach(analysisMngr.getImgPanel()::addDrawnLine);
        }
        if (saveObj.isDrawLines()) {
            analysisMngr.getImgPanel().showLines();
        } else {
            analysisMngr.getImgPanel().hideLines();
        }
        if (saveObj.isDrawSelections()) {
            analysisMngr.getImgPanel().showSelections();
        } else {
            analysisMngr.getImgPanel().hideSelections();
        }
        if (saveObj.isDrawScaleBars()) {
            analysisMngr.getImgPanel().showScaleBars();
        } else {
            analysisMngr.getImgPanel().hideScaleBars();
        }

        /*
         image operations
         */
        imageOperationMngr.updateBlurOperation(new BlurOperation(saveObj.getBlurFactor()));
        imageOperationMngr.updateSharpenOperation(new SharpenOperation(saveObj.getSharpenSigma(), saveObj.getSharpenWeight()));

        /*
         selections
         */
        selMngr.addOrUpdateSelections(saveObj.getSelSegs());
        saveObj.getLineSegs().forEach(selMngr::addOrUpdateSelection);

        /*
         render OCT (and other objects) to the screen
         */
        if (saveObj.getLogOCT() != null) {
            OCT oct = analysisMngr.getOct();
            analysisMngr.getImgPanel().setSize(new Dimension(oct.getImageWidth(), oct.getImageHeight()));
        }
        analysisMngr.getImgPanel().repaint();
        ui.validate();
        ui.pack();

        /*
         update UI with analysis settings
         */
        if (dispmode == OCTMode.LINEAR) {
            ui.getLinearOCTModeButton().setSelected(true);
        }
        ui.getLrpSmoothingSlider().setValue(selMngr.getLrpSmoothingFactor());
        ui.getLrpWidthTextField().setValue(selMngr.getSelectionWidth());
        ui.getScaleBarCheckBox().setSelected(analysisMngr.getImgPanel().isShowScaleBars());
        ui.getDispSegmentationCheckBox().setSelected(analysisMngr.getImgPanel().isDrawLines());
        ui.getDispSelectionsCheckBox().setSelected(analysisMngr.getImgPanel().isDrawSelections());
        ui.getOctSharpRadiusSlider().setValue((int) Math.round(imageOperationMngr.getSharp().getSharpenSigma() * 10D));
        ui.getOctSharpWeightSlider().setValue((int) Math.round(imageOperationMngr.getSharp().getSharpenWeight() * 100F));
        ui.getOctSmoothingSlider().setValue((int) Math.round(imageOperationMngr.getBlur().getBlurFactor() * 10D));
        if (analysisMngr.getAnalysisMode() != null) {
            ui.enableAnalysisTools();
        }
    }
}
