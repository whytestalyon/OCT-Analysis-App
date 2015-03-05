/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util.ip;

import ij.process.FloatProcessor;

/**
 *
 * @author Brandon
 */
public class BlurOperation implements ImageOperation {

    private double blurFactor;

    public BlurOperation(double blurFactor) {
        this.blurFactor = blurFactor;
    }

    @Override
    public void performOperation(FloatProcessor fp) {
        fp.blurGaussian(blurFactor);
    }

    public double getBlurFactor() {
        return blurFactor;
    }

    public void setBlurFactor(double blurFactor) {
        this.blurFactor = blurFactor;
    }

}
