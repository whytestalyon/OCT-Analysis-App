/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.io;

import chuiSegmentation.CSegImage;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import javax.swing.JOptionPane;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTImagePanel;
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
     * Given an OCT image this function segments the ILM (Inner Limiting
     * Membrane). It will return the segmentation as a collection of points with
     * coordinates relative to the image.
     *
     * @param image
     * @param distanceBetweenPoints
     * @return
     */
    public static Collection<Point> getSurfaceSegment(BufferedImage image, int distanceBetweenPoints) {
        // Only tested with BufferedImage.TYPE_BYPE_GRAY. 
        // It might work with other types but no promises. 
        CSegImage segImg = new CSegImage(image);
        segImg.getFlatImage(false);
        return segImg.getSegments()
                .getCurve(0, distanceBetweenPoints)
                .getPointCollection();
    }

    /**
     * Get the local maximums from a collection of Points.
     *
     * @param segmentLine
     * @return
     */
    public static LinkedList<Point> getPeaks(Collection<Point> segmentLine) {
        LinkedList<Point> maxPoints = new LinkedList<>();
        ArrayList<Point> pointList = new ArrayList<>(segmentLine);
        Point leftPeakPoint = new Point(0, 0);
        int leftPeakPointIndex = 0;
        Point rightPeakPoint = new Point(0, 0);
        int index = -1;
        for (Point point : pointList) {
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
                    Point prev = pointList.get(leftPeakPointIndex - 1);
                    //if two points back has a Y value that is less than or equal to the left peak point
                    //then we have found the end of the peak and we can process as such
                    if (prev.getY() <= leftPeakPoint.getY()) {
                        double peakx = rightPeakPoint.getX() - ((rightPeakPoint.getX() - leftPeakPoint.getX()) / 2D);
                        Point p = new Point(leftPeakPoint);
                        p.setLocation(peakx, leftPeakPoint.getY());
                        maxPoints.add(p);
                    }
                }
                leftPeakPoint = point;
                leftPeakPointIndex = index;
                rightPeakPoint = point;
            }
        }

        return maxPoints;
    }
    
    
}
