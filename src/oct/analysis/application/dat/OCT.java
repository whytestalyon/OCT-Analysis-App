/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import ij.process.ByteProcessor;
import java.awt.image.BufferedImage;
import oct.io.Util;

/**
 *
 * @author Brandon
 */
public class OCT {

    private final double scale;
    private BufferedImage octImage;
    private final BufferedImage logOctImage;
    private final BufferedImage linearOctImage;
    private int imageOffsetY = 0;
    private int imageOffsetX = 0;
    private final double logScale = 255D / Math.log(255D);

    public OCT(double scale, BufferedImage octImage) {
        this.scale = scale;
        ByteProcessor bp = new ByteProcessor(octImage);
        bp.blurGaussian(1.1);
        logOctImage = bp.getBufferedImage();
        this.octImage = octImage;
        linearOctImage = getLinearOCT(logOctImage);
    }

    public OCT(double axialLength, double nominalScanWidth, int octWidth, BufferedImage octImage) {
        double scanLength = (nominalScanWidth * axialLength) / 24D;
        scale = ((scanLength * 1000D) / (double) octWidth);
        ByteProcessor bp = new ByteProcessor(octImage);
        bp.blurGaussian(1.1);
        logOctImage = bp.getBufferedImage();
        this.octImage = octImage;
        linearOctImage = getLinearOCT(logOctImage);
    }

    public double getScale() {
        return scale;
    }

    public BufferedImage getOctImage() {
        return octImage;
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
        octImage = linearOctImage;
    }

    public void transformOCTToLogrithmic() {
        octImage = logOctImage;
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
