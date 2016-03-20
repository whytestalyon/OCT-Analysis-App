/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.MouseInputAdapter;
import oct.analysis.application.dat.AnalysisMode;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.analysis.application.dat.SelectionType;
import oct.util.Util;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OSLengthClickListener extends MouseInputAdapter {

    private static final OCTAnalysisManager octmngr = OCTAnalysisManager.getInstance();
    private static final SelectionLRPManager lrpmngr = SelectionLRPManager.getInstance();

    @Override
    public void mouseClicked(MouseEvent e) {
        if (octmngr.getImgPanel().coordinateOverlapsOCT(e.getPoint()) && octmngr.getAnalysisMode() == AnalysisMode.OS_LENGTH) {
            //record the location the user clicked on
            Point clickPoint = octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint());

            /*
             set up the dialog asking for the input information for the ROI over which the analysis 
             shoule be completed, the distance between LRPs taken, and if those measurments
             are in microns or pixels
             */
            double distanceBetweenLrp = -1, roiWidth = -1;
            boolean disIsInPixels = false, roiIsInPixels = false;
            int userOption = JOptionPane.OK_OPTION;

            do {
                JRadioButton disMicronButton = new JRadioButton("Microns", false);
                JRadioButton disPixelsButton = new JRadioButton("Pixels", true);
                ButtonGroup bg = new ButtonGroup();
                bg.add(disMicronButton);
                bg.add(disPixelsButton);
                JRadioButton roiMicronButton = new JRadioButton("Microns", true);
                JRadioButton roiPixelsButton = new JRadioButton("Pixels", false);
                ButtonGroup bg1 = new ButtonGroup();
                bg1.add(roiMicronButton);
                bg1.add(roiPixelsButton);
                JTextField field1 = new JTextField();
                JTextField field2 = new JTextField();
                Object[] message = {
                    "<html><b>Distance between LRPs:</b></html>", field2,
                    "Use:", disMicronButton, disPixelsButton,
                    " ",
                    "<html><b>ROI Width:</b></html>", field1,
                    "Use:", roiMicronButton, roiPixelsButton
                };
                userOption = JOptionPane.showConfirmDialog(octmngr.getImgPanel(), message, "Enter analysis values", JOptionPane.OK_CANCEL_OPTION);

                //check for user ok or cancel, validate input, reask if bad input
                if (userOption == JOptionPane.OK_OPTION) {
                    distanceBetweenLrp = Util.parseNumberFromInput(field1.getText());
                    roiWidth = Util.parseNumberFromInput(field2.getText());
                    disIsInPixels = disPixelsButton.isSelected();
                    roiIsInPixels = roiPixelsButton.isSelected();
                    if (distanceBetweenLrp <= 0 && roiWidth <= 0) {
                        userOption = JOptionPane.showConfirmDialog(octmngr.getImgPanel(), "Input for Distance between LRPs and ROI Width are bad, would you like to try again?", "Bad Input", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
                    } else if (distanceBetweenLrp <= 0) {
                        userOption = JOptionPane.showConfirmDialog(octmngr.getImgPanel(), "Input for Distance between LRPs is bad, would you like to try again?", "Bad Input", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
                    } else if (roiWidth <= 0) {
                        userOption = JOptionPane.showConfirmDialog(octmngr.getImgPanel(), "Input for ROI Width is bad, would you like to try again?", "Bad Input", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
                    }
                }
            } while ((distanceBetweenLrp <= 0 || roiWidth <= 0) && userOption == JOptionPane.OK_OPTION);

            //check that user wants to continue
            if (userOption == JOptionPane.OK_OPTION) {
                //input was good, proceed with analysis
                System.out.println("Results: dis = " + distanceBetweenLrp + ", roi = " + roiWidth + ", disPixels? " + disIsInPixels + ", roiPixels? " + roiIsInPixels);
            }

            octmngr.getImgPanel().removeMouseListener(this);
            octmngr.getImgPanel().repaint();
        }
        if (octmngr.getAnalysisMode() != AnalysisMode.OS_LENGTH) {
            octmngr.getImgPanel().removeMouseListener(this);
        }
    }

}
