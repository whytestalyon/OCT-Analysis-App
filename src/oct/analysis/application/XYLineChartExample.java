package oct.analysis.application;

import chuiSegmentation.CSegImage;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ListIterator;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import oct.io.TiffReader;
import oct.util.Segmentation;
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

    public XYLineChartExample() throws IOException {
        super("XY Line Chart Example with JFreechart");

        chartPanel = createChartPanel(false);
        add(chartPanel, BorderLayout.CENTER);

        setSize(640, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public JPanel createChartPanel(boolean addNew) throws IOException {
        String chartTitle = "Objects Movement Chart";
        String xAxisLabel = "X";
        String yAxisLabel = "Y";

        XYDataset dataset = createDataset(addNew);

        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
                xAxisLabel, yAxisLabel, dataset);
//        XYSplineRenderer renderer = new XYSplineRenderer(10);
//        renderer.setSeriesLinesVisible(0, true);
//        renderer.setSeriesShapesVisible(0, false);
//        renderer.setSeriesLinesVisible(1, false);
//        renderer.setSeriesShapesVisible(1, true);
//        chart.getXYPlot().setRenderer(renderer);

        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(600, 600));
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    public XYDataset createDataset(boolean addNewData) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series1 = new XYSeries("Object 1");

        BufferedImage img = TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\DiseasedOCTs\\DH_10160_OD_L_7_0_02_529disp_fr66reg_fr47-83_AL24p11.tif"));

        //get the contour of the ILM
        ArrayList<Point> ilmSeg = new ArrayList<>(getSurfaceSegment(img, 1, Segmentation.ILM_SEGMENT));
        //get the contour of the Brooks Membrane
        ArrayList<Point> brmSeg = new ArrayList<>(getSurfaceSegment(img, 1, Segmentation.BrM_SEGMENT));
        //calcualte the difference (in the Y value) between at each point along the X axis for the above contours
        ListIterator<Point> ilmIter = ilmSeg.listIterator();
        ListIterator<Point> brmIter = brmSeg.listIterator();
//        double[] diffs = new double[ilmSeg.size()];
//        Arrays.fill(diffs, 0);
        while(ilmIter.hasNext()) {
            Point ilmPoint = ilmIter.next();
            Point brmPoint = brmIter.next();
            System.out.println(ilmPoint.toString() + "; " + brmPoint.toString() + "; diff: " + brmPoint.distance(ilmPoint));
//            diffs[x] = ilmPoint.distance(brmPoint);
            series1.add(ilmPoint.getX(), brmPoint.distance(ilmPoint));
            
        }

        dataset.addSeries(series1);

        return dataset;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        XYLineChartExample myFrame = new XYLineChartExample();
        SwingUtilities.invokeLater(() -> {
            myFrame.setVisible(true);
        });

//        System.out.println("Removeing old stuff...");
//        myFrame.remove(myFrame.chartPanel);
//        Thread.sleep(10000);
//        System.out.println("Adding new stuff...");
//        myFrame.add(myFrame.createChartPanel(true));
//        myFrame.invalidate();
//        myFrame.validate();
    }

    public static Collection<Point> getSurfaceSegment(BufferedImage image, int distanceBetweenPoints, int segNum) {
        // Only tested with BufferedImage.TYPE_BYPE_GRAY. 
        // It might work with other types but no promises. 
        CSegImage segImg = new CSegImage(image);
        segImg.getFlatImage(false);
        return segImg.getSegments()
                .getCurve(segNum, distanceBetweenPoints)
                .getPointCollection();
    }
}
