/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTLine;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.analysis.application.dat.SelectionType;

/**
 *
 * @author Brandon
 */
public class Analysis {

    private static final OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();
    private static final SelectionLRPManager selectionLRPManager = SelectionLRPManager.getInstance();

    public static void findEZ(boolean interactive) {
        //based off of interactive mode specified, find the fovea, and then trigger EZ edge detection
        try {
            octMngr.findCenterOfFovea(!interactive);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(OCTAnalysisUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        octMngr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //if the change that occured was the setting (or update ) of the position of the fovea
                //then trigger the identification of the EZ edges
                if (OCTAnalysisManager.PROP_FOVEA_CENTER_X_POSITION.equals(evt.getPropertyName())) {
//                    SwingUtilities.invokeLater(() -> {
//                        int fv = octMngr.getFoveaCenterXPosition();
//                        OCTLine foveaSelection = new OCTLine(fv, 0, octMngr.getOct().getImageHeight(), SelectionType.FOVEAL, "Fovea", false);
//                        selectionLRPManager.addOrUpdateSelection(foveaSelection);
//                        octMngr.getImgPanel().repaint();
//                        //second, automatically find the X position of each EZ edge
//                        int[] ez = octMngr.getEZEdgeCoords();
//                        selectionLRPManager.addOrUpdateSelection(new OCTLine(ez[0], 0, octMngr.getOct().getImageHeight(), SelectionType.NONFOVEAL, "EZ Left", true));
//                        selectionLRPManager.addOrUpdateSelection(new OCTLine(ez[1], 0, octMngr.getOct().getImageHeight(), SelectionType.NONFOVEAL, "EZ Right", true));
//                        octMngr.getImgPanel().repaint();
//                    });

                    //execute the EZ edge detection as a seperate thread that will update the UI when the edges of the EZ are found
                    //monitor progress of finding the fovea
                    ProgressMonitor pm = new ProgressMonitor(octMngr.getImgPanel(),
                            "Analyzing OCT for edge of EZ...",
                            "", 0, 100);
                    pm.setMillisToDecideToPopup(0);
                    pm.setMillisToPopup(100);
                    pm.setProgress(0);
                    EZWorker ezw = new EZWorker(pm);
                    ezw.addPropertyChangeListener((PropertyChangeEvent ev) -> {
                        if ("progress".equals(ev.getPropertyName())) {
                            int progress1 = (Integer) ev.getNewValue();
                            pm.setProgress(progress1);
                        }
                    });
                    ezw.execute();

                    //remove the listener since it's no longer needed
                    octMngr.removePropertyChangeListener(this);
                }
            }
        });
    }

    public static void findFovea(boolean interactive) {
        try {
            octMngr.findCenterOfFovea(!interactive);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(OCTAnalysisUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        octMngr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //if the change that occured was the setting (or update ) of the position of the fovea
                //then paint the fovea to the screen
                if (OCTAnalysisManager.PROP_FOVEA_CENTER_X_POSITION.equals(evt.getPropertyName())) {
                    SwingUtilities.invokeLater(() -> {
                        int fv = octMngr.getFoveaCenterXPosition();
                        OCTSelection foveaSelection = selectionLRPManager.getFoveaSelection(fv, false);
                        selectionLRPManager.addOrUpdateSelection(foveaSelection);
                        octMngr.getImgPanel().repaint();
                    });
                    //remove the listener since it's no longer needed
                    octMngr.removePropertyChangeListener(this);
                }
            }
        });
    }

    public static void performEquidistant(boolean interactive) {
        //ask for the desired distance between selections
        int micronsBetweenSelections = (int) Math.round(oct.util.Util.parseNumberFromInput((String) JOptionPane.showInputDialog(null, "Enter the desired distance between selections(microns):", "Distance between selections", JOptionPane.QUESTION_MESSAGE)));
        //calculate the center of the fovea
        try {
            octMngr.findCenterOfFovea(!interactive);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(OCTAnalysisUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        octMngr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //if the change that occured was the setting (or update ) of the position of the fovea
                if (OCTAnalysisManager.PROP_FOVEA_CENTER_X_POSITION.equals(evt.getPropertyName())) {
                    SwingUtilities.invokeLater(() -> {
                        int fv = octMngr.getFoveaCenterXPosition();
                        //add equidistant selections from fovea (including the fovea selections)
                        selectionLRPManager.addOrUpdateEquidistantSelections(fv, micronsBetweenSelections);
                        octMngr.getImgPanel().repaint();
                    });
                    //remove the listener since it's no longer needed
                    octMngr.removePropertyChangeListener(this);
                }
            }
        });
    }

    public static void performMirror(boolean interactive) {
        findFovea(interactive);
    }
    
    public static void performOSRatio(boolean interactive) {
        //first find the 'Wilk Spot' a.k.a. the greatest distance between the 
        //EZ and IZ (not necessarily the bottom of the foveal pit)
        findFovea(interactive);
    }

}
