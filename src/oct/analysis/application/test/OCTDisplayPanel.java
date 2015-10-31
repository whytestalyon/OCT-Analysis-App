/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import javax.swing.JPanel;
import oct.analysis.application.dat.OCT;
import oct.analysis.application.dat.OCTAnalysisManager;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OCTDisplayPanel extends JPanel {

    private final OCTAnalysisManager analysisData = OCTAnalysisManager.getInstance();
    private final Dimension defaultPanelDimension = new Dimension(Integer.MAX_VALUE, 10);

    public OCTDisplayPanel() {
        //monitor for changes in the OCT, repaint component if change detected
        analysisData.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (OCTAnalysisManager.PROP_OCT.equals(evt.getPropertyName())) {
                //OCT has changed, repaint component to reflect new OCT
//                setMinimumSize(getMinimumSize());
//                setPreferredSize(getMinimumSize());
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        if (analysisData.getOct() != null) {
//            System.out.println("Painting! Min size: " + getMinimumSize().toString());
            grphcs.drawImage(analysisData.getOctImage(), 0, 0, null);
        }
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension min;
        if (analysisData.getOct() == null) {
            min = new Dimension(defaultPanelDimension);
        } else {
            OCT oct = analysisData.getOct();
            min = new Dimension(oct.getImageWidth(), oct.getImageHeight());
        }
//        System.out.println("dispPanel getmin: " + min);
        return min;
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getMinimumSize();
    }

}
