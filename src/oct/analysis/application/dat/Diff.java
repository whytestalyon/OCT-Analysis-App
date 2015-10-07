/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

/**
 *
 * @author Brandon
 */
public class Diff {
    private LinePoint linePoint1;
    private LinePoint linePoint2;

    public Diff(LinePoint linePoint1, LinePoint linePoint2) {
        this.linePoint1 = linePoint1;
        this.linePoint2 = linePoint2;
    }

    public LinePoint getLinePoint1() {
        return linePoint1;
    }

    public LinePoint getLinePoint2() {
        return linePoint2;
    }

    public double getYDiff() {
        return Math.abs(linePoint1.getY() - linePoint2.getY());
    }
    
}
