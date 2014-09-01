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
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 *
 * @author Brandon
 */
public class OCTImagePanel extends JPanel {

    private BufferedImage oct = null;
    private OCTSelection octSelection;
    private final int OFFSET = 1;

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

    public void addOCTSelection(OCTSelection o) {
        o.drawSelection(this.getGraphics());
        octSelection = o;
    }

    public void removeOCTSelection() {
        repaint(octSelection.getX_position(), octSelection.getY_position(), octSelection.getWidth()+OFFSET, octSelection.getHeight());
    }
}
