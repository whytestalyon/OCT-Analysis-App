/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.util.List;
import javax.swing.JOptionPane;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.OCTLine;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;

/**
 *
 * @author Brandon
 */
public class FindFoveaDialog {

    private final OCTImagePanel imgPanel;
    private final OCTAnalysisManager analysisMngr = OCTAnalysisManager.getInstance();
    private final SelectionLRPManager selMngr = SelectionLRPManager.getInstance();
    private Integer foveaXcoord;

    public FindFoveaDialog(OCTImagePanel imgPanel) {
        this.imgPanel = imgPanel;
    }

    /**
     * This method will take care of interacting with the user in determining
     * where the fovea is within the OCT. It first lets the user inspect the
     * automatically identified locations where the fovea may be and then choose
     * the selection that is at the fovea. If none of the automated findings are
     * at the fovea the user has the option to manual specify it's location.
     * Finally, the chosen X coordinate (within the OCT) of the fovea is
     * returned.
     *
     * @param fullAutoMode find the fovea automatically without user input when
     * true, otherwise find the fovea in semi-automatic mode involving user
     * interaction
     * @return the X coordinate (within the OCT) of the fovea
     */
    public int findFovea(boolean fullAutoMode) {
        //automatically find the possible locations of the fovea
        List<Integer> foveaPoints = analysisMngr.findPossibleFoveaPoints();
        foveaXcoord = foveaPoints.get(0);
        if (!fullAutoMode) {
            //draw potential fovea selections to screen for user to choose from
            foveaPoints.forEach(x -> {
                selMngr.addOrUpdateSelection(new OCTLine(x, 0, analysisMngr.getOct().getImageHeight(), OCTSelection.PERIPHERAL_SELECTION, "Potential Fovea"));
            });
            imgPanel.repaint();
            //notify user of how they can select the selection that is the fovea or make a new selection
            JOptionPane.showMessageDialog(imgPanel, "Please select (by clicking one of the gray boxes at the top "
                    + "of a selection) the selection that you believe is the fovea. If none of the presented "
                    + "seletions look like the location of the fovea click anywhere on the image to assign "
                    + "the location manually.",
                    "Select Fovea",
                    JOptionPane.INFORMATION_MESSAGE);
            //add mouse listener to determine where the click was and act accordingly
            imgPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    //determine if click was over one of the possible fovea selections
                    OCTSelection selection = selMngr.getOverlappingSelection(evt.getX(), evt.getY(), imgPanel.getImageOffsetX(), imgPanel.getImageOffsetY());
                    //clear all selections from being displayed
                    selMngr.removeSelections(true);
                    imgPanel.repaint();
                    if (selection == null) {
                        //user decided that none of the automated selections was correct and made their own selection
                        //check that new selection is what they want
                        do {
                            selMngr.addOrUpdateSelection(new OCTLine(evt.getX(), 0, analysisMngr.getOct().getImageHeight(), OCTSelection.PERIPHERAL_SELECTION, "Potential Fovea"));
                        } while (JOptionPane.showConfirmDialog(imgPanel, "Is this the location of the center of the fovea? If not hit 'No' and click on the image where you believe the center of the fovea resides.", "Center of Fovea?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION);
                    } else {
                        //user selected a selection to represent the fovea

                        //set x position of selected selection as center of fovea
                        foveaXcoord = selection.getXPositionOnOct();
                    }
                }
            });
        }
        return foveaXcoord;
    }

}
