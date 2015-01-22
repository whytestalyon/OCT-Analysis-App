/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.awt.Component;
import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javax.swing.SwingUtilities;
import oct.analysis.application.FoveaFindingExp;
import oct.analysis.application.LRPFrame;
import oct.analysis.application.OCTSelection;
import oct.util.Segmentation;
import oct.util.Util;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.jfree.data.xy.XYDataItem;

/**
 *
 * @author Brandon
 */
public class SelectionLRPManager {

    public static final String PROP_SELECTIONS = "selections";
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private final ConcurrentHashMap<String, OCTSelection> selectionMap;
    private final ConcurrentHashMap<String, LRPFrame> lrpDispMap;
    private final OCTAnalysisManager analysisData = OCTAnalysisManager.getInstance();
    private String selectedSelectionName = "";
    private int foveaCenterXPosition = -1;
    private int selectionWidth;
    private int lrpSmoothingFactor = 5;

    private SelectionLRPManager() {
        this.selectionMap = new ConcurrentHashMap<>(50);
        this.lrpDispMap = new ConcurrentHashMap<>(50);
    }

    public static SelectionLRPManager getInstance() {
        return SelectionLRPManagerHolder.INSTANCE;
    }

    public int getLrpSmoothingFactor() {
        return lrpSmoothingFactor;
    }

    public void setLrpSmoothingFactor(int lrpSmoothingFactor) {
        this.lrpSmoothingFactor = lrpSmoothingFactor;
    }

    public int getFoveaCenterXPosition() {
        return foveaCenterXPosition;
    }

    public void setFoveaCenterXPosition(int foveaCenterXPosition) {
        this.foveaCenterXPosition = foveaCenterXPosition;
    }

    public int getSelectionWidth() {
        return selectionWidth;
    }

    public void setSelectionWidth(int selectionWidth) {
        this.selectionWidth = selectionWidth;
        selectionMap.forEachValue(1000, (selection) -> {
            selection.setWidth(selectionWidth);
        });
    }

    public void setSelectedSelection(OCTSelection sel) {
        selectedSelectionName = sel.getSelectionName();
    }

    private static class SelectionLRPManagerHolder {

        private static final SelectionLRPManager INSTANCE = new SelectionLRPManager();
    }

    public void addOrUpdateSpatialSelections(int foveaXPosition) {
        foveaCenterXPosition = foveaXPosition;
        addOrUpdateSelections(getSpatialSelections(foveaXPosition));
    }

    public void addOrUpdateSelections(List<OCTSelection> selections) {
        selections.stream().forEach((selection) -> {
            addOrUpdateSelection(selection);
        });
    }

    public void addOrUpdateSelection(OCTSelection selection) {
        selectionMap.put(selection.getSelectionName(), selection);
        SwingUtilities.invokeLater(() -> {
            if (lrpDispMap.containsKey(selection.getSelectionName())) {
                //update the JFrame for the given selection with the new LRP
                updateLRP(selection);
            } else {
                //add new LRP frame for never before added selection
                LRPFrame newFrame = new LRPFrame(selection.createLRPPanel());
                lrpDispMap.put(selection.getSelectionName(), newFrame);
            }
        });
    }

    public void updateLRPs() {
        SwingUtilities.invokeLater(() -> {
            selectionMap.values().forEach((selection) -> {
                //update the JFrame for the given selection with the new LRP
                updateLRP(selection);
            });
        });
    }

    private void updateLRP(OCTSelection selection) {
        LRPFrame updateFrame = lrpDispMap.get(selection.getSelectionName());
        updateFrame.updateLRP(selection.createLRPPanel());
    }

    public void removeSelections(boolean removeLRPs) {
        if (removeLRPs) {
            //close all active LRP Frames
            lrpDispMap.forEach((lrpKey, lrpFrame) -> {
                lrpFrame.dispose();
            });
            //reset LRP tracking map
            lrpDispMap.clear();
        }
        //remove all of the selections
        selectionMap.clear();
    }

    public void removeSelection(OCTSelection s, boolean removeLRP) {
        if (removeLRP) {
            if (lrpDispMap.containsKey(s.getSelectionName())) {
                lrpDispMap.get(s.getSelectionName()).dispose();
            }
            //remove the lrp from the tracking map
            lrpDispMap.remove(s.getSelectionName());
        }
        //remove given selection from selection map
        selectionMap.remove(s.getSelectionName());
    }

    public List<OCTSelection> getSelections() {
        return new ArrayList<>(selectionMap.values());
    }

    public void updateSelectionOffsets(int oldXoffset, int oldYoffset) {
        selectionMap.forEach((selKey, selection) -> {
            //update selection to new offset
            selection.setPanel_x_position(selection.getPanel_x_position() - oldXoffset + analysisData.getOct().getImageOffsetX());
            selection.setPanel_y_position(selection.getPanel_y_position() - oldYoffset + analysisData.getOct().getImageOffsetY());
        });
    }

    public void displayLRPs(Component relativeTo) {
        //set intial relativity for frames
        Component prev = null;
        for (LRPFrame lrpFrame : lrpDispMap.values()) {
            if (prev == null) {
                prev = relativeTo;
            }
            lrpFrame.setRelativeTo(prev);
            prev = lrpFrame;
        }
        //display frames
        lrpDispMap.forEach((lrpKey, lrpFrame) -> {
            SwingUtilities.invokeLater(lrpFrame);
        });
    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Given a X coordinate of the fovea, provide a list of selections
     * (including one centered on the fovea) based on the user supplied scale
     * and desired width of each LRP selection.
     *
     * @param foveaXPosition the X position corresponding to the center of the
     * fovea
     * @return list containing all of the OCT image selections based on the
     * foveal selection and the desired distance between selections
     */
    private List<OCTSelection> getSpatialSelections(int foveaXPosition) {
        OCTSelection fovealSel = new OCTSelection(foveaXPosition - (selectionWidth / 2), analysisData.getOct().getImageOffsetY(), selectionWidth, analysisData.getOct().getOctImage().getHeight(), OCTSelection.FOVEAL_SELECTION, "FV");
        return getSelectionsFromFoveaSelection(fovealSel);
    }

    /**
     * Given an X coordinate return a selection centered around the supplied
     * position.
     *
     * @param position the X position corresponding to the center of the desired
     * selection
     * @param selectionName
     * @return a selection centered around the supplied position
     */
    public OCTSelection getSelection(int position, String selectionName) {
        return new OCTSelection(position - (selectionWidth / 2), analysisData.getOct().getImageOffsetY(), selectionWidth, analysisData.getOct().getOctImage().getHeight(), OCTSelection.PERIPHERAL_SELECTION, selectionName);
    }

    /**
     * Given an X coordinate of the fovea return single selection centered
     * around the supplied position.
     *
     * @param position the X position corresponding to the center of the fovea
     * @return fovea selection
     */
    public OCTSelection getFoveaSelection(int position) {
        foveaCenterXPosition = position;
        return new OCTSelection(position - (selectionWidth / 2), analysisData.getOct().getImageOffsetY(), selectionWidth, analysisData.getOct().getOctImage().getHeight(), OCTSelection.FOVEAL_SELECTION, "FV");
    }

    /**
     * Build out the OCT selections to the right and the left of the Foveal
     * Selection. This method will place a selection ever
     * @code{analysisMetrics.getDistanceBetweenSelections()} pixels (center to
     * center) to the right and left of the foveal selection. These selections
     * continue until the edge of the OCT image is reached in each direction.
     *
     * @param foveaSelection initial selection denoting the fovea on the OCT
     * @return list containing all of the OCT image selections based on the
     * foveal selection and the desired distance between selections
     */
    private List<OCTSelection> getSelectionsFromFoveaSelection(OCTSelection foveaSelection) {
        LinkedList<OCTSelection> selections = new LinkedList<>();
        //add foveal selction to list of selections
        selections.add(foveaSelection);
        //build selection list to the right of center
        for (int selX = foveaSelection.getPanel_x_position() + analysisData.getDistanceBetweenSelections(), selCnt = 1; (selX + foveaSelection.getWidth()) <= analysisData.getOct().getOctImage().getWidth() + analysisData.getOct().getImageOffsetX(); selX += analysisData.getDistanceBetweenSelections(), selCnt++) {
            selections.add(new OCTSelection(selX, analysisData.getOct().getImageOffsetY(), foveaSelection.getWidth(), foveaSelection.getHeight(), OCTSelection.PERIPHERAL_SELECTION, "R" + selCnt));
        }
        //build selection list to the left of the center
        for (int selX = foveaSelection.getPanel_x_position() - analysisData.getDistanceBetweenSelections(), selCnt = 1; selX >= analysisData.getOct().getImageOffsetX(); selX -= analysisData.getDistanceBetweenSelections(), selCnt++) {
            selections.add(new OCTSelection(selX, analysisData.getOct().getImageOffsetY(), foveaSelection.getWidth(), foveaSelection.getHeight(), OCTSelection.PERIPHERAL_SELECTION, "L" + selCnt));
        }

        return selections;
    }

    /**
     * Get the selection (excluding the fovea selection) that overlaps the give
     * X coordinate.
     *
     * @param xpos
     * @return the selection that overlaps the give X coordinate, or null if
     * none is found
     */
    public OCTSelection getOverlappingSelection(int xpos) {
        for (OCTSelection selection : selectionMap.values()) {
            if (selection.getSelectionType() == OCTSelection.PERIPHERAL_SELECTION && selection.positionOverlapsSelection(xpos)) {
                return selection;
            }
        }
        return null;
    }

    public void unselectSelections() {
        selectionMap.forEach((selKey, selection) -> {
            selection.setHighlighted(false);
        });
        selectedSelectionName = "";
    }

    public void moveSelectionRight(OCTSelection sel) {
        moveSelection(sel, 1);
    }

    public void moveSelectionLeft(OCTSelection sel) {
        moveSelection(sel, -1);
    }

    private void moveSelection(OCTSelection sel, int pixelsToMoveBy) {
        OCTSelection selection = null;
        if (!selectedSelectionName.isEmpty()
                && (selection = selectionMap.get(selectedSelectionName)) != null
                && (selection.getPanel_x_position() + pixelsToMoveBy) > analysisData.getOct().getImageOffsetX()
                && ((selection.getPanel_x_position() + pixelsToMoveBy + selectionWidth) < analysisData.getOct().getImageOffsetX() + analysisData.getOct().getOctImage().getWidth())) {
            selection.setPanel_x_position(selection.getPanel_x_position() + pixelsToMoveBy);
        }
    }

    public OCTSelection getSelectedSelection() {
        return selectionMap.get(selectedSelectionName);
    }

    /**
     * Obtain the X coordinate (relative to the OCT image) of the center of the
     * fovea.
     *
     * @return the X coordinate of the fovea relative to the OCT image supplied
     */
    public int getCenterOfFovea() {
        //interpolator for denoising signals
        UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
        //get the contour of the ILM
        double[][] ilmSeg = getXYArrays(new ArrayList<>(analysisData.getSegmentation().getSegment(Segmentation.ILM_SEGMENT)));
        //interpolate ILM contour
        UnivariateFunction ilmInterp = interpolator.interpolate(ilmSeg[0], ilmSeg[1]);
        //get the contour of the Brooks Membrane
        double[][] brmSeg = getXYArrays(new ArrayList<>(analysisData.getSegmentation().getSegment(Segmentation.BrM_SEGMENT)));
        UnivariateFunction brmInterp = interpolator.interpolate(brmSeg[0], brmSeg[1]);

        //calcualte the difference (in the Y value) between at each point along the X axis for the above contours
        double[] x = new double[analysisData.getOct().getLinearOctImage().getWidth()];
        double[] y = new double[analysisData.getOct().getLinearOctImage().getWidth()];
        for (int xPos = 0; xPos < analysisData.getOct().getLinearOctImage().getWidth(); xPos++) {
            double yPos = Math.abs(ilmInterp.value(xPos) - brmInterp.value(xPos));
            x[xPos] = xPos;
            y[xPos] = yPos;
        }

        //interpolate difference curve to function so we can find derivaties of diff.
        UnivariateFunction diffInerp = interpolator.interpolate(x, y);

        //create differentiator for the diff interpolation function
        FiniteDifferencesDifferentiator differ = new FiniteDifferencesDifferentiator(4, 0.25);//use 4 point differences differentiator (that is it uses 4 points arround the point in question to derive the slope at the given point) 
        UnivariateDifferentiableFunction difFunc = differ.differentiate(diffInerp);

        /*
         * collect the first derivative at each pixel in the image
         */
        int numFreeVariablesInFunction = 1;
        int order = 1;
        DerivativeStructure xd;
        DerivativeStructure yd;
        ArrayList<LinePoint> firstDeriv = new ArrayList<>(analysisData.getOct().getLinearOctImage().getWidth() - 1);
        IntStream.range(0, analysisData.getOct().getLinearOctImage().getWidth() - 1)
                .forEach((i) -> {
                    firstDeriv.add(new LinePoint(0, 0));
                });
        for (int xRealValue = 1; xRealValue <= analysisData.getOct().getLinearOctImage().getWidth() - 2; xRealValue++) {
            //get first derivative at the given point
            xd = new DerivativeStructure(numFreeVariablesInFunction, order, 0, xRealValue);
            yd = difFunc.value(xd);
            firstDeriv.set(xRealValue, new LinePoint(xRealValue, yd.getPartialDerivative(1)));//first derivative at point
        }

        /*
         search for the local max and mins in the first derivative
         */
        List<LinePoint> peaks = Util.findMaxAndMins(firstDeriv);

        /*
         collect the difference in value between each point and its neighbors
         */
        LinePoint prevPeak = null;
        LinkedList<Diff> diffs = new LinkedList<>();
        for (LinePoint curPeak : peaks) {
            if (prevPeak != null) {
                diffs.add(new Diff(prevPeak, curPeak));
            }
            prevPeak = curPeak;
        }
        //search for diff with greatest change in slope between peaks
        Diff maxDiff = diffs.stream().max(Comparator.comparingDouble(diff -> diff.getYDiff())).get();
//        List<LinePoint> lp = new ArrayList<LinePoint>(2);
//        lp.add(maxDiff.getLinePoint1());
//        lp.add(maxDiff.getLinePoint2());
//        try {
//            Util.graphPoints(firstDeriv, lp);
//        } catch (IOException ex) {
//            Logger.getLogger(SelectionLRPManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
        //find where the sign changes along derivative
        double sign = Math.signum(maxDiff.getLinePoint1().getY());
        int signChangeXPos = maxDiff.getLinePoint1().getX() + 1;
        for (; sign == Math.signum(firstDeriv.get(signChangeXPos).getY()); signChangeXPos++);
        //find which point is closer to true zero arround sign change
        int centerOfFovea = (Math.abs(firstDeriv.get(signChangeXPos).getY()) < Math.abs(firstDeriv.get(signChangeXPos - 1).getY())) ? signChangeXPos : signChangeXPos - 1;
        System.out.println("Fovea found at: " + centerOfFovea);
        return centerOfFovea;
    }

    public double[][] getXYArrays(List<Point> points) {
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

    private static class Diff {

        private LinePoint linePoint1, linePoint2;

        public Diff(LinePoint linePoint1, LinePoint linePoint2) {
            this.linePoint1 = linePoint1;
            this.linePoint2 = linePoint2;
        }

        public LinePoint getLinePoint1() {
            return linePoint1;
        }

        public LinePoint getLinePoint2() {
            return linePoint2;
        }

        public double getYDiff() {
            return Math.abs(linePoint1.getY() - linePoint2.getY());
        }

    }

}
