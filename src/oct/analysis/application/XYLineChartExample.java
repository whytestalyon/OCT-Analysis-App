package oct.analysis.application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * This program demonstrates how to draw XY line chart with XYDataset using
 * JFreechart library.
 *
 * @author www.codejava.net
 *
 */
public class XYLineChartExample extends JFrame {

    final JPanel chartPanel;

    public XYLineChartExample() {
        super("XY Line Chart Example with JFreechart");

        chartPanel = createChartPanel(false);
        add(chartPanel, BorderLayout.CENTER);

        setSize(640, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public JPanel createChartPanel(boolean addNew) {
        String chartTitle = "Objects Movement Chart";
        String xAxisLabel = "X";
        String yAxisLabel = "Y";

        XYDataset dataset = createDataset(addNew);

        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
                xAxisLabel, yAxisLabel, dataset);
        XYSplineRenderer renderer = new XYSplineRenderer(10);
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesVisible(1, true);
        chart.getXYPlot().setRenderer(renderer);

        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(200, 200));
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    public XYDataset createDataset(boolean addNewData) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series1 = new XYSeries("Object 1");
        XYSeries series2 = new XYSeries("Object 2");
        XYSeries series3 = new XYSeries("Object 3");

        series1.add(1.0, 2.0);
        series1.add(2.0, 3.0);
        series1.add(3.0, 2.5);
        series1.add(3.5, 2.8);
        series1.add(4.2, 6.0);

        series2.add(2.0, 1.0);
        series2.add(2.5, 2.4);
        series2.add(3.2, 1.2);
        series2.add(3.9, 2.8);
        series2.add(4.6, 3.0);

        series3.add(1.2, 4.0);
        series3.add(2.5, 4.4);
        series3.add(3.8, 4.2);
        series3.add(4.3, 3.8);
        series3.add(4.5, 4.0);

        if (addNewData) {
            series3.add(2.2, 4.0);
            series3.add(3.5, 4.4);
            series3.add(4.8, 4.2);
            series3.add(5.3, 3.8);
            series3.add(6.5, 4.0);
        }

        dataset.addSeries(series1);
        dataset.addSeries(series2);
        dataset.addSeries(series3);

        return dataset;
    }

    public static void main(String[] args) throws InterruptedException {
        XYLineChartExample myFrame = new XYLineChartExample();
        SwingUtilities.invokeLater(() -> {
            myFrame.setVisible(true);
        });

//        System.out.println("Removeing old stuff...");
        myFrame.remove(myFrame.chartPanel);
        Thread.sleep(10000);
//        System.out.println("Adding new stuff...");
        myFrame.add(myFrame.createChartPanel(true));
        myFrame.invalidate();
        myFrame.validate();
    }
}
