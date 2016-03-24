/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import ij.measure.CurveFitter;
import ij.measure.Minimizer;
import java.awt.BorderLayout;
import java.awt.Point;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import oct.analysis.application.LRPFrame;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.OSLengthResult;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.analysis.application.dat.SelectionType;
import oct.analysis.application.lrp.LRPPanel;
import oct.util.Line;
import oct.util.Util;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OSLengthWorker extends SwingWorker<OSLengthResult, Void> {

    private static final OCTAnalysisManager octmngr = OCTAnalysisManager.getInstance();
    private static final SelectionLRPManager lrpmngr = SelectionLRPManager.getInstance();
    private final Point clickPoint;
    private double distanceBetweenLrp, roiWidth;
    private final boolean disIsInPixels, roiIsInPixels;
    private final DecimalFormat df = new DecimalFormat("#,##0.000");

    public OSLengthWorker(Point clickPoint, double distanceBetweenLrp, double roiWidth, boolean disIsInPixels, boolean roiIsInPixels) {
        this.clickPoint = clickPoint;
        this.distanceBetweenLrp = distanceBetweenLrp;
        this.roiWidth = roiWidth;
        this.disIsInPixels = disIsInPixels;
        this.roiIsInPixels = roiIsInPixels;
    }

    @Override
    protected OSLengthResult doInBackground() throws Exception {
        //convert ROI width to pixels if it isn't already 
        if (!roiIsInPixels) {
            roiWidth = octmngr.microns2PixelsInX(roiWidth);
        }
        //convert distance between LRPs to pixels if it isn't already
        if (!disIsInPixels) {
            distanceBetweenLrp = octmngr.microns2PixelsInX(distanceBetweenLrp);
        }

        //perform analysis
        OSLengthResult result = null;
        try {
            Line iz = new Line((int) (roiWidth / distanceBetweenLrp) + 2);
            Line ez = new Line((int) (roiWidth / distanceBetweenLrp) + 2);
            octmngr.getImgPanel().addDrawnLine(iz);
            octmngr.getImgPanel().addDrawnLine(ez);
            OCTAnalysisUI.getInstance().getDispSegmentationCheckBox().setSelected(true);
            octmngr.getImgPanel().showLines();
            OCTSelection prevSelection = null;
            for (int lrpPos = clickPoint.x - (int) (roiWidth / 2d); lrpPos <= clickPoint.x + (int) (roiWidth / 2d); lrpPos += Math.round(distanceBetweenLrp)) {
                if (isCancelled()) {
                    throw new InterruptedException("OS Length worked canceled");
                }
                //check for a previous selection
                if (lrpmngr.getSelections().size() > 0) {
                    prevSelection = lrpmngr.getSelections().get(0);
                }
                //create selection and LRP for the current point that will be analyzed
                OCTSelection curSel = lrpmngr.createSelection(lrpPos, "Current X = " + lrpPos, SelectionType.NONFOVEAL, false);
                lrpmngr.addOrUpdateSelection(curSel);
                octmngr.getImgPanel().repaint();
                OSLengthWorker waitObject = this;
                //display current LRP to user near the previous LRP
                System.out.println("Displaying LRP");
                if (prevSelection == null) {
                    lrpmngr.displayLRPs(OCTAnalysisUI.getInstance());
                } else {
                    lrpmngr.displayLRPs(lrpmngr.getLRP(prevSelection));
                }
                //remove previous LRP if one was present
                System.out.println("Removing previous selection...");
                lrpmngr.removeSelection(prevSelection, true);

                //add a 'next LRP' button to allow the user to navigate to the next LRP after
                //making their peak selections
                JButton nextbtn = new JButton("Next LRP");
                nextbtn.addActionListener((evt) -> {
                    synchronized (waitObject) {
                        waitObject.notify();
                    }
                });
                nextbtn.setEnabled(false);
                LRPFrame curFrame = lrpmngr.getLRP(curSel);
                curFrame.add(nextbtn, BorderLayout.PAGE_END);
                curFrame.validate();

                //add click listener to the LRP panel
                System.out.println("Adding click listener..");
                LRPPanel tmpPanel = curFrame.getLrpPanel();
                LinkedList<SelectPoint> selectList = new LinkedList<>();
                tmpPanel.addChartMouseListener(new ChartMouseListener() {

                    @Override
                    public void chartMouseClicked(ChartMouseEvent event) {
                        //handle user clicks on the chart
                        ChartEntity entity = event.getEntity();
                        if (entity instanceof XYItemEntity) {
                            //calculate the coordinates of the clicked point
                            XYItemEntity xyent = (XYItemEntity) entity;
                            HighlightXYRenderer renderer = (HighlightXYRenderer) tmpPanel.getChart().getXYPlot().getRenderer();
                            int y = octmngr.getImgPanel().getHeight() - ((int) Math.round(xyent.getDataset().getXValue(xyent.getSeriesIndex(), xyent.getItem())));
                            SelectPoint clickedPoint = new SelectPoint(curSel.getXPositionOnOct(), y, xyent.getSeriesIndex(), xyent.getItem());
                            renderer.addOrRemoveSelectedItem(xyent.getSeriesIndex(), xyent.getItem());
                            //if user is clicking previously clicked point, deselect 
                            //it, otherwise add to list of selected points
                            if (!selectList.remove(clickedPoint)) {
                                if (selectList.size() == 2) {
                                    SelectPoint removedPoint = selectList.pollFirst();
                                    renderer.addOrRemoveSelectedItem(removedPoint.getSeries(), removedPoint.getItem());
                                }
                                selectList.add(clickedPoint);
                            }
                            if (selectList.size() == 2) {
                                nextbtn.setEnabled(true);
                            } else {
                                nextbtn.setEnabled(false);
                            }
                        }
                    }

                    @Override
                    public void chartMouseMoved(ChartMouseEvent event) {
                        //do nothing, don't care if the mouse has moved over the chart
                    }
                });

                //wait for user click on 'next LRP' button
                synchronized (this) {
                    this.wait();
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException("OS Length worked canceled");
                }

                //add selected points to respective segmentation lines
                SelectPoint ezpt = selectList.stream().reduce(selectList.get(0), (SelectPoint t, SelectPoint u) -> (t.y < u.y) ? t : u);
                ez.add(ezpt);
                selectList.remove(ezpt);
                iz.add(selectList.peek());

                System.out.println("Done waiting, points added!");
            }
            //remove last selection because analysis is done
            lrpmngr.removeSelections(true);
            octmngr.getImgPanel().clearDrawnPoint();
            octmngr.getImgPanel().repaint();

            if (isCancelled() || Thread.interrupted()) {
                throw new InterruptedException();
            }
            /*
             calculate the difference between EZ and IZ
             */
            Line diff = new Line(iz.size());
            for (int i = 0; i < iz.size(); i++) {
                Point izPoint = iz.get(i);
                Point ezPoint = ez.get(i);
                diff.add(new Point(izPoint.x, Math.abs(ezPoint.y - izPoint.y)));
            }
            /*
             fit a gaussian curve to the difference using Apache math
             */
            WeightedObservedPoints obs = new WeightedObservedPoints();
            diff.forEach(p -> {
                obs.add(p.getX(), p.getY());
            });
            double[] parameters = GaussianCurveFitter.create().fit(obs.toList());
            Gaussian g = new Gaussian(parameters[0], parameters[1], parameters[2]);
            /*
             fit a gaussian curve to the difference using imagej
             */
            CurveFitter cf = new CurveFitter(diff.stream().mapToDouble(p -> p.getX()).toArray(), diff.stream().mapToDouble(p -> p.getY()).toArray());
            cf.doFit(CurveFitter.GAUSSIAN);
            boolean fitted = false;
            for (int i = 0; i < 30; i++) {
                Thread.sleep(100);
                if (cf.getStatus() == Minimizer.SUCCESS) {
                    fitted = true;
                    break;
                }
            }
            //get the values of the various fits at certain x points
            XYSeries diffPoints = new XYSeries("Raw EZ/IZ Difference");
            diff.forEach(p -> {
                diffPoints.add(p.getX(), p.getY());
            });
            XYSeries amgPoints = new XYSeries("Apache Math gaussian fit");
            XYSeries ijgPoints = new XYSeries("ImageJ gaussian fit");
            XYSeries amgmPoint = new XYSeries("Apache Math gaussian fit maximum");
            XYSeries ijgmPoint = new XYSeries("ImageJ gaussian fit maximum");
            if (fitted) {
                double[] params = cf.getParams();
                for (double x = diff.getFirstPoint().getX(); x <= diff.getLastPoint().getX(); x += 0.25) {
                    amgPoints.add(x, g.value(x));
                    ijgPoints.add(x, cf.f(params, x));
                }
                amgmPoint.add(((List<XYDataItem>) amgPoints.getItems()).stream().filter(p -> p.getYValue() == amgPoints.getMaxY()).findFirst().orElse(null));
                ijgmPoint.add(((List<XYDataItem>) ijgPoints.getItems()).stream().filter(p -> p.getYValue() == ijgPoints.getMaxY()).findFirst().orElse(null));
            } else {
                System.err.println("Failed to fit, b/c " + cf.getStatusString());
                for (double x = diff.getFirstPoint().getX(); x <= diff.getLastPoint().getX(); x += 0.25) {
                    amgPoints.add(x, g.value(x));
                }
                amgmPoint.add(((List<XYDataItem>) amgPoints.getItems()).stream().filter(p -> p.getYValue() == amgPoints.getMaxY()).findFirst().orElse(null));
            }
            result = new OSLengthResult(diffPoints, amgPoints, ijgPoints, amgmPoint, ijgmPoint);
        } catch (InterruptedException ie) {
            //do nothing but return null since task was canceled
            System.out.println(ie.getMessage());
        }

        return result;
    }

    @Override
    protected void done() {
        try {
            OSLengthResult result = get();
            lrpmngr.addOrUpdateSelection(lrpmngr.createSelection((int) Math.round(result.getMaxDiffLocation()), "Max OS length", SelectionType.FOVEAL, false));
            octmngr.getImgPanel().repaint();
            result.graphResult();
            //display results message to user
            JLabel info = new JLabel("<html><b>OS length results<b><html>");
            Object[] message = new Object[]{
                info,
                "Max OS Length: " + df.format(octmngr.pixels2MicronsInY(result.getMaxDiff())) + " \u00B5m",
                "X Location of Max Length (pixels): " + Math.round(result.getMaxDiffLocation()),
                "Fit used: " + result.getMaxDiffSource()
            };
            JOptionPane.showMessageDialog(OCTAnalysisUI.getInstance(), message);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(OSLengthWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class SelectPoint extends Point {

        private int series, item;

        public SelectPoint(int x, int y, int series, int item) {
            super(x, y);
            this.series = series;
            this.item = item;
        }

        public int getSeries() {
            return series;
        }

        public int getItem() {
            return item;
        }
    }

}
