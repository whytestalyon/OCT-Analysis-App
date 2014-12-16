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

    public OCTLine(int panel_x_position, int panel_y_position, int height, int selectionType, String selectionName) {
        super(panel_x_position, panel_y_position, 1, height, selectionType, selectionName);
    }

    @Override
    public void drawSelection(Graphics g) {
        g.setColor(Color.green);
//        System.out.println("Drawing selection at x: " + panel_x_position + ", y: " + panel_y_position + ", w: " + width + ", h: " + (height - 1));
        g.drawLine(panel_x_position, panel_y_position, panel_x_position, panel_y_position + height - 1);
        drawn = true;
    }
    
}
