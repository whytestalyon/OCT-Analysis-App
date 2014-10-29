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
import oct.analysis.application.calc.SelectionUtil;
import oct.analysis.application.dat.OCTAnalysisDAO;

/**
 *
 * @author Brandon
 */
public class OCTImagePanel extends JPanel {

    private BufferedImage oct = null;
    private List<OCTSelection> selectionList = null;
    private final OCTAnalysisDAO analysisMetrics = OCTAnalysisDAO.getInstance();
    private int imageOffsetY = 0;
    private int imageOffsetX = 0;

    public OCTImagePanel(BufferedImage oct, LayoutManager lm, boolean bln) {
        super(lm, bln);
        setBorder(BorderFactory.createLineBorder(Color.black));
        this.oct = oct;
    }

    public OCTImagePanel(BufferedImage oct, LayoutManager lm) {
        super(lm);
        setBorder(BorderFactory.createLineBorder(Color.black));
        this.oct = oct;
    }

    public OCTImagePanel(BufferedImage oct, boolean bln) {
        super(bln);
        setBorder(BorderFactory.createLineBorder(Color.black));
        this.oct = oct;
    }

    public OCTImagePanel(BufferedImage oct) {
        setBorder(BorderFactory.createLineBorder(Color.black));
        this.oct = oct;
    }

    public OCTImagePanel() {
    }

    public void setOct(BufferedImage oct) {
        this.oct = oct;
    }

    @Override
    public Dimension getPreferredSize() {
        if (oct == null) {
            return super.getPreferredSize(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return new Dimension(oct.getWidth(), oct.getHeight());
        }
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        if (oct != null) {
            //collect previous offsets
            int oldXoffset = imageOffsetX;
            int oldYoffset = imageOffsetY;
            //center the image within the panel, make sure the selection draw method takes this into account
            int imageWidth = oct.getWidth();
            int panelWidth = this.getWidth();
            imageOffsetX = 0;
            if (panelWidth > imageWidth) {
                imageOffsetX = panelWidth / 2 - imageWidth / 2;
            }
            int imageHeight = oct.getHeight();
            int panelHeight = this.getHeight();
            imageOffsetY = 0;
            if (panelHeight > imageHeight) {
                imageOffsetY = panelHeight / 2 - imageHeight / 2;
            }
            //draw OCT to the JPanel
            grphcs.drawImage(oct, imageOffsetX, imageOffsetY, null);
            //draw the selections to the panel if available
            if (selectionList != null) {
                selectionList.stream().forEach((selection) -> {
                    //update selection to new offset
                    selection.setX_position(selection.getX_position() - oldXoffset + imageOffsetX);
                    selection.setY_position(selection.getY_position() - oldYoffset + imageOffsetY);
                    selection.drawSelection(grphcs);
                });
            }
        }
    }

    public void addOCTSelectionsToPanel() {
        selectionList = SelectionUtil.getSelectionsFromFoveaSelection(analysisMetrics.getFoveaSelection(), oct.getWidth(), analysisMetrics.getDistanceBetweenSelections(), imageOffsetX, imageOffsetY);
        this.repaint();
    }

    public void removeOCTSelection() {
        selectionList = null;
        this.repaint();
    }

    public BufferedImage getOct() {
        return oct;
    }

    public void updateOCTSelections() {
        removeOCTSelection();
        addOCTSelectionsToPanel();
    }

    public List<OCTSelection> getSelectionList() {
        return selectionList;
    }

    public int getImageOffsetY() {
        return imageOffsetY;
    }

    public int getImageOffsetX() {
        return imageOffsetX;
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
        if (oct == null) {
            return false;
        }
        boolean withinX = ((imageOffsetX + oct.getWidth()) - x) * (x - imageOffsetX) > -1;
        boolean withinY = ((imageOffsetY + oct.getHeight()) - y) * (y - imageOffsetY) > -1;
        return withinX && withinY;
    }
}
