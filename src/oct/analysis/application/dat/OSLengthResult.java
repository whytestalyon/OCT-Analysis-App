/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import oct.util.Line;
import oct.util.Util;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OSLengthResult {

    private XYSeries diffPoints;
    private XYSeries amgPoints;
    private XYSeries ijgPoints;
    private XYSeries amgmPoint;
    private XYSeries ijgmPoint;
    private Line ez, iz;
    
    private OSLengthResult() {
    }

    public static OSLengthResult getInstance() {
        return OSLengthResultHolder.INSTANCE;
    }

    public XYSeries getDiffPoints() {
        return diffPoints;
    }

    public XYSeries getAmgPoints() {
        return amgPoints;
    }

    public XYSeries getIjgPoints() {
        return ijgPoints;
    }

    public XYSeries getAmgmPoint() {
        return amgmPoint;
    }

    public XYSeries getIjgmPoint() {
        return ijgmPoint;
    }

    public Line getEz() {
        return ez;
    }

    public Line getIz() {
        return iz;
    }

    private static class OSLengthResultHolder {

        private static final OSLengthResult INSTANCE = new OSLengthResult();
    }
    
    public void setSegments(Line ez, Line iz){
        this.ez = ez;
        this.iz = iz;
    }

    public void setSeries(XYSeries diffPoints, XYSeries amgPoints, XYSeries ijgPoints, XYSeries amgmPoint, XYSeries ijgmPoint) {
        this.diffPoints = diffPoints;
        this.amgPoints = amgPoints;
        this.ijgPoints = ijgPoints;
        this.amgmPoint = amgmPoint;
        this.ijgmPoint = ijgmPoint;
    }

    public void graphResult() {
        Util.graphSeries("OS Length", new XYSeries[]{diffPoints, amgPoints, ijgPoints, amgmPoint, ijgmPoint}, new boolean[]{false, false, false, true, true}, new boolean[]{true, true, true, false, false});
    }

    public double getMaxDiff() {
        return (ijgmPoint.getItemCount() > 0) ? ijgmPoint.getMaxY() : amgmPoint.getMaxY();
    }
    
    public double getMaxDiffLocation() {
        return (ijgmPoint.getItemCount() > 0) ? ijgmPoint.getMaxX() : amgmPoint.getMaxX();
    }

    public String getMaxDiffSource() {
        return (ijgmPoint.getItemCount() > 0) ? "ImageJ gaussian fit" : "Apache Math gaussian fit";
    }
}
