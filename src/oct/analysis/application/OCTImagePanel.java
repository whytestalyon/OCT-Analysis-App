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
import java.awt.image.BufferedImage;
import java.util.List;
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
        if (analysisData.getOct().getOctImage() == null) {
            return super.getPreferredSize(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return new Dimension(analysisData.getOct().getOctImage().getWidth(), analysisData.getOct().getOctImage().getHeight());
        }
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        OCT oct = analysisData.getOct();
        if (oct != null && oct.getOctImage() != null) {
            //collect previous offsets
            int oldXoffset = oct.getImageOffsetX();
            int oldYoffset = oct.getImageOffsetY();
            //center the image within the panel, make sure the selection draw method takes this into account
            int imageWidth = analysisData.getOct().getOctImage().getWidth();
            int panelWidth = this.getWidth();
            int imageOffsetX = 0;
            if (panelWidth > imageWidth) {
                imageOffsetX = panelWidth / 2 - imageWidth / 2;
            }
            oct.setImageOffsetX(imageOffsetX);
            int imageHeight = analysisData.getOct().getOctImage().getHeight();
            int panelHeight = this.getHeight();
            int imageOffsetY = 0;
            if (panelHeight > imageHeight) {
                imageOffsetY = panelHeight / 2 - imageHeight / 2;
            }
            oct.setImageOffsetY(imageOffsetY);
            //draw OCT to the JPanel
            grphcs.drawImage(analysisData.getOct().getOctImage(), imageOffsetX, imageOffsetY, null);
            //update selection to new offset
            selectionLrpMngr.updateSelectionOffsets(oldXoffset, oldYoffset);
            //draw the selections to the panel if available
            selectionLrpMngr.getSelections().stream().forEach((selection) -> {
                selection.drawSelection(grphcs);
            });
        }
    }
}
