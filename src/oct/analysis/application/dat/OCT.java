package oct.analysis.application.dat;

import ij.ImagePlus;
import ij.process.ImageConverter;
import java.awt.Point;
import java.awt.image.BufferedImage;
import oct.util.Util;

/**
 *
 * @author Brandon
 */
public class OCT {

    private final BufferedImage logOctImage;
    private final BufferedImage linearOctImage;
    private final double logScale = 255D / Math.log(255D);

    /**
     * Creates a simple data structure for caching the log and linear versions
     * of an OCT image.
     *
     * @param octImage the log OCT image to be cached
     */
    public OCT(BufferedImage octImage) {
        //segmentation as well as some image operations can only be done on 
        //8-bit gray scale images, ensure image is in useable format for application
        ImagePlus ip = new ImagePlus("", octImage);
        if (ip.getBitDepth() != 8) {
            ImageConverter ic = new ImageConverter(ip);
            ic.convertToGray8();
        }
        //store log OCT image
        logOctImage = ip.getBufferedImage();
        //store liner scale version of OCT
        linearOctImage = getLinearOCT(logOctImage);
    }

    /**
     * Get the logrithmic version of the OCT
     *
     * @return
     */
    public BufferedImage getLogOctImage() {
        return logOctImage;
    }

    /**
     * Get the linear version of the OCT
     *
     * @return
     */
    public BufferedImage getLinearOctImage() {
        return linearOctImage;
    }

    /**
     * Get the width of the OCT image
     *
     * @return
     */
    public int getImageWidth() {
        return logOctImage.getWidth();
    }

    /**
     * Get the height of the OCT image
     *
     * @return
     */
    public int getImageHeight() {
        return logOctImage.getHeight();
    }

    /**
     * Get a deep copy of the supplied image after each pixel has been converted
     * to a linear value.
     *
     * @param logOCT
     * @return
     */
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

    /**
     * Calculate the exponential of an RGB value to obtain the original linear
     * value.
     *
     * @param rgb
     * @return
     */
    private int exp(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        int ret = (rgb & 0xFF000000) | (((int) Math.exp((double) r / logScale)) << 16) | (((int) Math.exp((double) g / logScale)) << 8) | ((int) Math.exp((double) b / logScale));
        return ret;
    }

    /**
     * Calculate the logrithmic value of an assumed linear RGB value.
     *
     * @param rgb
     * @return
     */
    private int ln(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        int ret = (rgb & 0xFF000000) | (((int) (Math.log(r) * logScale)) << 16) | (((int) (Math.log(g) * logScale)) << 8) | ((int) (Math.log(b) * logScale));
        return ret;
    }

}
