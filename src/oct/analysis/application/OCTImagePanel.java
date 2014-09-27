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

/**
 *
 * @author Brandon
 */
public class OCTImagePanel extends JPanel {

    private BufferedImage oct = null;
    private final int OFFSET = 1;
    private List<OCTSelection> selectionList;

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
        //draw OCT to the JPanel
        grphcs.drawImage(oct, 0, 0, null);
    }

    public void addOCTSelections(List<OCTSelection> selectionList) {
        this.selectionList = selectionList;
        selectionList.stream().forEach((o) -> {
            o.drawSelection(this.getGraphics());
        });
    }

    public void removeOCTSelection() {
        selectionList.stream().forEach((o) -> {
            repaint(o.getX_position(), o.getY_position(), o.getWidth() + OFFSET, o.getHeight());
        });
    }

    public BufferedImage getOct() {
        return oct;
    }

    public List<OCTSelection> getSelectionList() {
        return selectionList;
    }
}
