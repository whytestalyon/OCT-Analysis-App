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
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import oct.io.TiffReader;
import oct.util.Segmentation;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
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
public class FoveaFindingExp extends JFrame {

    final JPanel chartPanel;

    public FoveaFindingExp(String title, BufferedImage img) throws IOException {
        super("XY Line Chart Example with JFreechart");

        chartPanel = createChartPanel(title, img);
        add(chartPanel, BorderLayout.CENTER);

        setSize(640, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public final JPanel createChartPanel(String title, BufferedImage img) throws IOException {
        String xAxisLabel = "Pixel";
        String yAxisLabel = "2nd Derivative";

        XYDataset dataset = createDataset(img);
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset);
        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(640, 480));
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    public XYDataset createDataset(BufferedImage img) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        //get the contour of the ILM
        ArrayList<Point> ilmSeg = new ArrayList<>(getSurfaceSegment(img, 1, Segmentation.ILM_SEGMENT));
        XYSeries ilm = new XYSeries("ILM Segment");
        ilmSeg.forEach((p) -> {
            ilm.add(p.getX(), 480 - p.getY());
        });
//        dataset.addSeries(ilm);
        //get the contour of the Brooks Membrane
        ArrayList<Point> brmSeg = new ArrayList<>(getSurfaceSegment(img, 1, Segmentation.BrM_SEGMENT));
        XYSeries brm = new XYSeries("BrM Segment");
        brmSeg.forEach((p) -> {
            brm.add(p.getX(), 480 - p.getY());
        });
//        dataset.addSeries(brm);
        //calcualte the difference (in the Y value) between at each point along the X axis for the above contours
        ListIterator<Point> ilmIter = ilmSeg.listIterator();
        ListIterator<Point> brmIter = brmSeg.listIterator();
        double[] x = new double[ilmSeg.size()];
        double[] y = new double[ilmSeg.size()];
        XYSeries series1 = new XYSeries("Segment Diff.");
        for (int i = 0; ilmIter.hasNext(); i++) {
            Point ilmPoint = ilmIter.next();
            Point brmPoint = brmIter.next();
            System.out.println(ilmPoint.toString() + "; " + brmPoint.toString() + "; diff: " + brmPoint.distance(ilmPoint));
            double xPos = ilmPoint.getX();
            double yPos = brmPoint.distance(ilmPoint);
            series1.add(xPos, yPos);
            x[i] = xPos;
            y[i] = yPos;
        }
//        dataset.addSeries(series1);

        //build spline function to help find derivaties of diff.
        UnivariateInterpolator interpolator = new LoessInterpolator(0.4, 0);//good for identifying the true foveal center
//        UnivariateInterpolator interpolator = new LoessInterpolator(0.2, 0);//good for removing and identifying relative vicinity of foveal center
        UnivariateFunction function = interpolator.interpolate(x, y);

        //get first and second derivative second derivative of the diff
        FiniteDifferencesDifferentiator differ = new FiniteDifferencesDifferentiator(4, 0.25);//use 8 point differences differentiator (that is it uses 8 points arround the point in question to derive the slope at the given point) 
        UnivariateDifferentiableFunction difFunc = differ.differentiate(function);

        //plot derivatives
        XYSeries interpolated = new XYSeries("Interp.");
        XYSeries fd = new XYSeries("F'");
        XYSeries sd = new XYSeries("F''");
        int params = 1;
        int order = 2;
        DerivativeStructure xd;
        DerivativeStructure yd;

        for (int xRealValue = 1; xRealValue <= x[x.length - 1] - 1; xRealValue++) {
            //get first and second derivative at the given point
            xd = new DerivativeStructure(params, order, 0, xRealValue);
            yd = difFunc.value(xd);
            interpolated.add(xRealValue, yd.getValue());//interpolated difference value
            fd.add(xRealValue, yd.getPartialDerivative(1));//first derivative at point
            sd.add(xRealValue, yd.getPartialDerivative(2));//second derivative at point
        }
//        dataset.addSeries(interpolated);
//        dataset.addSeries(fd);
        dataset.addSeries(sd);

        return dataset;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        //map of input OCTs, some normal some diseased
        HashMap<String, BufferedImage> octMap = new HashMap<>();
        octMap.put("RW_10174_OD_L_7_0", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Normal\\AVG_RW_10174_OD_L_7_0_02_529disp_25frames.tif")));
        octMap.put("RW_10174_OD_L_7_90", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Normal\\AVG_RW_10174_OD_L_7_90_05_529disp_27frames.tif")));
        octMap.put("RW_10174_OS", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Normal\\AVG_RW_10174_OS_Raw_L_90.tif")));
        octMap.put("DH_10160_OD", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\DiseasedOCTs\\DH_10160_OD_L_7_0_02_529disp_fr66reg_fr47-83_AL24p11.tif")));
        octMap.put("JC_10193_OD", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\DiseasedOCTs\\JC_10193_OD_L_7_0_0000002_OCT_600x1000_fr90reg_fr80-100_resized_810x460.tif")));
        octMap.put("KS_10175_OS", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\DiseasedOCTs\\KS_10175_OS_L_7_0_24_529disp_fr38reg_fr28-70_AL24p2.tif")));
        octMap.put("KS_10238_OS", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\DiseasedOCTs\\KS_10238_OS_L_7_90_05_529disp_reg_fr1-25_AL21p35.tif")));
        octMap.put("KS_10243_OD", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\DiseasedOCTs\\KS_10243_OD_L_7_0_02_529disp_reg_fr1-14_AL22p43.tif")));

        for (Map.Entry<String, BufferedImage> ent : octMap.entrySet()) {
            FoveaFindingExp myFrame = new FoveaFindingExp(ent.getKey(), ent.getValue());
            SwingUtilities.invokeLater(() -> {
                myFrame.setVisible(true);
            });
        }

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
