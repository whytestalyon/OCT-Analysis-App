/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.awt.Component;
import java.awt.Point;
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
import oct.analysis.application.err.OverOCTEdgeException;

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
    public void addOrUpdateEquidistantSelections(int foveaXPosition, int micronsBetweenSelections) {
        if (foveaXPosition != analysisData.getFoveaCenterXPosition()) {
            System.out.println("Updating fovea position! Old: " + analysisData.getFoveaCenterXPosition() + ", New: " + foveaXPosition);
            analysisData.setFoveaCenterXPosition(foveaXPosition);
        }
        addOrUpdateSelections(getEquidistantSelections(foveaXPosition, micronsBetweenSelections));
    }

    public void addOrUpdateSelections(List<OCTSelection> selections) {
        selections.stream().forEach((selection) -> {
            addOrUpdateSelection(selection);
        });
    }

    public void addOrUpdateSelection(OCTSelection selection) {
        selectionMap.put(selection.getSelectionName(), selection);
        if (lrpDispMap.containsKey(selection.getSelectionName())) {
            //update the JFrame for the given selection with the new LRP
            updateLRP(selection);
        } else {
            //add new LRP frame for never before added selection
            LRPFrame newFrame = new LRPFrame(selection.createLRPPanel());
            lrpDispMap.put(selection.getSelectionName(), newFrame);
        }
    }

    public void updateLRPs() {
        SwingUtilities.invokeLater(() -> {
            selectionMap.values().parallelStream().forEach((selection) -> {
                //update the JFrame for the given selection with the new LRP
                updateLRP(selection);
            });
        });
    }

    public void updateLRP(OCTSelection selection) {
        LRPFrame updateFrame = lrpDispMap.get(selection.getSelectionName());
        if (updateFrame != null) {
            selection.updateLRP(updateFrame);
        }
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
            System.out.println("Displaying " + lrpFrame.getLrpPanel().getTitle() + "!");
            lrpFrame.run();
        });
    }

    public LRPFrame getLRP(String name) {
        return lrpDispMap.get(name);
    }

    public LRPFrame getLRP(OCTSelection selection) {
        return getLRP(selection.getSelectionName());
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
    public OCTSelection createSelection(int xPositionOnOCT, String selectionName, SelectionType selType, boolean moveable) {
        if (selType == SelectionType.FOVEAL) {
            return getFoveaSelection(xPositionOnOCT, moveable);
        } else {
            return new OCTSelection(xPositionOnOCT, 0, selectionWidth, analysisData.getOct().getImageHeight(), selType, selectionName, moveable);
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
        return new OCTSelection(xPositionOnOCT, 0, selectionWidth, analysisData.getOct().getImageHeight(), SelectionType.FOVEAL, "Fovea", moveable);
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
        int selX;
        for (int i = 1; (selX = foveaSelection.getXPositionOnOct() + analysisData.getNumPixelFromFovea(i)) <= analysisData.getOct().getImageWidth(); i++) {
            try {
                selections.add(createSelection(selX, "R" + i, SelectionType.NONFOVEAL, false));
            } catch (OverOCTEdgeException oe) {
                //just need ot fail silenty here, nothin is wrong just selection too wide to add since it would go past edge of OCT
                break;
            }
        }
        //build selection list to the left of the center
        for (int i = 1; (selX = foveaSelection.getXPositionOnOct() - analysisData.getNumPixelFromFovea(i)) >= 0; i++) {
            try {
                selections.add(createSelection(selX, "L" + i, SelectionType.NONFOVEAL, false));
            } catch (OverOCTEdgeException oe) {
                //just need ot fail silenty here, nothin is wrong just selection too wide to add since it would go past edge of OCT
                break;
            }
        }

        return selections;
    }

    /**
     * Get the selection that overlaps with the X coordinate of the given point,
     * if any. If the point doesn't overlap a selection then null will be
     * returned. The provided coordinate should be relative to the OCT image,
     * and not the component containing the image.
     *
     * @param xPos
     * @param ignoreFoveaSelection
     * @return the selection that overlaps the give X coordinate, or null if
     * none is found
     */
    public OCTSelection getSelection(int xPos, boolean ignoreFoveaSelection) {
        return getSelection(new Point(xPos, analysisData.getImgPanel().getImageOffsetY() + 2), ignoreFoveaSelection);
    }

    /**
     * Get the selection that has its select button overlapped by the given
     * point, if any. If the point doesn't overlap a selection then null will be
     * returned. The provided coordinate should be relative to the OCT image,
     * and not the component containing the image.
     *
     * @param p
     * @param ignoreFoveaSelection
     * @return the selection with its select button overlapped by the supplied
     * point, or null if none exists
     */
    public OCTSelection getSelection(Point p, boolean ignoreFoveaSelection) {
        Optional<OCTSelection> sel;
        if (ignoreFoveaSelection) {
            sel = selectionMap.values().stream().filter(s -> s.getSelectionType() != SelectionType.FOVEAL).filter(s -> s.getSelectionButtonShape().contains(p)).findFirst();
        } else {
            sel = selectionMap.values().stream().filter(s -> s.getSelectionButtonShape().contains(p)).findFirst();
        }
        return sel.isPresent() ? sel.get() : null;
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

    public boolean selectionTopContains(Point mousePos) {
        return selectionMap.values().stream().filter(sel -> sel.overlapsTopOfSelection(mousePos)).count() > 0;
    }

    public boolean selectionBottomContains(Point mousePos) {
        return selectionMap.values().stream().filter(sel -> sel.overlapsBottomOfSelection(mousePos)).count() > 0;
    }
}
