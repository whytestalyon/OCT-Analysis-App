/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Color;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SegmentationManager;
import oct.util.Line;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OSRatioClickListener extends MouseInputAdapter {

    private static final SegmentationManager segmngr = SegmentationManager.getInstance();
    private static final OCTAnalysisManager octmngr = OCTAnalysisManager.getInstance();

    @Override
    public void mouseClicked(MouseEvent e) {
        if (octmngr.getImgPanel().coordinateOverlapsOCT(e.getPoint())) {
            Line nearestLine = segmngr.getLineIfNearPoint(octmngr.getImgPanel().translatePanelPointToOctPoint(e.getPoint()));
            if (nearestLine != null && !segmngr.getSelectedSegLines().contains(nearestLine)) {
                segmngr.addSelectedSeg(nearestLine);
                nearestLine.setDrawColor(Color.cyan);
                octmngr.getImgPanel().repaint();
            }
        }
    }

}
