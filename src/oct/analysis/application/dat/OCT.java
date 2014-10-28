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
}
