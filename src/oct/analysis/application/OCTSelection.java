/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Brandon
 */
public class OCTSelection {

    public static final int FOVEAL_SELECTION = 0;
    public static final int PERIPHERAL_SELECTION = 1;
    public static final int SMOOTHING_FACTOR = 5; // smoothing: the strength of the smoothing filter; 1=no change, larger values smoothes more
    private final String selectionName;
    private final int selectionType;
    private int x_position;
    private int y_position;
    private final int width;
    private final int height;

    public OCTSelection(int x_position, int y_position, int width, int height, int selectionType, String selectionName) {
        this.x_position = x_position;
        this.y_position = y_position;
        this.width = width;
        this.height = height;
        this.selectionType = selectionType;
        this.selectionName = selectionName;
    }

    public void drawSelection(Graphics g) {
        g.setColor(Color.green);
        System.out.println("Drawing selection at x: " + x_position + ", y: " + y_position + ", w: " + width + ", h: " + (height - 1));
        g.drawRect(x_position, y_position, width, height - 1);
    }

    public int getX_position() {
        return x_position;
    }

    public int getY_position() {
        return y_position;
    }

    public void setX_position(int x_position) {
        this.x_position = x_position;
    }

    public void setY_position(int y_position) {
        this.y_position = y_position;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSelectionType() {
        return selectionType;
    }

    public String getSelectionName() {
        return selectionName;
    }

    public XYSeries getLrpSeriesFromOCT(BufferedImage oct) {
        if (x_position + width > oct.getWidth()) {
            return null;
        }
        XYSeries lrp = new XYSeries(selectionName + " LRP");
        int value = Integer.MAX_VALUE;
        //iterate over each row of pixels in the selection area and calculate average pixel intensity
        for (int y = height - 1; y >= 0; y--) {
            int sum = 0;

            for (int xindex = x_position + 1; xindex < x_position + 1 + width; xindex++) {
                sum += calculateGrayScaleValue(oct.getRGB(xindex, y));
            }
            //calculate average pixel intensity
            int avg = sum / width;
            //smooth the LRP to provide a higher quality LRP signal
            if (value == Integer.MAX_VALUE) {
                //initialize the first value for the smoothing filter
                value = avg;
            } else {
                //smooth the LRP signal
                value += ((avg - value) / SMOOTHING_FACTOR);
                avg = value;
            }
            //add LRP value to return series
            lrp.add(y, avg);
        }

        return lrp;
    }

    /**
     * Determine the gray scale value of a pixel based on its RGB value.
     *
     * @param rgb
     * @return
     */
    private int calculateGrayScaleValue(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);

        int grayLevel = (r + g + b) / 3;
        return grayLevel;
    }

}
