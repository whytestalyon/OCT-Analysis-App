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
    
    public void drawSelection(Graphics g){
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
    
}
