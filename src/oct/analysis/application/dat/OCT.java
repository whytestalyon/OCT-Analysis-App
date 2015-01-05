/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import ij.plugin.filter.UnsharpMask;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import oct.util.Util;

/**
 *
 * @author Brandon
 */
public class OCT {

    private boolean logOct = true;
    private double scale;
    private BufferedImage logOctImage;
    private BufferedImage linearOctImage;
    private int imageOffsetY = 0;
    private int imageOffsetX = 0;
    private final double logScale = 255D / Math.log(255D);
    private double blurFactor = 0;
    private volatile boolean updateBlur = false;
    private double sharpenSigma = 0; //"Radius (Sigma)" is the standard deviation (blur radius) of the Gaussian blur that is subtracted.
    private float sharpenWeight = 0; //Mask Weight" determines the strength of filtering, where "Mask Weight"=1 would be an infinite weight of the high-pass filtered image that is added.
    private volatile boolean updateSharpen = true;
    private FloatProcessor logOctFP;
    private FloatProcessor linearOctFP;
    private final UnsharpMask sharpener = new UnsharpMask();

    public OCT(double scale, BufferedImage octImage) {
        init(scale, octImage);
    }

    public OCT(double axialLength, double nominalScanWidth, int octWidth, BufferedImage octImage) {
        double scanLength = (nominalScanWidth * axialLength) / 24D;
        init(((scanLength * 1000D) / (double) octWidth), octImage);
    }

    private void init(double scale, BufferedImage octImage) {
        this.scale = scale;
        this.logOctImage = octImage;
        //store liner scale version of OCT
        linearOctImage = getLinearOCT(octImage);
        //store image processors for various filters
        logOctFP = new ByteProcessor(logOctImage).convertToFloatProcessor();
        linearOctFP = new ByteProcessor(linearOctImage).convertToFloatProcessor();
        //initialize snapshots of the original images to reset to later
        logOctFP.snapshot();
        linearOctFP.snapshot();
    }

    public double getScale() {
        return scale;
    }

    public BufferedImage getOctImage() {
        //determine if operations need be applied (or changed from previous values)
        if (updateBlur || updateSharpen) {
            updateBlur = updateSharpen = false;
            logOctFP.reset();//return to original image
            linearOctFP.reset();//return to original image
            if (blurFactor > 0) {
                logOctFP.blurGaussian(blurFactor);
                linearOctFP.blurGaussian(blurFactor);
            }
            if (sharpenSigma > 0 && sharpenWeight > 0) {
                sharpener.sharpenFloat(logOctFP, sharpenSigma, sharpenWeight);
                sharpener.sharpenFloat(linearOctFP, sharpenSigma, sharpenWeight);
//            JFrame frame = new JFrame();
//            frame.getContentPane().setLayout(new FlowLayout());
//            frame.getContentPane().add(new JLabel(new ImageIcon(bp.getBufferedImage())));
//            frame.getContentPane().add(new JLabel(new ImageIcon(fp.getBufferedImage())));
//            frame.pack();
//            frame.setVisible(true);
            }
        }
        return (logOct) ? logOctFP.getBufferedImage() : linearOctFP.getBufferedImage();
    }

    public int getImageOffsetY() {
        return imageOffsetY;
    }

    public void setImageOffsetY(int imageOffsetY) {
        this.imageOffsetY = imageOffsetY;
    }

    public int getImageOffsetX() {
        return imageOffsetX;
    }

    public void setImageOffsetX(int imageOffsetX) {
        this.imageOffsetX = imageOffsetX;
    }

    public void transformOCTToLinear() {
        logOct = false;
    }

    public void transformOCTToLogrithmic() {
        logOct = true;
    }

    public boolean isLogOct() {
        return logOct;
    }

    public boolean isLinearOct() {
        return !logOct;
    }

    public double getBlurFactor() {
        return blurFactor;
    }

    public void setBlurFactor(double blurFactor) {
        if (blurFactor != this.blurFactor) {
            this.blurFactor = blurFactor;
            updateBlur = true;
        }
    }

    public void setSharpenSigma(double sharpenSigma) {
        if (this.sharpenSigma != sharpenSigma) {
            this.sharpenSigma = sharpenSigma;
            updateSharpen = true;
        }
    }

    public void setSharpenWeight(float sharpenWeight) {
        if (sharpenWeight < 0 || sharpenWeight > 1) {
            throw new IllegalArgumentException("Illegal Sharpen weight of " + sharpenWeight + ", must be between 0 and 1.");
        }
        if (this.sharpenWeight != sharpenWeight) {
            this.sharpenWeight = sharpenWeight;
            updateSharpen = true;
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
        if (logOctImage == null) {
            return false;
        }
        boolean withinX = ((imageOffsetX + logOctImage.getWidth()) - x) * (x - imageOffsetX) > -1;
        boolean withinY = ((imageOffsetY + logOctImage.getHeight()) - y) * (y - imageOffsetY) > -1;
        return withinX && withinY;
    }

    private BufferedImage getLinearOCT(BufferedImage logOCT) {
        BufferedImage retImg = Util.deepCopyBufferedImage(logOCT);
        int w = retImg.getWidth();
        int h = retImg.getHeight();
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int pixel = retImg.getRGB(j, i);
                retImg.setRGB(j, i, exp(pixel));
            }
        }
        return retImg;
    }

    private int exp(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        int ret = (rgb & 0xFF000000) | (((int) Math.exp((double) r / logScale)) << 16) | (((int) Math.exp((double) g / logScale)) << 8) | ((int) Math.exp((double) b / logScale));
        return ret;
    }

    private int ln(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        int ret = (rgb & 0xFF000000) | (((int) (Math.log(r) * logScale)) << 16) | (((int) (Math.log(g) * logScale)) << 8) | ((int) (Math.log(b) * logScale));
        return ret;
    }

}
