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
import oct.analysis.application.OCTImagePanel;

/**
 *
 * @author Brandon
 */
public class MouseListeningTextArea extends JLabel implements MouseMotionListener {

    public static final String ORIGIN_STRING = "(0,0)";
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
        if (octPanel.coordinateOverlapsOCT(p)) {
            setText("(" + p.x + "," + p.y + ")");
        } else {
            setText(ORIGIN_STRING);
        }
    }

}
