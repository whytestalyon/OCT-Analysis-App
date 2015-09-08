/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JPanel;
import oct.analysis.application.dat.LinePoint;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.analysis.application.dat.SelectionType;
import oct.analysis.application.err.OverOCTEdgeException;
import oct.util.Util;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author Brandon
 */
public class OCTSelection {

    /*
     listed as static so selection manager is initialized when selections are deserialized from analysis save file.
     listed as transient to signify to the serializer that the selection manager does not need to be serialized to file
     */
    private static transient final SelectionLRPManager selMngr = SelectionLRPManager.getInstance();
    private static transient final OCTAnalysisManager analysisMngr = OCTAnalysisManager.getInstance();
    protected String selectionName;
    protected SelectionType selectionType;
    protected int xPositionOnOct;
    protected int yPositionOnOct;
    protected int width;
    protected int height;
    protected Polygon topLine;
    protected Polygon bottomLine;
    protected boolean highlighted = false;
    protected boolean drawn = false;
    protected boolean resizable = true;
    protected boolean moveable;

    public OCTSelection(int xPositionOnOct, int yPositionOnOct, int width, int height, SelectionType selectionType, String selectionName, boolean moveable) {
        this.xPositionOnOct = xPositionOnOct;
        this.width = width;
        int leftEdge = getSelectionLeftEdgeCoordinate();
        int rightEdge = getSelectionRightEdgeCoordinate();
        if (leftEdge < 0 || rightEdge >= analysisMngr.getOct().getImageWidth()) {
            throw new OverOCTEdgeException("Selection past OCT edge!");
        }
        this.selectionType = selectionType;
        this.selectionName = selectionName;
        this.moveable = moveable;
        if (height == analysisMngr.getOct().getImageHeight() || height == 0) {
            //calculate the y position and height of selection based on LRP
            List<LinePoint> lrp = getLrpAcrossOCT(analysisMngr.getOctImage());
            int maxReflectivity = lrp
                    .stream()
                    .mapToInt(LinePoint::getX)
                    .max()
                    .getAsInt();
            //window LRP to 20px above and below the points in the LRP where the
            //reflectivity is 65% of the maximum
            //get top of window
            int tmpy = lrp.stream()
                    .filter(p -> p.getX() >= (0.75D * (double) maxReflectivity))
                    .mapToInt(p -> (int) Math.round(p.getY()))
                    .min()
                    .getAsInt() - 20;
            this.yPositionOnOct = (tmpy < 0) ? 0 : tmpy;
            //get bottom of window
            int tmpHeight = lrp.stream()
                    .filter(p -> p.getX() >= (0.75D * (double) maxReflectivity))
                    .mapToInt(p -> (int) Math.round(p.getY()))
                    .max()
                    .getAsInt() - this.yPositionOnOct + 21;
            this.height = (this.yPositionOnOct + tmpHeight >= analysisMngr.getOct().getImageHeight()) ? analysisMngr.getOct().getImageHeight() - this.yPositionOnOct : tmpHeight;
        } else {
            this.yPositionOnOct = yPositionOnOct;
            this.height = height;
        }
        //set the regions that define the top and bottom lines of the drawn selection
        //imageOffsetX + leftEdge, imageOffsetY + yPositionOnOct, imageOffsetX + rightEdge, imageOffsetY + yPositionOnOct
        topLine = new Polygon(new int[]{leftEdge, leftEdge, rightEdge, rightEdge}, new int[]{this.yPositionOnOct - 2, this.yPositionOnOct + 2, this.yPositionOnOct + 2, this.yPositionOnOct - 2}, 4);
        //imageOffsetX + leftEdge, imageOffsetY + yPositionOnOct + height, imageOffsetX + rightEdge, imageOffsetY + yPositionOnOct + height
        bottomLine = new Polygon(new int[]{leftEdge, leftEdge, rightEdge, rightEdge}, new int[]{this.yPositionOnOct + this.height - 2, this.yPositionOnOct + this.height + 2, this.yPositionOnOct + this.height + 2, this.yPositionOnOct + this.height - 2}, 4);
    }

    public void drawSelection(Graphics g, int imageOffsetX, int imageOffsetY) {
        if (highlighted) {
            g.setColor(Color.pink);
        } else {
            g.setColor(Color.green);
        }
        //draw lines arround the area that is the selection
        int leftEdge = getSelectionLeftEdgeCoordinate();
        int rightEdge = getSelectionRightEdgeCoordinate();
//        g.drawRect(imageOffsetX + leftEdge - 1, imageOffsetY + yPositionOnOct - 1, width + 2, height + 2);
        //draw left line
        g.drawLine(imageOffsetX + leftEdge - 1, imageOffsetY + yPositionOnOct, imageOffsetX + leftEdge - 1, imageOffsetY + yPositionOnOct + height - 1);
        //draw right line
        g.drawLine(imageOffsetX + rightEdge + 1, imageOffsetY + yPositionOnOct, imageOffsetX + rightEdge + 1, imageOffsetY + yPositionOnOct + height - 1);
        //draw top line
        g.drawLine(imageOffsetX + leftEdge, imageOffsetY + yPositionOnOct, imageOffsetX + rightEdge, imageOffsetY + yPositionOnOct);
        //draw bottom line
        g.drawLine(imageOffsetX + leftEdge, imageOffsetY + yPositionOnOct + height, imageOffsetX + rightEdge, imageOffsetY + yPositionOnOct + height);
        //draw button for interacting with the selection
        drawSelectButton(g, imageOffsetX, imageOffsetY);
        drawn = true;
    }

    protected void drawSelectButton(Graphics g, int imageOffsetX, int imageOffsetY) {
        Polygon buttonOutline = getSelectionButtonShape();
        buttonOutline.translate(imageOffsetX, imageOffsetY);
        g.setColor(Color.lightGray);
        g.drawPolygon(buttonOutline);
        g.fillPolygon(buttonOutline);
        Polygon button = new Polygon();
        button.addPoint(imageOffsetX + xPositionOnOct - 5, imageOffsetY);
        button.addPoint(imageOffsetX + xPositionOnOct - 5, imageOffsetY + 15);
        button.addPoint(imageOffsetX + xPositionOnOct, imageOffsetY + 20);
        button.addPoint(imageOffsetX + xPositionOnOct + 5, imageOffsetY + 15);
        button.addPoint(imageOffsetX + xPositionOnOct + 5, imageOffsetY);
        g.setColor(Color.DARK_GRAY);
        g.drawPolygon(button);
    }

    public Polygon getSelectionButtonShape() {
        Polygon buttonOutline = new Polygon();
        buttonOutline.addPoint(xPositionOnOct - 6, -1);
        buttonOutline.addPoint(xPositionOnOct - 6, 16);
        buttonOutline.addPoint(xPositionOnOct, 22);
        buttonOutline.addPoint(xPositionOnOct + 6, 16);
        buttonOutline.addPoint(xPositionOnOct + 6, -1);
        return buttonOutline;
    }

    public final int getSelectionLeftEdgeCoordinate() {
        int lineOffset;
        if (width % 2 == 0) {
            //figure edge offset for even width (off-center)
            lineOffset = (width / 2) - 1;
        } else {
            //figure edge offset for odd width (centered)
            lineOffset = (width - 1) / 2;
        }
        return xPositionOnOct - lineOffset;
    }

    public final int getSelectionRightEdgeCoordinate() {
        int lineOffset;
        if (width % 2 == 0) {
            //figure edge offset for even width (off-center)
            lineOffset = (width / 2);
        } else {
            //figure edge offset for odd width (centered)
            lineOffset = (width - 1) / 2;
        }
        return xPositionOnOct + lineOffset;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public int getXPositionOnOct() {
        return xPositionOnOct;
    }

    public int getYPositionOnOct() {
        return yPositionOnOct;
    }

    public void setXPositionOnOct(int xPositionOnOct) {
        if (moveable) {
            this.xPositionOnOct = xPositionOnOct;
        }
    }

    public void setYPositionOnOct(int yPositionOnOct) {
        this.yPositionOnOct = yPositionOnOct;
    }

    public void setWidth(int width) {
        if (resizable) {
            this.width = width;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isDrawn() {
        return drawn;
    }

    public void setDrawn(boolean drawn) {
        this.drawn = drawn;
    }

    public String getSelectionName() {
        return selectionName;
    }

    public SelectionType getSelectionType() {
        return selectionType;
    }

    public boolean isMoveable() {
        return moveable;
    }

    public boolean overlapsBottomOfSelection(Point p) {
        return bottomLine.contains(p);
    }
    
    public boolean overlapsTopOfSelection(Point p) {
        return topLine.contains(p);
    }
    
    public JPanel createLRPPanel() {
        //create the series collection from the LRP data
        XYSeriesCollection lrp = new XYSeriesCollection();
        lrp.addSeries(getLrpSeriesFromOCT(OCTAnalysisManager.getInstance().getOctImage()));
//        System.out.println("Processing graph " + lrp.getSeriesKey(0).toString());
        lrp.addSeries(findMaximums(lrp.getSeries(0), selectionName + " LRP Maximums"));
        List<XYSeries> fwhm = getFWHMForLRPPeaks(lrp.getSeries(1), lrp.getSeries(0));
        fwhm.forEach((fwhmSeries) -> {
            lrp.addSeries(fwhmSeries);
        });
        //create chart panel for LRP
        JFreeChart chart = ChartFactory.createXYLineChart(lrp.getSeriesKey(0).toString(), "Pixel Height", "Reflectivity", lrp, PlotOrientation.HORIZONTAL, false, true, false);
        XYPlot plot = chart.getXYPlot();
//        plot.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);
//        plot.getDomainAxis().setInverted(true);
        //set up rendering principles
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShapesFilled(1, true);
        renderer.setSeriesPaint(1, Color.BLUE);
        for (int i = 2; i < fwhm.size() + 2; i++) {
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesPaint(i, Color.BLACK);
        }
        plot.setRenderer(renderer);
        //make panel
        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(200, 200));
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    public final List<LinePoint> getLrpAcrossOCT(BufferedImage oct) {
        LinkedList<LinePoint> lrp = new LinkedList<>();
        int leftEdge = getSelectionLeftEdgeCoordinate();

        //iterate over each row of pixels across the OCT and calculate average pixel intensity
        for (int y = oct.getHeight() - 1; y >= 0; y--) {
            int yVal = y;
            //calculate average pixel grayscale intensity
            int avgReflectivity = (int) Math.round(Arrays.stream(oct.getRGB(leftEdge, yVal, width, 1, null, 0, width))
                    .map(Util::calculateGrayScaleValue)
                    .average()
                    .getAsDouble());
            //add LRP value to return series
            lrp.add(new LinePoint(avgReflectivity, y));
        }

        return lrp;
    }

    public final XYSeries getLrpSeriesFromOCT(BufferedImage oct) {
        XYSeries lrp = new XYSeries(selectionName + " LRP");
        lrp.setKey(selectionName);

        int leftEdge = getSelectionLeftEdgeCoordinate();

        double value = -1;
        //iterate over each row of pixels in the selection area and calculate average pixel intensity
        for (int y = yPositionOnOct + height - 1; y >= yPositionOnOct; y--) {
            int yVal = y;
            //calculate average pixel grayscale intensity
            double curPixelIntensity = Arrays.stream(oct.getRGB(leftEdge, yVal, width, 1, null, 0, width))
                    .map(Util::calculateGrayScaleValue)
                    .average()
                    .getAsDouble();
            //smooth the LRP to provide a higher quality LRP signal
            if (value <= -1) {
                //initialize the first value for the smoothing filter
                value = curPixelIntensity;
            } else {
                //smooth the LRP signal
                value += ((curPixelIntensity - value) / selMngr.getLrpSmoothingFactor());
            }
            //add LRP value to return series
            lrp.add(oct.getHeight() - y, value);
        }

        return lrp;
    }

    public static XYSeries findMaximums(XYSeries lrpSeries, String title) {
        XYSeries lrpMaxPoints = new XYSeries(title);
        XYDataItem leftPeakPoint = new XYDataItem(0, 0);
        int leftPeakPointIndex = 0;
        XYDataItem rightPeakPoint = new XYDataItem(0, 0);
        boolean first = true;
        int index = -1;
        List<XYDataItem> pointList = (List<XYDataItem>) lrpSeries.getItems();
        for (XYDataItem point : pointList) {
            index++;
            if (first) {
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                first = false;
                continue;
            }
            if (leftPeakPoint.getYValue() < point.getYValue()) {
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                rightPeakPoint = point;
            } else if (leftPeakPoint.getYValue() == point.getYValue()) {
                rightPeakPoint = point;
            } else {
                //determine if we are coming down off of a peak by looking two points behind the current point
                if (leftPeakPointIndex > 0) {
                    XYDataItem prev = pointList.get(leftPeakPointIndex - 1);
                    //if two points back has a Y value that is less than or equal to the left peak point
                    //then we have found the end of the peak and we can process as such
                    if (prev.getYValue() <= leftPeakPoint.getYValue()) {
                        double peakx = rightPeakPoint.getXValue() - ((rightPeakPoint.getXValue() - leftPeakPoint.getXValue()) / 2D);
                        lrpMaxPoints.add(peakx, leftPeakPoint.getY());
                    }
                }
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                rightPeakPoint = point;
            }
        }

        return lrpMaxPoints;
    }

    public static XYSeries findMaxAndMins(XYSeries lrpSeries, String title) {
        XYSeries lrpMaxPoints = new XYSeries("");
        ((List<XYDataItem>) lrpSeries.getItems()).forEach(p -> {
            lrpMaxPoints.add(p.getXValue(), Math.abs(p.getYValue()));
        });
        return findMaximums(lrpMaxPoints, title);
    }

    public List<XYSeries> getFWHMForLRPPeaks(XYSeries lrpPeaks, XYSeries lrpSeries) {
        LinkedList<XYSeries> seriesList = new LinkedList<>();
        List<XYDataItem> pointList = (List<XYDataItem>) lrpSeries.getItems();
        List<XYDataItem> peakList = (List<XYDataItem>) lrpPeaks.getItems();
        //iterate through the peaks, process FWHM for each peak
        for (XYDataItem peak : peakList) {
            //grab index of the closest point to the peak
            int peakIndex = -1;
            for (XYDataItem pnt : pointList) {
                peakIndex++;
                if (Math.abs(pnt.getXValue() - peak.getXValue()) < 0.6D) {
                    break;
                }
            }
            //calculate point with Y value of valley to the left of peak
            XYDataItem leftValleyPoint = null;
            ListIterator<XYDataItem> it = pointList.listIterator(peakIndex);
            double prevY = peak.getYValue();
            while (it.hasPrevious()) {
                XYDataItem leftPoint = it.previous();
                if (leftPoint.getYValue() <= prevY) {
                    prevY = leftPoint.getYValue();
                    leftValleyPoint = leftPoint;
                } else {
                    break;
                }
            }
            //calculate point with Y value of valley to the right of peak
            XYDataItem rightValleyPoint = null;
            it = pointList.listIterator(peakIndex);
            prevY = peak.getYValue();
            while (it.hasNext()) {
                XYDataItem rightPoint = it.next();
                if (rightPoint.getYValue() <= prevY) {
                    prevY = rightPoint.getYValue();
                    rightValleyPoint = rightPoint;
                } else {
                    break;
                }
            }
            //determine half max Y value
            double halfMaxYValue;
            if (rightValleyPoint.getYValue() == leftValleyPoint.getYValue()) {
                halfMaxYValue = peak.getYValue() - ((peak.getYValue() - leftValleyPoint.getYValue()) / 2D);
            } else if (rightValleyPoint.getYValue() > leftValleyPoint.getYValue()) {
                halfMaxYValue = peak.getYValue() - ((peak.getYValue() - rightValleyPoint.getYValue()) / 2D);
            } else {
                halfMaxYValue = peak.getYValue() - ((peak.getYValue() - leftValleyPoint.getYValue()) / 2D);
            }
            //determine the X value on both sides of the peak that corresponds to the half max Y value
            double leftX = pointList.get(0).getXValue(), rightX = pointList.get(pointList.size() - 1).getXValue();
            XYDataItem prevPoint = pointList.get(peakIndex);
            it = pointList.listIterator(peakIndex);
            while (it.hasPrevious()) {
                XYDataItem leftPoint = it.previous();
                if (leftPoint.getYValue() == halfMaxYValue) {
                    leftX = leftPoint.getXValue();
                    break;
                } else {
                    if (leftPoint.getYValue() < halfMaxYValue) {
//                        System.out.println("Left X for peak (" + peak.getXValue() + "," + peak.getYValue() + "): ");
                        leftX = calculateXFromYForLineWithTwoPoints(leftPoint, prevPoint, halfMaxYValue);
//                        System.out.println("    Left X: (" + leftX + "," + halfMaxYValue + "): ");
                        break;
                    } else {
                        prevPoint = leftPoint;
                    }
                }
            }
            prevPoint = pointList.get(peakIndex);
            it = pointList.listIterator(peakIndex);
            while (it.hasNext()) {
                XYDataItem rightPoint = it.next();
                if (rightPoint.getYValue() == halfMaxYValue) {
                    rightX = rightPoint.getXValue();
                    break;
                } else {
                    if (rightPoint.getYValue() < halfMaxYValue) {
//                        System.out.println("Right X for peak (" + peak.getXValue() + "," + peak.getYValue() + "): ");
                        rightX = calculateXFromYForLineWithTwoPoints(rightPoint, prevPoint, halfMaxYValue);
//                        System.out.println("    Right X: (" + leftX + "," + halfMaxYValue + "): ");
                        break;
                    } else {
                        prevPoint = rightPoint;
                    }
                }
            }
            //store the two points for the half max full width line for this peak
            XYSeries peakSeries = new XYSeries("(" + peak.getXValue() + "," + peak.getYValue() + ")FWHM");
            peakSeries.add(leftX, halfMaxYValue);
            peakSeries.add(rightX, halfMaxYValue);
            seriesList.add(peakSeries);
        }
        return seriesList;
    }

    private double calculateXFromYForLineWithTwoPoints(XYDataItem pt1, XYDataItem pt2, double y) {
//        System.out.println("    P1: (" + pt1.getXValue() + "," + pt1.getYValue() + ")");
//        System.out.println("    P2: (" + pt2.getXValue() + "," + pt2.getYValue() + ")");
        //calculate slope 
        double slope = (pt1.getYValue() - pt2.getYValue()) / (pt1.getXValue() - pt2.getXValue());
//        System.out.println("    Slope: " + slope);
        //calculate y value at y-intercept (aka b)
        double yint = pt1.getYValue() - (slope * pt1.getXValue());
//        System.out.println("    Y-int: " + yint);
        //return 
        return (y - yint) / slope;
    }

}
