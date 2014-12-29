/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import ij.process.ByteProcessor;
import java.awt.image.BufferedImage;
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
    private BufferedImage blurredLogOctImage;
    private BufferedImage blurredLinearOctImage;
    private int imageOffsetY = 0;
    private int imageOffsetX = 0;
    private final double logScale = 255D / Math.log(255D);
    private double blurFactor = 0;

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
        //update the blurred images
        updateBlurredImages();
    }

    public double getScale() {
        return scale;
    }

    public BufferedImage getOctImage() {
        if (blurFactor > 0) {
            return (logOct) ? blurredLogOctImage : blurredLinearOctImage;
        } else {
            return (logOct) ? logOctImage : linearOctImage;
        }
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
        this.blurFactor = blurFactor;
        updateBlurredImages();
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

    private void updateBlurredImages() {
        //store blurred version of Log OCT
        ByteProcessor bp = new ByteProcessor(Util.deepCopyBufferedImage(logOctImage));
        bp.blurGaussian(blurFactor);
        blurredLogOctImage = bp.getBufferedImage();
        //store blurred version of Linear OCT
        bp = new ByteProcessor(Util.deepCopyBufferedImage(linearOctImage));
        bp.blurGaussian(blurFactor);
        blurredLinearOctImage = bp.getBufferedImage();
    }
}
