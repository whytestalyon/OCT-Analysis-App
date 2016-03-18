/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.lrp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import oct.analysis.application.comp.HighlightXYRenderer;
import oct.analysis.application.dat.OCTAnalysisManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class LRPPanel extends ChartPanel {

    private final XYSeries lrpSeries, maximaSeries, hiddenMaximaSeries;
    private List<XYSeries> fwhmSeries;
    private final XYSeriesCollection graphData = new XYSeriesCollection();
    private final OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();

    public LRPPanel(String title, int xPosOnOct, XYSeries lrpSeries, XYSeries maximaSeries, XYSeries hiddenMaximaSeries, List<XYSeries> fwhmSeries) {
        super(null);
        this.lrpSeries = lrpSeries;
        this.maximaSeries = maximaSeries;
        this.hiddenMaximaSeries = hiddenMaximaSeries;
        this.fwhmSeries = fwhmSeries;

        //add series data to graph
        graphData.addSeries(lrpSeries);
        graphData.addSeries(maximaSeries);
        graphData.addSeries(hiddenMaximaSeries);
        fwhmSeries.forEach(fwhms -> {
            graphData.addSeries(fwhms);
        });

        //create the chart for displaying the data
        setChart(ChartFactory.createXYLineChart(title, "Pixel Height", "Reflectivity", graphData, PlotOrientation.HORIZONTAL, false, true, false));

        //create a custom renderer to control the display of each series
        //set draw properties for the LRP data
        HighlightXYRenderer renderer = new HighlightXYRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesPaint(0, Color.RED);

        //set draw properties for the maxima data
        renderer.setDrawOutlines(true);
        renderer.setUseOutlinePaint(true);
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShapesFilled(1, true);
        renderer.setSeriesPaint(1, Color.BLUE);

        //set draw properties for the hidden maxima data
        renderer.setDrawOutlines(true);
        renderer.setUseOutlinePaint(true);
        renderer.setSeriesLinesVisible(2, false);
        renderer.setSeriesShapesVisible(2, true);
        renderer.setSeriesShapesFilled(2, true);
        renderer.setSeriesPaint(2, Color.MAGENTA);

        //set draw properties for each of the full-width half-max lines
        for (int i = 3; i < fwhmSeries.size() + 3; i++) {
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesPaint(i, Color.BLACK);
        }

        //add listener for highlighting points on the graph when the mouse hovers over them
        addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseClicked(ChartMouseEvent cme) {
                //do nothing for now
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {
                ChartEntity entity = cme.getEntity();
                if (!(entity instanceof XYItemEntity)) {
                    renderer.setHighlightedItem(-1, -1);
                    octMngr.getImgPanel().clearDrawnPoint();
                } else {
                    XYItemEntity xyent = (XYItemEntity) entity;
                    renderer.setHighlightedItem(xyent.getSeriesIndex(), xyent.getItem());
                    int y = octMngr.getImgPanel().getHeight() - ((int) Math.round(xyent.getDataset().getXValue(xyent.getSeriesIndex(), xyent.getItem())));
                    octMngr.getImgPanel().setDrawPoint(new Point(xPosOnOct, y));
                }
            }
        });

        getChart().getXYPlot().setRenderer(renderer);

        //configure initial display setting for the panel
        setPreferredSize(new Dimension(200, 200));
        setFillZoomRectangle(true);
        setMouseWheelEnabled(true);
    }

    public void setLrpSeries(XYSeries lrpSeries) {
        this.lrpSeries.clear();
        ((List<XYDataItem>) (Object) lrpSeries.getItems()).forEach(item -> {
            this.lrpSeries.add(item, false);
        });
        this.lrpSeries.fireSeriesChanged();
    }

    public void setMaximaSeries(XYSeries maximaSeries) {
        this.maximaSeries.clear();
        ((List<XYDataItem>) (Object) maximaSeries.getItems()).forEach(item -> {
            this.maximaSeries.add(item, false);
        });
        this.maximaSeries.fireSeriesChanged();
    }

    public void setHiddenMaximaSeries(XYSeries hiddenMaximaSeries) {
        this.hiddenMaximaSeries.clear();
        ((List<XYDataItem>) (Object) hiddenMaximaSeries.getItems()).forEach(item -> {
            this.hiddenMaximaSeries.add(item, false);
        });
        this.hiddenMaximaSeries.fireSeriesChanged();
    }

    public void setFwhmSeries(List<XYSeries> fwhmSeries) {
        for (int i = 3; i < this.fwhmSeries.size() + 3; i++) {
            graphData.removeSeries(i);
        }
        this.fwhmSeries = fwhmSeries;
        this.fwhmSeries.forEach(graphData::addSeries);
        //set draw properties of the for each of the full-width half-max lines
        for (int i = 3; i < this.fwhmSeries.size() + 3; i++) {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesPaint(i, Color.BLACK);
            getChart().getXYPlot().setRenderer(i, renderer);
        }
    }

}
