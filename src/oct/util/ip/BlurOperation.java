/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util.ip;

import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import oct.util.Util;

/**
 *
 * @author Brandon
 */
public class BlurOperation implements FilterOperation {

    private double blurFactor;
    private final GaussianBlur gb = new GaussianBlur();

    public BlurOperation(double blurFactor) {
        this.blurFactor = blurFactor;
    }

    public double getBlurFactor() {
        return blurFactor;
    }

    public void setBlurFactor(double blurFactor) {
        this.blurFactor = blurFactor;
    }

    @Override
    public boolean isActive() {
        return blurFactor > 0D;
    }

    @Override
    public BufferedImage performOperation(BufferedImage bi) {
        ByteProcessor bp = new ByteProcessor(Util.deepCopyBufferedImage(bi));
        bp.toFloat(0, null);
        bp.blurGaussian(blurFactor);
//        JOptionPane.showMessageDialog(null, new JLabel(new ImageIcon( bp.getBufferedImage() )) );
        return bp.getBufferedImage();
    }

}
