/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class ResizeOCTSelectionMouseMonitor extends MouseInputAdapter {

    private final SelectionLRPManager selmngr = SelectionLRPManager.getInstance();
    private final OCTAnalysisManager octmngr = OCTAnalysisManager.getInstance();
    private OCTAnalysisUI ui = null;
    private boolean resizeTop = false;
    private boolean resizeBottom = false;
    private OCTSelection sel = null;

    public OCTAnalysisUI getUi() {
        return ui;
    }

    public void setUi(OCTAnalysisUI ui) {
        this.ui = ui;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (octmngr.getImgPanel() != null) {
            System.out.println("Mouse dragged: top = " + resizeTop + ", bottom = " + resizeBottom);
            if (sel != null) {
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
                ui.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
            } else if (selmngr.selectionBottomContains(p)) {
                ui.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
            } else if (!(resizeBottom || resizeTop)) {
                ui.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (octmngr.getImgPanel() != null && ui != null) {
            Point p = octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint());
            if (selmngr.selectionTopContains(p)) {
                resizeTop = true;
            } else if (selmngr.selectionBottomContains(p)) {
                resizeBottom = true;
            }
            sel = selmngr.getSelection(octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint()).x, false);
            System.out.println("Mouse pressed: top = " + resizeTop + ", bottom = " + resizeBottom);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        resizeBottom = false;
        resizeTop = false;
        sel = null;
        System.out.println("Mouse released!");
    }

}
