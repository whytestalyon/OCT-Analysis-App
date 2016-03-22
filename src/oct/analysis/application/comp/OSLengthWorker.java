/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import ij.measure.CurveFitter;
import ij.measure.Minimizer;
import java.awt.Point;
import java.util.LinkedList;
import java.util.stream.DoubleStream;
import javax.swing.SwingWorker;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCTAnalysisManager;
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
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OSLengthWorker extends SwingWorker<Double, Void> {

    private static final OCTAnalysisManager octmngr = OCTAnalysisManager.getInstance();
    private static final SelectionLRPManager lrpmngr = SelectionLRPManager.getInstance();
    private final Point clickPoint;
    private double distanceBetweenLrp, roiWidth;
    private final boolean disIsInPixels, roiIsInPixels;

    public OSLengthWorker(Point clickPoint, double distanceBetweenLrp, double roiWidth, boolean disIsInPixels, boolean roiIsInPixels) {
        this.clickPoint = clickPoint;
        this.distanceBetweenLrp = distanceBetweenLrp;
        this.roiWidth = roiWidth;
        this.disIsInPixels = disIsInPixels;
        this.roiIsInPixels = roiIsInPixels;
    }

    @Override
    protected Double doInBackground() throws Exception {
        //convert ROI width to pixels if it isn't already 
        if (!roiIsInPixels) {
            roiWidth = octmngr.microns2PixelsInX(roiWidth);
        }
        //convert distance between LRPs to pixels if it isn't already
        if (!disIsInPixels) {
            distanceBetweenLrp = octmngr.microns2PixelsInX(distanceBetweenLrp);
        }

        //perform analysis
        Double result = null;
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
                //add click listener to the LRP panel
                System.out.println("Adding click listener..");
                LRPPanel tmpPanel = lrpmngr.getLRP(curSel).getLrpPanel();
                tmpPanel.addChartMouseListener(new ChartMouseListener() {

                    int selectCntr = 0;

                    @Override
                    public void chartMouseClicked(ChartMouseEvent event) {
                        //handle user clicks on the chart
                        ChartEntity entity = event.getEntity();
                        if (entity instanceof XYItemEntity) {
                            //calculate the coordinates of the clicked point
                            XYItemEntity xyent = (XYItemEntity) entity;
                            HighlightXYRenderer renderer = (HighlightXYRenderer) tmpPanel.getChart().getXYPlot().getRenderer();
                            int y = octmngr.getImgPanel().getHeight() - ((int) Math.round(xyent.getDataset().getXValue(xyent.getSeriesIndex(), xyent.getItem())));
                            Point clickedPoint = new Point(curSel.getXPositionOnOct(), y);
                            //if user is clicking previously clicked point, deselect 
                            //it, otherwise add to apropriate segmentation line
                            if (selectCntr == 0) {
                                ez.add(clickedPoint);
                                octmngr.getImgPanel().repaint();
                                renderer.setHighlightedItem(xyent.getSeriesIndex(), xyent.getItem());
                                selectCntr++;
                            } else if (selectCntr == 1) {
                                if (ez.getLastPoint().equals(clickedPoint)) {
                                    ez.remove(clickedPoint);
                                    octmngr.getImgPanel().repaint();
                                    renderer.setHighlightedItem(-1, -1);
                                    selectCntr = 0;
                                } else {
                                    iz.add(clickedPoint);
                                    octmngr.getImgPanel().repaint();
                                    selectCntr++;
                                }
                            }
                        }
                        synchronized (waitObject) {
                            if (selectCntr == 2) {
                                waitObject.notify();
                            }
                        }
                    }

                    @Override
                    public void chartMouseMoved(ChartMouseEvent event) {
                        //do nothing, don't care if the mouse has moved over the chart
                    }
                });
                //wait for user click on LRP 
                synchronized (this) {
                    this.wait();
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException("OS Length worked canceled");
                }
                System.out.println("Done waiting!");
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
            for (int i = 0; i < 20; i++) {
                Thread.sleep(100);
                if (cf.getStatus() == Minimizer.SUCCESS) {
                    fitted = true;
                    break;
                }
            }
            //get graph points
            XYSeries diffPoints = new XYSeries("Diff points");
            diff.forEach(p -> {
                diffPoints.add(p.getX(), p.getY());
            });
            XYSeries amgPoints = new XYSeries("Apache Math gaussian points");
            XYSeries ijgPoints = new XYSeries("ImageJ gaussian points");
            if (fitted) {
                double[] params = cf.getParams();
                for (double x = diff.getFirstPoint().getX(); x <= diff.getLastPoint().getX(); x += 0.25) {
                    amgPoints.add(x, g.value(x));
                    ijgPoints.add(x, cf.f(params, x));
                }
            } else {
                System.err.println("Failed to fit, b/c " + Minimizer.STATUS_STRING[cf.getStatus()]);
                for (double x = diff.getFirstPoint().getX(); x <= diff.getLastPoint().getX(); x += 0.25) {
                    amgPoints.add(x, g.value(x));
                }
            }
            Util.graphSeries(diffPoints, amgPoints, ijgPoints);
        } catch (InterruptedException ie) {
            //do nothing but return null since task was canceled
            System.out.println(ie.getMessage());
        }

        return result;
    }

}
