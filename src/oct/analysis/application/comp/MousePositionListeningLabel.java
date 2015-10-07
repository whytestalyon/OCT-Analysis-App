/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JLabel;
import oct.analysis.application.dat.OCTAnalysisManager;

/**
 *
 * @author Brandon
 */
public class MousePositionListeningLabel extends JLabel implements MouseMotionListener {

    public static final String ORIGIN_STRING = "(0,0)";
    private static final OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();
    private OCTAnalysisManager octmngr = OCTAnalysisManager.getInstance();

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        if (octmngr.getImgPanel() != null) {
            if (octmngr.getImgPanel().coordinateOverlapsOCT(p)) {
                p = octmngr.getImgPanel().translatePanelPointToOctPoint(p);
                setText("(" + p.x + "," + (octMngr.getOct().getImageHeight() - p.y) + ")");
            } else {
                setText(ORIGIN_STRING);
            }
        }
    }

}
