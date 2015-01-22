package oct.analysis.application;

import chuiSegmentation.CSegImage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataItem;
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
        add(chartPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public final JPanel createChartPanel(String title, BufferedImage img) throws IOException {
        String xAxisLabel = "Pixel";
        String yAxisLabel = "Slope";

        XYDataset dataset = createDataset(img, title);
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset);
        XYLineAndShapeRenderer chartRenderer = new XYLineAndShapeRenderer(true, false);
        chartRenderer.setSeriesLinesVisible(3, false);
        chartRenderer.setSeriesShapesVisible(3, true);
        chartRenderer.setSeriesLinesVisible(4, false);
        chartRenderer.setSeriesShapesVisible(4, true);
        chart.getXYPlot().setRenderer(chartRenderer);
        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(640, 480));
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    public XYDataset createDataset(BufferedImage img, String title) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        //get the contour of the ILM
        ArrayList<Point> ilmSeg = new ArrayList<>(getSurfaceSegment(img, 1, Segmentation.ILM_SEGMENT));
        final double[] xi = new double[ilmSeg.size()];
        final double[] yi = new double[ilmSeg.size()];
        final AtomicInteger index = new AtomicInteger(0);
        ilmSeg.forEach((p) -> {
            int i = index.getAndIncrement();
            xi[i] = p.getX();
            yi[i] = p.getY();
        });
        //interpolate ILM contour
        UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
        UnivariateFunction function = interpolator.interpolate(xi, yi);
        //add ILM contour to graph series
        XYSeries ilm = new XYSeries("ILM Segment");
        for (int xPosition = 0; xPosition < img.getWidth(); xPosition++) {
            ilm.add(xPosition, function.value(xPosition));
        }
//        dataset.addSeries(ilm);
        //get the contour of the Brooks Membrane
        ArrayList<Point> brmSeg = new ArrayList<>(getSurfaceSegment(img, 1, Segmentation.BrM_SEGMENT));
        final double[] xb = new double[brmSeg.size()];
        final double[] yb = new double[brmSeg.size()];
        index.set(0);
        brmSeg.forEach((p) -> {
            int i = index.getAndIncrement();
            xb[i] = p.getX();
            yb[i] = p.getY();
        });
        //interpolate BrM contour
        function = interpolator.interpolate(xb, yb);
        //add BrM contour to graph series
        XYSeries brm = new XYSeries("BrM Segment");
        for (int xPosition = 0; xPosition < img.getWidth(); xPosition++) {
            brm.add(xPosition, function.value(xPosition));
        }
//        dataset.addSeries(brm);

        //draw segmentation on image
//        drawSegmentation(img, ilm, brm, title);
        add(drawSegmentation(img, ilm, brm), BorderLayout.NORTH);

        //calcualte the difference (in the Y value) between at each point along the X axis for the above contours
        ListIterator<XYDataItem> ilmIter = ((List<XYDataItem>) ilm.getItems()).listIterator();
        ListIterator<XYDataItem> brmIter = ((List<XYDataItem>) brm.getItems()).listIterator();

        XYSeries segDiff = new XYSeries("Segment Diff.");
        double[] x = new double[brm.getItemCount()];
        double[] y = new double[brm.getItemCount()];
        for (int i = 0; ilmIter.hasNext(); i++) {
            XYDataItem ilmPoint = ilmIter.next();
            XYDataItem brmPoint = brmIter.next();
            double xPos = ilmPoint.getXValue();
            double yPos = Math.abs(brmPoint.getYValue() - ilmPoint.getYValue());
            segDiff.add(xPos, yPos);
            x[i] = xPos;
            y[i] = yPos;
        }
//        dataset.addSeries(segDiff);

        //build spline function to help find derivaties of diff.
//        interpolator = new LoessInterpolator(0.4, 0);//good for identifying the true foveal center
//        UnivariateInterpolator interpolator = new LoessInterpolator(0.2, 0);//good for removing and identifying relative vicinity of foveal center
        function = interpolator.interpolate(x, y);

        //get first and second derivative second derivative of the diff
        FiniteDifferencesDifferentiator differ = new FiniteDifferencesDifferentiator(4, 0.25);//use 4 point differences differentiator (that is it uses 4 points arround the point in question to derive the slope at the given point) 
        UnivariateDifferentiableFunction difFunc = differ.differentiate(function);

        //plot derivatives
        XYSeries interpolated = new XYSeries("Interp.");
        XYSeries fd = new XYSeries("F'");
        XYSeries sd = new XYSeries("F''");
        XYSeries td = new XYSeries("F'''");
        int params = 1;
        int order = 3;
        DerivativeStructure xd;
        DerivativeStructure yd;
        double[] firstDerive = new double[img.getWidth()];
        Arrays.fill(firstDerive, 0);

        for (int xRealValue = 1; xRealValue <= x[x.length - 1] - 1; xRealValue++) {
            //get first and second derivative at the given point
            xd = new DerivativeStructure(params, order, 0, xRealValue);
            yd = difFunc.value(xd);
            interpolated.add(xRealValue, yd.getValue());//interpolated difference value
            firstDerive[xRealValue] = yd.getPartialDerivative(1);
            fd.add(xRealValue, yd.getPartialDerivative(1));//first derivative at point
            sd.add(xRealValue, yd.getPartialDerivative(2));//second derivative at point
            td.add(xRealValue, yd.getPartialDerivative(3));//third derivative at point
        }
//        dataset.addSeries(interpolated);
        dataset.addSeries(fd);
        dataset.addSeries(sd);
        dataset.addSeries(td);
        XYSeries peaks = OCTSelection.findMaxAndMins(fd, "F' Peaks");
        dataset.addSeries(peaks);
        dataset.addSeries(findFovea(peaks, firstDerive));

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
        octMap.put("JC_10073_OS", TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\DiseasedOCTs\\JC_10073_OS_L_7_0_0000027_OCT_1000x600_reg_6fr_resized_810x460.tif")));

        for (Map.Entry<String, BufferedImage> ent : octMap.entrySet()) {
            FoveaFindingExp myFrame = new FoveaFindingExp(ent.getKey(), ent.getValue());
            SwingUtilities.invokeLater(() -> {
                myFrame.pack();
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

    public static JPanel drawSegmentation(BufferedImage oct, XYSeries ilmSeg, XYSeries brmSeg) {
        return new DemoOCTPanel(oct, ilmSeg, brmSeg);
    }

    public static XYSeries findFovea(XYSeries peaks, double[] firstDeriv) {
        //get difference in slope between each peak point and it's neighbors
        XYDataItem prevPeak = null;
        LinkedList<Diff> diffs = new LinkedList<>();
        for (XYDataItem curPeak : (List<XYDataItem>) peaks.getItems()) {
            if (prevPeak != null) {
                int x1 = prevPeak.getX().intValue();
                int x2 = curPeak.getX().intValue();
                double diff = Math.abs(firstDeriv[x1] - firstDeriv[x2]);
                diffs.add(new Diff(x1, x2, diff));
            }
            prevPeak = curPeak;
        }
        //search for diff with greatest change in slope between peaks
        Diff maxDiff = diffs.stream().max(Comparator.comparingDouble(diff -> diff.diff)).get();
        //find where the sign changes along derivative
        double sign = Math.signum(firstDeriv[maxDiff.x1]);
        int signChangeXPos = maxDiff.x1 + 1;
        for (; sign == Math.signum(firstDeriv[signChangeXPos]); signChangeXPos++);
        //find which point is closer to true zero
        int foveaX = (Math.abs(firstDeriv[signChangeXPos]) < Math.abs(firstDeriv[signChangeXPos - 1])) ? signChangeXPos : signChangeXPos - 1;
        //return series with single point representing the fovea
        XYSeries ret = new XYSeries("Fovea");
        ret.add(foveaX, 0);
        return ret;
    }

    private static class Diff {

        int x1, x2;
        double diff;

        public Diff(int x1, int x2, double diff) {
            this.x1 = x1;
            this.x2 = x2;
            this.diff = diff;
        }

    }

    private static class DemoOCTPanel extends JPanel {

        BufferedImage oct;
        XYSeries ilmSeg;
        XYSeries brmSeg;

        public DemoOCTPanel(BufferedImage oct, XYSeries ilmSeg, XYSeries brmSeg) {
            this.oct = oct;
            this.ilmSeg = ilmSeg;
            this.brmSeg = brmSeg;
            setSize(oct.getWidth(), oct.getHeight());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(oct.getWidth(), oct.getHeight()); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(oct, 0, 0, null);
            g.setColor(Color.red);
            ((List<XYDataItem>) ilmSeg.getItems()).forEach(p -> {
                g.drawLine(p.getX().intValue(), p.getY().intValue(), p.getX().intValue(), p.getY().intValue());
            });
            g.setColor(Color.green);
            ((List<XYDataItem>) brmSeg.getItems()).forEach(p -> {
                g.drawLine(p.getX().intValue(), p.getY().intValue(), p.getX().intValue(), p.getY().intValue());
            });
        }

    }
}
