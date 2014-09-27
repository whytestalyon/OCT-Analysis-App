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

    private final int x_position;
    private final int y_position;
    private final int width;
    private final int height;

    public OCTSelection(int x_position, int y_position, int width, int height) {
        this.x_position = x_position;
        this.y_position = y_position;
        this.width = width;
        this.height = height;
    }

    public void drawSelection(Graphics g) {
        g.setColor(Color.green);
        g.drawRect(x_position, y_position, width, height);
    }

    public int getX_position() {
        return x_position;
    }

    public int getY_position() {
        return y_position;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public XYSeries getLrpSeriesFromOCT(BufferedImage oct) {
        if (x_position + width > oct.getWidth()) {
            return null;
        }
        XYSeries lrp = new XYSeries("LRP");
        //iterate over each row of pixels in the selection area and calculate average pixel intensity
        for (int y = height-1; y >= 0; y--) {
            double sum = 0;

            for (int xindex = x_position + 1; xindex < x_position + 1 + width; xindex++) {
                sum += (double) oct.getRGB(xindex, y);
            }
            //calculate average pixel intensity
            double avg = sum / (double) width;
//            System.out.println("Avg: " + avg + "; y: " + y);
            //add LRP value to return series
            lrp.add(avg, (double) y);
        }

        return lrp;
    }
}
