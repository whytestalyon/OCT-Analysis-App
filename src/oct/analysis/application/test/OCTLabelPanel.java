/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.test;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import javax.swing.JLabel;
import oct.analysis.application.dat.OCTAnalysisManager;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OCTLabelPanel extends JLabel {

    private final OCTAnalysisManager analysisData = OCTAnalysisManager.getInstance();
    private final HashMap<String, OCT

    public OCTLabelPanel() {
        //monitor for changes in the OCT, repaint component if change detected
        analysisData.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (OCTAnalysisManager.PROP_OCT.equals(evt.getPropertyName())) {
                //OCT has changed, repaint component to reflect new OCT
               
            }
        });
    }
    
    
}
