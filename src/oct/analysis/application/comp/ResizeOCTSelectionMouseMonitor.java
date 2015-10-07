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
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class ResizeOCTSelectionMouseMonitor implements MouseMotionListener {

    private final SelectionLRPManager selmngr = SelectionLRPManager.getInstance();
    private final OCTAnalysisManager octmngr = OCTAnalysisManager.getInstance();
    private OCTAnalysisUI ui = null;
    private boolean resizeTop = false;
    private boolean resizeBottom = false;

    public OCTAnalysisUI getUi() {
        return ui;
    }

    public void setUi(OCTAnalysisUI ui) {
        this.ui = ui;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (octmngr.getImgPanel() != null) {
            if (resizeBottom || resizeTop) {
                OCTSelection sel = selmngr.getSelection(octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint()).x, false);
                if (resizeBottom) {
                    int newHeight = octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint()).y - sel.getYPositionOnOct();
                    sel.setHeight(newHeight < 1 ? 1 : newHeight);
                } else {
                    int oldBottomYPoint = sel.getYPositionOnOct() + sel.getHeight();
                    sel.setYPositionOnOct(octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint()).y);
                    sel.setHeight(oldBottomYPoint - sel.getYPositionOnOct());
                }
                octmngr.getImgPanel().repaint();
                selmngr.updateLRP(sel);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //change icon displayed based on if the mouse is hovering over a resizable portion of a selection
        if (octmngr.getImgPanel() != null && ui != null) {
            Point p = octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint());
            if (selmngr.selectionTopContains(p)) {
                resizeTop = true;
                ui.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
            } else if (selmngr.selectionBottomContains(p)) {
                resizeBottom = true;
                ui.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
            } else if (resizeBottom || resizeTop) {
                resizeBottom = false;
                resizeTop = false;
                ui.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

}
