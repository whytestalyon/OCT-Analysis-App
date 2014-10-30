/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.awt.image.BufferedImage;

/**
 *
 * @author Brandon
 */
public class OCT {

    private final double scale;
    private final BufferedImage octImage;
    private int imageOffsetY = 0;
    private int imageOffsetX = 0;

    public OCT(double scale, BufferedImage octImage) {
        this.scale = scale;
        this.octImage = octImage;
    }

    public OCT(double axialLength, double nominalScanWidth, int octWidth, BufferedImage octImage) {
        double scanLength = (nominalScanWidth * axialLength) / 24D;
        scale = ((scanLength * 1000D) / (double) octWidth);
        this.octImage = octImage;
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
        if (octImage == null) {
            return false;
        }
        boolean withinX = ((imageOffsetX + octImage.getWidth()) - x) * (x - imageOffsetX) > -1;
        boolean withinY = ((imageOffsetY + octImage.getHeight()) - y) * (y - imageOffsetY) > -1;
        return withinX && withinY;
    }
}
