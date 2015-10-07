/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class EZEdgeCoord {
    private final int leftXCoord, rightXCoord;

    public EZEdgeCoord(int leftXCoord, int rightXCoord) {
        this.leftXCoord = leftXCoord;
        this.rightXCoord = rightXCoord;
    }

    public int getLeftXCoord() {
        return leftXCoord;
    }

    public int getRightXCoord() {
        return rightXCoord;
    }
    
}
