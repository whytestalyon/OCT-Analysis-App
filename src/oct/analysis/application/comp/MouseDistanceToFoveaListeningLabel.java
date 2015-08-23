/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import javax.swing.JLabel;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.dat.OCTAnalysisManager;

/**
 *
 * @author Brandon
 */
public class MouseDistanceToFoveaListeningLabel extends JLabel implements MouseMotionListener {

    public static final String ORIGIN_STRING = "N/A";
    private static final DecimalFormat df = new DecimalFormat("#,##0.0##");
    private static final OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();
    private OCTImagePanel octPanel;

    public void setOctPanel(OCTImagePanel octPanel) {
        this.octPanel = octPanel;
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        if (octPanel.coordinateOverlapsOCT(p) && octMngr.getFoveaCenterXPosition() > 0) {
            p = octPanel.translatePanelPointToOctPoint(p);
            setText(df.format(octMngr.getDistanceFromFovea(p.x))+" \u00B5m");
        } else {
            setText(ORIGIN_STRING);
        }
    }

}
