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

    private final BufferedImage oct;

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

    @Override
    public Dimension getPreferredSize() {
        /*
         Dimensions method overriden so we can have the JFrame automatically resize
         to the image once this JPanel is added to the JFrame. This makes it easy
         to account for differences in the size of OCT images for display in the UI. 
         */
        return new Dimension(oct.getWidth(), oct.getHeight());
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        //draw OCT to the JPanel
        grphcs.drawImage(oct, 0, 0, this);              
    }

}
