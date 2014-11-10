/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JPanel;
import oct.analysis.application.dat.OCT;
import oct.analysis.application.dat.OCTAnalysisManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
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

    public static final int FOVEAL_SELECTION = 0;
    public static final int PERIPHERAL_SELECTION = 1;
    public static final int SMOOTHING_FACTOR = 5; // smoothing: the strength of the smoothing filter; 1=no change, larger values smoothes more
    private final String selectionName;
    private final int selectionType;
    private int panel_x_position;
    private int panel_y_position;
    private final int width;
    private final int height;

    public OCTSelection(int panel_x_position, int panel_y_position, int width, int height, int selectionType, String selectionName) {
        this.panel_x_position = panel_x_position;
        this.panel_y_position = panel_y_position;
        this.width = width;
        this.height = height;
        this.selectionType = selectionType;
        this.selectionName = selectionName;
    }

    public void drawSelection(Graphics g) {
        g.setColor(Color.green);
        System.out.println("Drawing selection at x: " + panel_x_position + ", y: " + panel_y_position + ", w: " + width + ", h: " + (height - 1));
        g.drawRect(panel_x_position, panel_y_position, width, height - 1);
    }

    public int getPanel_x_position() {
        return panel_x_position;
    }

    public int getPanel_y_position() {
        return panel_y_position;
    }

    public void setPanel_x_position(int panel_x_position) {
        this.panel_x_position = panel_x_position;
    }

    public void setPanel_y_position(int panel_y_position) {
        this.panel_y_position = panel_y_position;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSelectionType() {
        return selectionType;
    }

    public String getSelectionName() {
        return selectionName;
    }

    public JPanel createLRPPanel() {
        //create the series collection from the LRP data
        XYSeriesCollection lrp = new XYSeriesCollection();
        lrp.addSeries(getLrpSeriesFromOCT(OCTAnalysisManager.getInstance().getOct()));
        lrp.addSeries(getLrpPeaks(lrp.getSeries(0)));
        //create chart panel for LRP
        JFreeChart chart = ChartFactory.createXYLineChart(lrp.getSeriesKey(0).toString(), "Pixel Height", "Avg. Pixel Intensity", lrp, PlotOrientation.HORIZONTAL, false, true, false);
        XYPlot plot = chart.getXYPlot();
        plot.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);
        plot.getDomainAxis().setInverted(true);
        //set up rendering principles
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShapesFilled(1, true);
        plot.setRenderer(renderer);
        //make panel
        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(200, 200));
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    public XYSeries getLrpSeriesFromOCT(OCT oct) {
        XYSeries lrp = new XYSeries(selectionName + " LRP");
        lrp.setKey(selectionName);
        int value = Integer.MAX_VALUE;
        //iterate over each row of pixels in the selection area and calculate average pixel intensity
        for (int y = height - 1; y >= 0; y--) {
            int sum = 0;

            for (int xindex = panel_x_position - oct.getImageOffsetX() + 1; xindex < panel_x_position - oct.getImageOffsetX() + 1 + width; xindex++) {
                sum += calculateGrayScaleValue(oct.getOctImage().getRGB(xindex, y));
            }
            //calculate average pixel intensity
            int avg = sum / width;
            //smooth the LRP to provide a higher quality LRP signal
            if (value == Integer.MAX_VALUE) {
                //initialize the first value for the smoothing filter
                value = avg;
            } else {
                //smooth the LRP signal
                value += ((avg - value) / SMOOTHING_FACTOR);
            }
            //add LRP value to return series
            lrp.add(y, value);
        }

        return lrp;
    }

    /**
     * Determine the gray scale value of a pixel based on its RGB value.
     *
     * @param rgb
     * @return
     */
    private int calculateGrayScaleValue(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);

        int grayLevel = (r + g + b) / 3;
        return grayLevel;
    }

    public XYSeries getLrpPeaks(XYSeries lrpSeries) {
        XYSeries lrpMaxPoints = new XYSeries(selectionName + " LRP maximums");
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
}
