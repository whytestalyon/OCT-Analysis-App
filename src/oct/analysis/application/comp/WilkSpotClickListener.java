/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;
import oct.analysis.application.dat.AnalysisMode;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.analysis.application.dat.SelectionType;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class WilkSpotClickListener extends MouseInputAdapter {

    private static final OCTAnalysisManager octmngr = OCTAnalysisManager.getInstance();
    private static final SelectionLRPManager lrpmngr = SelectionLRPManager.getInstance();

    @Override
    public void mouseClicked(MouseEvent e) {
        if (octmngr.getImgPanel().coordinateOverlapsOCT(e.getPoint()) && octmngr.getAnalysisMode() == AnalysisMode.WILK_SPOT) {
            Point clickPoint = octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint());
            //grab LRPs centered at the x position supplied and centered at the 
            //3 x positions on either side
            lrpmngr.addOrUpdateSelection(lrpmngr.getSelection(clickPoint.x, "Wilk Spot", SelectionType.NONFOVEAL, false));
            for (int i = 1; i < 4; i++) {
                lrpmngr.addOrUpdateSelection(lrpmngr.getSelection(clickPoint.x + i, "Wilk Spot + " + i, SelectionType.NONFOVEAL, false));
                lrpmngr.addOrUpdateSelection(lrpmngr.getSelection(clickPoint.x - i, "Wilk Spot - " + i, SelectionType.NONFOVEAL, false));
            }
            octmngr.getImgPanel().removeMouseListener(this);
            octmngr.getImgPanel().repaint();
        }
        if (octmngr.getAnalysisMode() != AnalysisMode.WILK_SPOT) {
            octmngr.getImgPanel().removeMouseListener(this);
        }
    }

}
