/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

/**
 *
 * @author Brandon
 */
public class OCTMetrics {

    private double axialLength = -1;
    private final double scale;
    private double nominalScanWidth = -1;

    public OCTMetrics(double scale) {
        this.scale = scale;
    }

    public OCTMetrics(double axialLength, double nominalScanWidth, int octWidth) {
        this.axialLength = axialLength;
        this.nominalScanWidth = nominalScanWidth;
        double scanLength = (nominalScanWidth * axialLength) / 24D;
        scale = ((scanLength * 1000D) / (double) octWidth);
    }

    public double getAxialLength() {
        return axialLength;
    }

    public double getScale() {
        return scale;
    }

    public double getNominalScanLength() {
        return nominalScanWidth;
    }
}
