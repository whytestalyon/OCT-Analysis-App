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

    private static final UnsharpMask sharpener = new UnsharpMask();
    ; //Unsharp mask used to perform the sharpening operation
    private double sharpenSigma = 0; //"Radius (Sigma)" is the standard deviation (blur radius) of the Gaussian blur that is subtracted.
    private float sharpenWeight = 0; //Mask Weight" determines the strength of filtering, where "Mask Weight"=1 would be an infinite weight of the high-pass filtered image that is added.
    BlurOperation bo;

    public SharpenOperation(double sharpenSigma, float sharpenWeight) {
        this.bo = new BlurOperation(sharpenSigma);
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

        BlurOperation blur = new BlurOperation(sharpenSigma);
        BufferedImage sharpBi = blur.performOperation(bi);
        for (int y = 0; y < sharpBi.getHeight(); y++) {
            for (int x = 0; x < sharpBi.getWidth(); x++) {
                float oldPix = Util.calculateGrayScaleValue(bi.getRGB(x, y));
                float newPix = Util.calculateGrayScaleValue(sharpBi.getRGB(x, y));
                float sharpPix = (oldPix - sharpenWeight * newPix) / (1f - sharpenWeight);
                if (sharpPix > 255) {
                    sharpBi.setRGB(x, y, Util.calculateRGBValue(255));
                } else if (sharpPix < 0) {
                    sharpBi.setRGB(x, y, Util.calculateRGBValue(0));
                } else {
                    sharpBi.setRGB(x, y, Util.calculateRGBValue((int) sharpPix));
                }
            }
        }
        return sharpBi;
    }

    public static BufferedImage performSharpenUsingImageJFloatProcessor(double sharpenSigma, float sharpenWeight, BufferedImage bi) {
        FloatProcessor fp = new ByteProcessor(bi).convertToFloatProcessor();
        fp.snapshot();
        new UnsharpMask().sharpenFloat(fp, sharpenSigma, sharpenWeight);
        return fp.getBufferedImage();
    }
}
