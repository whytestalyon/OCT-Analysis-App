/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util.ip;

import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.UnsharpMask;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import oct.util.Util;

/**
 *
 * @author Brandon
 */
public class SharpenOperation implements FilterOperation {

    private final UnsharpMask sharpener; //Unsharp mask used to perform the sharpening operation
    private double sharpenSigma = 0; //"Radius (Sigma)" is the standard deviation (blur radius) of the Gaussian blur that is subtracted.
    private float sharpenWeight = 0; //Mask Weight" determines the strength of filtering, where "Mask Weight"=1 would be an infinite weight of the high-pass filtered image that is added.
    BlurOperation bo;

    public SharpenOperation(double sharpenSigma, float sharpenWeight) {
        this.bo = new BlurOperation(sharpenSigma);
        this.sharpener = new UnsharpMask();
        this.sharpenSigma = sharpenSigma;
        this.sharpenWeight = sharpenWeight;
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

    @Override
    public boolean isActive() {
        return sharpenWeight > 0F && sharpenSigma > 0D;
    }

    @Override
    public BufferedImage performOperation(BufferedImage bi) {
        FloatProcessor fp = new ByteProcessor(bi).convertToFloatProcessor();
        fp.snapshot();
        new UnsharpMask().sharpenFloat(fp, sharpenSigma, sharpenWeight);
        return fp.getBufferedImage();
    }

    /**
     * Unsharp Mask filtering of a float image. 'fp' must have a valid snapshot.
     * Method cannibalized from the ImageJ v1.49 source code from UnsharpMask.
     */
    private void sharpenFloat(FloatProcessor fp) {
        fp.snapshot();
        float[] pixels = (float[]) fp.getPixels();
        float[] snapshotPixels = (float[]) fp.getSnapshotPixels();
        int width = fp.getWidth();
        Rectangle roi = fp.getRoi();
        for (int y = roi.y; y < roi.y + roi.height; y++) {
            for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                pixels[p] = (snapshotPixels[p] - sharpenWeight * pixels[p]) / (1f - sharpenWeight);
            }
        }
    }

}
