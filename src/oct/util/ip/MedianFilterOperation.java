/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util.ip;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import oct.util.Util;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class MedianFilterOperation implements CustomOperation {

    private boolean active = false;

    @Override
    public boolean isActive() {
        return active;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    @Override
    public void performOperation(BufferedImage bi) {
        int[][] pixels = Util.convertTo2D(bi);
        //convert to grayscale
        for (int[] pixelColumn : pixels) {
            for (int y = 0; y < pixelColumn.length; y++) {
                int pixel = Util.calculateGrayScaleValue(pixelColumn[y]);
                pixelColumn[y] = pixel;
            }
        }
        //median filter
        int[] krnl = new int[9];
        for (int x = 1; x < pixels.length - 1; x++) {
            for (int y = 1; y < pixels[x].length - 1; y++) {
                krnl[0] = pixels[x - 1][y - 1];
                krnl[1] = pixels[x][y - 1];
                krnl[2] = pixels[x + 1][y - 1];
                krnl[3] = pixels[x - 1][y];
                krnl[4] = pixels[x][y];
                krnl[5] = pixels[x + 1][y];
                krnl[6] = pixels[x - 1][y + 1];
                krnl[7] = pixels[x][y + 1];
                krnl[8] = pixels[x + 1][y + 1];
                pixels[x][y] = Arrays.stream(krnl).sorted().toArray()[4];
            }
        }
        //update the image
        for (int x = 1; x < pixels.length - 1; x++) {
            for (int y = 1; y < pixels[x].length - 1; y++) {
                bi.setRGB(x, y, Util.calculateRGBValue(pixels[x][y]));
            }
        }
    }

}
