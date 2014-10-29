/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
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
    private final OCTAnalysisDAO analysisData = OCTAnalysisDAO.getInstance();

    public void addOrUpdateSelections(List<OCTSelection> selections) {
        selections.stream().forEach((selection) -> {
            selectionMap.put(selection.getSelectionName(), selection);
            if (lrpDispMap.containsKey(selection.getSelectionName())) {
                //update the JFrame for the given selection with the new LRP
                LRPFrame updateFrame = lrpDispMap.get(selection.getSelectionName());
                updateFrame.updateLRP(selection.createLRPPanel());
            } else {
                //add new LRP frame for never before added selection
                LRPFrame newFrame = new LRPFrame(selection.createLRPPanel());
                lrpDispMap.put(selection.getSelectionName(), newFrame);
            }
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

}
