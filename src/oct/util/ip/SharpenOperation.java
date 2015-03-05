/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util.ip;

import ij.plugin.filter.UnsharpMask;
import ij.process.FloatProcessor;

/**
 *
 * @author Brandon
 */
public class SharpenOperation implements ImageOperation {

    private final UnsharpMask sharpener; //Unsharp mask used to perform the sharpening operation
    private double sharpenSigma; //"Radius (Sigma)" is the standard deviation (blur radius) of the Gaussian blur that is subtracted.
    private float sharpenWeight; //Mask Weight" determines the strength of filtering, where "Mask Weight"=1 would be an infinite weight of the high-pass filtered image that is added.

    public SharpenOperation(double sharpenSigma, float sharpenWeight) {
        this.sharpener = new UnsharpMask();
        this.sharpenSigma = sharpenSigma;
        this.sharpenWeight = sharpenWeight;
    }

    @Override
    public void performOperation(FloatProcessor fp) {
        sharpener.sharpenFloat(fp, sharpenSigma, sharpenWeight);
    }

    public double getSharpenSigma() {
        return sharpenSigma;
    }

    public float getSharpenWeight() {
        return sharpenWeight;
    }

    public void setSharpenSigma(double sharpenSigma) {
        this.sharpenSigma = sharpenSigma;
    }

    public void setSharpenWeight(float sharpenWeight) {
        this.sharpenWeight = sharpenWeight;
    }

}
