/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.dat.SelectionLRPManager;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class ResizeOCTSelectionMouseMonitor implements MouseMotionListener {

    private final SelectionLRPManager selmngr = SelectionLRPManager.getInstance();
    private OCTImagePanel octip = null;
    private OCTAnalysisUI ui = null;

    public OCTAnalysisUI getUi() {
        return ui;
    }

    public void setUi(OCTAnalysisUI ui) {
        this.ui = ui;
    }

    public OCTImagePanel getOctip() {
        return octip;
    }

    public void setOctip(OCTImagePanel octip) {
        this.octip = octip;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //change icon displayed based on if the mouse is hovering over a resizable portion of a selection
        if (octip != null && ui != null) {
            Point p = octip.translatePanelPointToOctPoint(e.getPoint());
            if (selmngr.selectionTopContains(p)) {
                ui.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
            }else if (selmngr.selectionBottomContains(p)) {
                ui.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
            } else {
                ui.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

}
