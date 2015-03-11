/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import oct.analysis.application.dat.OCT;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;

/**
 *
 * @author Brandon
 */
public class OCTImagePanel extends JPanel {

    private final OCTAnalysisManager analysisData = OCTAnalysisManager.getInstance();
    private final SelectionLRPManager selectionLrpMngr = SelectionLRPManager.getInstance();
    private int imageOffsetY = 0;
    private int imageOffsetX = 0;

    public OCTImagePanel(LayoutManager lm, boolean bln) {
        super(lm, bln);
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public OCTImagePanel(LayoutManager lm) {
        super(lm);
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public OCTImagePanel(boolean bln) {
        super(bln);
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public OCTImagePanel() {
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    @Override
    public Dimension getPreferredSize() {
        if (analysisData.getOct() == null) {
            return super.getPreferredSize(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return new Dimension(analysisData.getOct().getImageWidth(), analysisData.getOct().getImageHeight());
        }
    }
    
    public void resetImageOffsets(){
        imageOffsetY = imageOffsetX = 0;
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        OCT oct = analysisData.getOct();
        if (oct != null) {
            //center the image within the panel, make sure the selection draw method takes this into account
            int imageWidth = oct.getImageWidth();
            int panelWidth = this.getWidth();
            if (panelWidth > imageWidth) {
                imageOffsetX = panelWidth / 2 - imageWidth / 2;
            }
            int imageHeight = oct.getImageHeight();
            int panelHeight = this.getHeight();
            if (panelHeight > imageHeight) {
                imageOffsetY = panelHeight / 2 - imageHeight / 2;
            }
            //draw OCT to the JPanel
            grphcs.drawImage(analysisData.getOctImage(), imageOffsetX, imageOffsetY, null);
            //draw the selections to the panel if available
            selectionLrpMngr.getSelections().stream().forEach((selection) -> {
                selection.drawSelection(grphcs, imageOffsetX, imageOffsetY);
            });
        }
    }

    /**
     * Determine if the supplied coordinate overlaps with the area of this panel
     * that displays the OCT image
     *
     * @param x
     * @param y
     * @return true if the coordinate is within the bounds of the displayed OCT,
     * false if it isn't or if the OCT image isn't displayed already
     */
    public boolean coordinateOverlapsOCT(int x, int y) {
        OCT oct = analysisData.getOct();
        if (oct != null) {
            boolean withinX = ((imageOffsetX + oct.getImageWidth()) - x) * (x - imageOffsetX) > -1;
            boolean withinY = ((imageOffsetY + oct.getImageHeight()) - y) * (y - imageOffsetY) > -1;
            return withinX && withinY;
        } else {
            return false;
        }
    }
}
