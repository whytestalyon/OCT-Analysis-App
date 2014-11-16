/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.swing.SwingUtilities;
import oct.analysis.application.LRPFrame;
import oct.analysis.application.OCTSelection;

/**
 *
 * @author Brandon
 */
public class SelectionLRPManager {

    public static final String PROP_SELECTIONS = "selections";
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private final HashMap<String, OCTSelection> selectionMap = new HashMap<>(50);//key is selection name
    private final HashMap<String, LRPFrame> lrpDispMap = new HashMap<>(50);//key is name of selection that backs lrp
    private final OCTAnalysisManager analysisData = OCTAnalysisManager.getInstance();
    private int foveaCenterXPosition;
    private int selectionWidth;
    private int lrpSmoothingFactor = 10;

    private SelectionLRPManager() {
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
    }

    private static class SelectionLRPManagerHolder {

        private static final SelectionLRPManager INSTANCE = new SelectionLRPManager();
    }

    public void addSelections(int foveaXPosition) {
        foveaCenterXPosition = foveaXPosition;
        updateSelections(getSelections(foveaXPosition));
    }

    public void updateSelections(List<OCTSelection> selections) {
        selections.stream().forEach((selection) -> {
            selectionMap.put(selection.getSelectionName(), selection);
            if (lrpDispMap.containsKey(selection.getSelectionName())) {
                //update the JFrame for the given selection with the new LRP
                System.out.println("Updateing LRP for " + selection.getSelectionName());
                LRPFrame updateFrame = lrpDispMap.get(selection.getSelectionName());
                updateFrame.updateLRP(selection.createLRPPanel());
            } else {
                //add new LRP frame for never before added selection
                LRPFrame newFrame = new LRPFrame(selection.createLRPPanel());
                lrpDispMap.put(selection.getSelectionName(), newFrame);
            }
        });
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
    private List<OCTSelection> getSelections(int foveaXPosition) {
        OCTSelection fovealSel = new OCTSelection(foveaXPosition - (selectionWidth / 2), analysisData.getOct().getImageOffsetY(), selectionWidth, analysisData.getOct().getOctImage().getHeight(), OCTSelection.FOVEAL_SELECTION, "FV");
        return getSelectionsFromFoveaSelection(fovealSel);
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
}
