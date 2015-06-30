/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Color;
import java.awt.Graphics;
import oct.analysis.application.dat.SelectionType;

/**
 *
 * @author Brandon
 */
public class OCTLine extends OCTSelection {

    public OCTLine(int xPositionOnOct, int yPositionOnOct, int height, SelectionType selectionType, String selectionName, boolean moveable) {
        super(xPositionOnOct, yPositionOnOct, 1, height, selectionType, selectionName, moveable);
    }

    @Override
    public void drawSelection(Graphics g, int imageOffsetX, int imageOffsetY) {
        if (highlighted) {
            g.setColor(Color.pink);
        } else {
            g.setColor(Color.green);
        }
//        System.out.println("Drawing selection at x: " + xPositionOnOct + ", y: " + yPositionOnOct + ", w: " + width + ", h: " + (height - 1));
        g.drawLine(imageOffsetX + xPositionOnOct, imageOffsetY + yPositionOnOct, imageOffsetX + xPositionOnOct, imageOffsetY + yPositionOnOct + height - 1);
        //draw button for interacting with the selection
        drawSelectButton(g, imageOffsetX, imageOffsetY);
        drawn = true;
    }

}
