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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
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

    public static OCT getOCT(BufferedImage octImage, OCTAnalysisUI octAnalysisUI) {
        Object[] options = {"I have the scale!", "I have axial length and scan width!"};
        int n = JOptionPane.showOptionDialog(octAnalysisUI, "We need to know the scale of the OCT. What information do you have?", "Determine OCT Scale...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        switch (n) {
            case JOptionPane.YES_OPTION:
                double scale = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT scale (microns per pixel):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                OCTAnalysisManager.getInstance().setScale(scale);
                return new OCT(octImage);
            case JOptionPane.NO_OPTION:
                double nominalScanWidth = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT nominal scan length(millimeter):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                double axialLength = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT scale (millimeter):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                OCTAnalysisManager.getInstance().setScale(axialLength, nominalScanWidth, octImage.getWidth());
                return new OCT(octImage);
            default:
                break;
        }
        return null;
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
        graphFrame.add(chartPanel, BorderLayout.SOUTH);
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

        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;

        int[][] result = new int[width][height];
        if (hasAlphaChannel) {
            final int pixelLength = 4;
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
                argb += ((int) pixels[pixel + 1] & 0xff); // blue
                argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
                result[col][row] = argb;
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final int pixelLength = 3;
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += -16777216; // 255 alpha
                argb += ((int) pixels[pixel] & 0xff); // blue
                argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
                result[col][row] = argb;
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
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
        saveObj.setDrawPoint(drawPoint);
        saveObj.setLinesToDraw(linesToDraw);
        saveObj.setDrawLines(drawLines);
        saveObj.setDrawSelections(drawSelections);
        /*
         OCT and OCT analysis manager data
         */
        double scale = analysisMngr.getScale();
        int micronsBetweenSelections = analysisMngr.getMicronsBetweenSelections();
        OCT oct = analysisMngr.getOct();
        OCTMode displayMode = analysisMngr.getDisplayMode(); //default display mode of image is assumed to be a Log OCT image
        int foveaCenterXPosition = analysisMngr.getFoveaCenterXPosition();
        saveObj.setScale(scale);
        saveObj.setMicronsBetweenSelections(micronsBetweenSelections);
        saveObj.setLogOCT(TiffWriter.writeTiffImageToByteArray(oct.getLogOctImage()));
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

        //restore the analysis
        /*
         OCT and OCT analysis manager data
         */
        analysisMngr.setScale(saveObj.getScale());
        analysisMngr.setMicronsBetweenSelections(saveObj.getMicronsBetweenSelections());
        OCT oct = new OCT(TiffReader.readTiffImage(saveObj.getLogOCT()));
        analysisMngr.setOct(oct);
        analysisMngr.setOCTMode(saveObj.getDisplayMode());
        analysisMngr.setFoveaCenterXPosition(saveObj.getFoveaCenterXPosition());
        
        /*
         Analysis info
         */
        analysisMngr.setAnalysisMode(saveObj.getAnalysisMode());
        /*
         Selection info
         */
        selMngr.setLrpSmoothingFactor(saveObj.getLrpSmoothingFactor());
        System.out.println("lrp sf: " + selMngr.getLrpSmoothingFactor());
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
        analysisMngr.getImgPanel().setSize(new Dimension(oct.getImageWidth(), oct.getImageHeight()));
        analysisMngr.getImgPanel().repaint();
        ui.validate();
        ui.pack();
    }
}
