/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * @author Brandon
 */
public class OCTLine extends OCTSelection {

    public OCTLine(int xPositionOnOct, int yPositionOnOct, int height, int selectionType, String selectionName) {
        super(xPositionOnOct, yPositionOnOct, 1, height, selectionType, selectionName);
    }

    @Override
    public void drawSelection(Graphics g, int imageOffsetX, int imageOffsetY) {
        g.setColor(Color.green);
//        System.out.println("Drawing selection at x: " + xPositionOnOct + ", y: " + yPositionOnOct + ", w: " + width + ", h: " + (height - 1));
        g.drawLine(imageOffsetX + xPositionOnOct, imageOffsetY + yPositionOnOct, imageOffsetX + xPositionOnOct, imageOffsetY + yPositionOnOct + height - 1);
        drawn = true;
    }

}
