/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.awt.Component;
import java.awt.Polygon;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ConcurrentHashMap<String, OCTSelection> selectionMap;
    private final ConcurrentHashMap<String, LRPFrame> lrpDispMap;
    private final OCTAnalysisManager analysisData = OCTAnalysisManager.getInstance();
    private String selectedSelectionName = "";
    private int selectionWidth = 5;
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

    public int getSelectionWidth() {
        return selectionWidth;
    }

    public void setSelectionWidth(int selectionWidth) {
        this.selectionWidth = selectionWidth;
        selectionMap.forEach((key, selection) -> {
            selection.setWidth(selectionWidth);
        });
        //update the LRPs for all of the selections (if they are being presented)
        updateLRPs();
    }

    public void setSelectedSelection(OCTSelection sel) {
        selectedSelectionName = sel.getSelectionName();
    }

    private static class SelectionLRPManagerHolder {

        private static final SelectionLRPManager INSTANCE = new SelectionLRPManager();
    }

    /**
     * Adds a selection for the fovea as well as the equidistant (from the
     * fovea) selections based on the supplied center position of the fovea. All
     * selections will be equal distant from each other and be generated outward
     * from the position of the center of the fovea.
     *
     * @param foveaXPosition center position of he fovea
     * @param micronsBetweenSelections the number of microns between each
     * selection
     */
    public void addOrUpdateSpatialSelections(int foveaXPosition, int micronsBetweenSelections) {
        addOrUpdateSelections(getEquidistantSelections(foveaXPosition, micronsBetweenSelections));
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
            selectionMap.values().parallelStream().forEach((selection) -> {
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

    public void removeNonfovealSelections(boolean removeLRPs) {
        if (removeLRPs) {
            //close all active LRP Frames
            lrpDispMap.forEach((lrpKey, lrpFrame) -> {
                lrpFrame.dispose();
            });
            //reset LRP tracking map
            lrpDispMap.clear();
        }
        //remove all of the selections
        Optional<OCTSelection> f = selectionMap.values().stream().filter(sel -> sel.getSelectionType() == SelectionType.FOVEAL).findFirst();
        selectionMap.clear();
        if (f.isPresent()) {
            OCTSelection fovealsel = f.get();
            selectionMap.put(fovealsel.getSelectionName(), fovealsel);
        }
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
     * and desired width of each selection.
     *
     * @param xPositionOnOCT the X position corresponding to the center of the
     * fovea relative to the top left corner of the OCT image (NOT the top left
     * corner of the window)
     * @param micronsBetweenSelections the number of microns between each
     * selection
     * @return list containing all of the OCT image selections based on the
     * foveal selection and the desired distance between selections
     */
    private List<OCTSelection> getEquidistantSelections(int xPositionOnOCT, int micronsBetweenSelections) {
        OCTSelection fovealSel = getFoveaSelection(xPositionOnOCT, false);
        return getSelectionsFromFoveaSelection(fovealSel, micronsBetweenSelections);
    }

    /**
     * Given an X coordinate return a selection centered around the supplied
     * position on the OCT.
     *
     * @param xPositionOnOCT the X position corresponding to the center of the
     * desired selection
     * @param selectionName
     * @return a selection centered around the supplied position
     */
    public OCTSelection getSelection(int xPositionOnOCT, String selectionName, SelectionType selType, boolean moveable) {
        if (selType == SelectionType.FOVEAL) {
            return getFoveaSelection(xPositionOnOCT, moveable);
        } else {
            return new OCTSelection(xPositionOnOCT - (selectionWidth / 2), 0, selectionWidth, analysisData.getOct().getImageHeight(), selType, selectionName, moveable);
        }
    }

    /**
     * Given an X coordinate of the fovea return single selection centered
     * around the supplied position.
     *
     * @param xPositionOnOCT the X position corresponding to the center of the
     * fovea on the OCT
     * @return fovea selection
     */
    public OCTSelection getFoveaSelection(int xPositionOnOCT, boolean moveable) {
        return new OCTSelection(xPositionOnOCT - (selectionWidth / 2), 0, selectionWidth, analysisData.getOct().getImageHeight(), SelectionType.FOVEAL, "Fovea", moveable);
    }

    /**
     * Build out the OCT selections to the right and the left of the Foveal
     * Selection. This method will place a selection every
     * {@link OCTAnalysisManager#getDistanceBetweenSelections() } pixels (center
     * to center) to the right and left of the foveal selection. These
     * selections continue until the edge of the OCT image is reached in each
     * direction.
     *
     * @param foveaSelection initial selection denoting the fovea on the OCT
     * @param micronsBetweenSelections the number of microns between each
     * selection
     * @return list containing all of the OCT image selections based on the
     * foveal selection and the desired distance between selections
     */
    private List<OCTSelection> getSelectionsFromFoveaSelection(OCTSelection foveaSelection, int micronsBetweenSelections) {
        //set the microns between selections so we can calculate distance (in pixels) between selections
        analysisData.setMicronsBetweenSelections(micronsBetweenSelections);
        LinkedList<OCTSelection> selections = new LinkedList<>();
        //add foveal selction to list of selections
        selections.add(foveaSelection);
        //build selection list to the right of center
        for (int selX = foveaSelection.getXPositionOnOct() + analysisData.getDistanceBetweenSelections(), selCnt = 1; (selX + foveaSelection.getWidth()) <= analysisData.getOct().getImageWidth(); selX += analysisData.getDistanceBetweenSelections(), selCnt++) {
            selections.add(new OCTSelection(selX, 0, selectionWidth, foveaSelection.getHeight(), SelectionType.NONFOVEAL, "R" + selCnt, false));
        }
        //build selection list to the left of the center
        for (int selX = foveaSelection.getXPositionOnOct() - analysisData.getDistanceBetweenSelections(), selCnt = 1; selX >= 0; selX -= analysisData.getDistanceBetweenSelections(), selCnt++) {
            selections.add(new OCTSelection(selX, 0, selectionWidth, foveaSelection.getHeight(), SelectionType.NONFOVEAL, "L" + selCnt, false));
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
    public OCTSelection getOverlappingSelection(int xpos, int ypos, int xOffset, int yOffset) {
        for (OCTSelection selection : selectionMap.values()) {
            Polygon selbtn = selection.getSelectionButtonShape();
            selbtn.translate(xOffset, yOffset);
            if (selbtn.contains(xpos, ypos)) {
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
        if (sel.getXPositionOnOct() + pixelsToMoveBy >= 0
                && sel.getXPositionOnOct() + pixelsToMoveBy + selectionWidth - 1 < analysisData.getOct().getImageWidth()) {
            sel.setXPositionOnOct(sel.getXPositionOnOct() + pixelsToMoveBy);
            updateLRP(sel);
        }
    }

    public OCTSelection getSelectedSelection() {
        return selectionMap.get(selectedSelectionName);
    }

}
