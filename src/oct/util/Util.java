/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.dat.LinePoint;
import oct.analysis.application.dat.OCT;

/**
 *
 * @author Brandon
 */
public class Util {

    public static double parseNumberFromInput(String in) {
        if (in.matches("[0-9]+(\\.[0-9]+)*")) {
            return Double.parseDouble(in);
        } else {
            return -1;
        }
    }

    public static OCT getOCT(BufferedImage octImage, OCTAnalysisUI octAnalysisUI, OCTImagePanel octAnalysisPanel) {
        Object[] options = {"I have the scale!", "I have axial length and scan width!"};
        int n = JOptionPane.showOptionDialog(octAnalysisUI, "We need to know the scale of the OCT. What information do you have?", "Determine OCT Scale...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        switch (n) {
            case JOptionPane.YES_OPTION:
                double scale = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT scale (microns per pixel):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                return new OCT(scale, octImage);
            case JOptionPane.NO_OPTION:
                double nominalScanWidth = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT nominal scan length(millimeter):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                double axialLength = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT scale (millimeter):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                return new OCT(axialLength, nominalScanWidth, octAnalysisPanel.getWidth(), octImage);
            default:
                break;
        }
        return null;
    }

    public static BufferedImage deepCopyBufferedImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * Get the local maximums from a collection of Points.
     *
     * @param line assumes that the line starts at X = 0
     * @return
     */
    public static LinkedList<LinePoint> getMaximums(List<LinePoint> line) {
        LinkedList<LinePoint> maxPoints = new LinkedList<>();
        ArrayList<LinePoint> pointList = new ArrayList<>(line);
        LinePoint leftPeakPoint = new LinePoint(0, 0);
        int leftPeakPointIndex = 0;
        LinePoint rightPeakPoint = new LinePoint(0, 0);
        int index = -1;
        for (LinePoint point : pointList) {
            index++;
            if (index == 0) {
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                continue;
            }
            if (leftPeakPoint.getY() < point.getY()) {
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                rightPeakPoint = point;
            } else if (leftPeakPoint.getY() == point.getY()) {
                rightPeakPoint = point;
            } else {
                //determine if we are coming down off of a peak by looking two points behind the current point
                if (leftPeakPointIndex > 0) {
                    LinePoint prev = pointList.get(leftPeakPointIndex - 1);
                    //if two points back has a Y value that is less than or equal to the left peak point
                    //then we have found the end of the peak and we can process as such
                    if (prev.getY() <= leftPeakPoint.getY()) {
                        double peakx = (double) rightPeakPoint.getX() - ((double) (rightPeakPoint.getX() - leftPeakPoint.getX()) / 2D);
                        maxPoints.add(new LinePoint((int) Math.round(peakx), leftPeakPoint.getY()));
                    }
                }
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                rightPeakPoint = point;
            }
        }

        return maxPoints;
    }

    public static List<LinePoint> findMaxAndMins(List<LinePoint> line) {
        //create list of all positive Y values to get peaks
        ArrayList<LinePoint> convList = new ArrayList<>(line.size());
        line.forEach(p -> {
            convList.add(new LinePoint(p.getX(), Math.abs(p.getY())));
        });
        //find X values of peaks
        List<LinePoint> peaks = getMaximums(convList);
        //collect peak points
        List<LinePoint> ret = line.parallelStream()
                .filter(p -> peaks.stream().anyMatch(pk -> pk.getX() == p.getX()))
                .collect(Collectors.toList());
        //sort be X position
        ret.sort(Comparator.comparingInt(peak -> peak.getX()));
        return ret;
    }
}
