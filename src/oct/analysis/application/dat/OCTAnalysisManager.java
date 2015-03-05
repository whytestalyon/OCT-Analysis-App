/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import oct.util.Segmentation;
import oct.util.Util;
import oct.util.ip.ImageOperation;
import oct.util.ip.SharpenOperation;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisManager {

    private double scale;
    private int micronsBetweenSelections = 0;
    private OCT oct = null;
    private OCTMode displayMode = OCTMode.LOG; //default display mode of image is assumed to be a Log OCT image
    private int foveaCenterXPosition = -1;

    private OCTAnalysisManager() {
    }

    public static OCTAnalysisManager getInstance() {
        return OCTAnalysisMetricsHolder.INSTANCE;
    }

    /**
     * Obtain the X coordinate (relative to the OCT image) of the center of the
     * fovea.
     *
     * @return the X coordinate of the fovea relative to the OCT image supplied
     */
    public int getFoveaXPosition() {
        if (foveaCenterXPosition < 0) {
            //find the fovea since it hasn't been found/defined yet
            UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
            Segmentation octSeg = getSegmentation(new SharpenOperation(15, 0.5F));
            double[][] ilmSeg = Util.getXYArraysFromPoints(new ArrayList<>(octSeg.getSegment(Segmentation.ILM_SEGMENT)));
            UnivariateFunction ilmInterp = interpolator.interpolate(ilmSeg[0], ilmSeg[1]);
            double[][] brmSeg = Util.getXYArraysFromPoints(new ArrayList<>(octSeg.getSegment(Segmentation.BrM_SEGMENT)));
            UnivariateFunction brmInterp = interpolator.interpolate(brmSeg[0], brmSeg[1]);
            double[][] diffLine = Util.getXYArraysFromLinePoints(findAbsoluteDiff(brmInterp, ilmInterp, 0, getOct().getLinearOctImage().getWidth() - 1));
            UnivariateFunction diffInerp = interpolator.interpolate(diffLine[0], diffLine[1]);
            FiniteDifferencesDifferentiator differ = new FiniteDifferencesDifferentiator(4, 0.25);
            UnivariateDifferentiableFunction difFunc = differ.differentiate(diffInerp);
            /*
             * collect the first derivative at each pixel in the image
             */
            int numFreeVariablesInFunction = 1;
            int order = 1;
            DerivativeStructure xd;
            DerivativeStructure yd;
            ArrayList<LinePoint> firstDeriv = new ArrayList<>(getOct().getLinearOctImage().getWidth() - 1);
            IntStream.range(0, getOct().getLinearOctImage().getWidth() - 1).forEach((int i) -> {
                firstDeriv.add(new LinePoint(0, 0));
            });
            for (int xRealValue = 1; xRealValue <= getOct().getLinearOctImage().getWidth() - 2; xRealValue++) {
                xd = new DerivativeStructure(numFreeVariablesInFunction, order, 0, xRealValue);
                yd = difFunc.value(xd);
                firstDeriv.set(xRealValue, new LinePoint(xRealValue, yd.getPartialDerivative(1)));
            }
            List<LinePoint> peaks = Util.findMaxAndMins(firstDeriv);
            LinePoint prevPeak = null;
            LinkedList<Diff> diffs = new LinkedList<>();
            for (LinePoint curPeak : peaks) {
                if (prevPeak != null) {
                    diffs.add(new Diff(prevPeak, curPeak));
                }
                prevPeak = curPeak;
            }
            Diff maxDiff = diffs.stream().max(Comparator.comparingDouble((Diff diff) -> diff.getYDiff())).get();
            double sign = Math.signum(maxDiff.getLinePoint1().getY());
            int signChangeXPos = maxDiff.getLinePoint1().getX() + 1;
            for (; sign == Math.signum(firstDeriv.get(signChangeXPos).getY()); signChangeXPos++) {
                ;
            }
            foveaCenterXPosition = (Math.abs(firstDeriv.get(signChangeXPos).getY()) < Math.abs(firstDeriv.get(signChangeXPos - 1).getY())) ? signChangeXPos : signChangeXPos - 1;
            System.out.println("Fovea found at: " + foveaCenterXPosition);
        }
        return foveaCenterXPosition;
    }

    /**
     * Define where the center of the fovea is.
     *
     * @param foveaCenterXPosition
     */
    public void setFoveaCenterXPosition(int foveaCenterXPosition) {
        this.foveaCenterXPosition = foveaCenterXPosition;
    }

    /**
     * Get the X coordinates of the edges of the EZ on both sides of the fovea.
     *
     * @return an array containing two elements in the following order: x
     * coordinate of the left edge of the EZ, x coordinate of the right edge of
     * the EZ.
     */
    public int[] getEZEdgeCoords() {
        if (foveaCenterXPosition < 0) {
            return null;
        }
        System.out.println("Searching for EZ...");
        UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
        double[][] brmSeg = Util.getXYArraysFromPoints(new ArrayList<>(getSegmentation(new SharpenOperation(15, 0.5F)).getSegment(Segmentation.BrM_SEGMENT)));
        UnivariateFunction brmInterp = interpolator.interpolate(brmSeg[0], brmSeg[1]);
        BufferedImage sharpOCT = getSharpenedOctImage(1.0F);
        int searchY = (int) Math.round(brmInterp.value(foveaCenterXPosition)) + 1;
        do {
            searchY--;
        } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) > 0 || isSurroundedByWhite(foveaCenterXPosition, searchY, sharpOCT));
        LinkedList<Point> contour = new LinkedList<>();
        Point startPoint = new Point(foveaCenterXPosition, searchY);
        contour.add(findContourRight(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT));
        while (contour.get(0).equals(startPoint)) {
            contour = new LinkedList<>();
            do {
                searchY--;
            } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) == 0);
            do {
                searchY--;
            } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) > 0 || isSurroundedByWhite(foveaCenterXPosition, searchY, sharpOCT));
            startPoint = new Point(foveaCenterXPosition, searchY);
            contour.add(findContourRight(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT));
        }
        contour.add(findContourLeft(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT));
        Map<Double, List<Point>> grouped = contour.stream().collect(Collectors.groupingBy(Point::getX));
        List<Point> refinedContour = grouped.values().stream().map((List<Point> points) -> {
            int maxY = points.stream().mapToInt((Point p) -> p.y).max().getAsInt();
            return new Point(points.get(0).x, maxY);
        }).sorted((Point p1, Point p2) -> Integer.compare(p1.x, p2.x)).collect(Collectors.toList());
        int minX = (int) refinedContour.stream().mapToDouble(Point::getX).min().getAsDouble();
        int maxX = (int) refinedContour.stream().mapToDouble(Point::getX).max().getAsDouble();
        double avgDif = refinedContour.stream().filter((Point p) -> p.x < minX + 5 || p.x > maxX - 5).mapToDouble((Point p) -> Math.abs(p.getY() - brmInterp.value(p.x))).average().getAsDouble();
        List<LinePoint> adjRefContour = refinedContour.parallelStream().map((Point p) -> new LinePoint(p.x, p.y + avgDif)).collect(Collectors.toList());
        double[][] refinedContourPoints = Util.getXYArraysFromLinePoints(adjRefContour);
        UnivariateFunction interpRefContour = interpolator.interpolate(refinedContourPoints[0], refinedContourPoints[1]);
        List<LinePoint> clp = refinedContour.stream().map((Point p) -> new LinePoint(p.x, sharpOCT.getHeight() - interpRefContour.value(p.x))).collect(Collectors.toList());
        List<LinePoint> slp = refinedContour.stream().map((Point p) -> new LinePoint(p.x, sharpOCT.getHeight() - brmInterp.value(p.x))).collect(Collectors.toList());
        Util.graphPoints(clp, slp);
        List<LinePoint> diffLine = findAbsoluteDiff(brmInterp, interpRefContour, minX, maxX);
        Util.graphPoints(diffLine);
        return null;
    }

    private static class OCTAnalysisMetricsHolder {

        private static final OCTAnalysisManager INSTANCE = new OCTAnalysisManager();
    }

    public int getDistanceBetweenSelections() {
        return (oct == null) ? -1 : (int) (micronsBetweenSelections * (1D / scale));
    }

    public int getMicronsBetweenSelections() {
        return micronsBetweenSelections;
    }

    public void setMicronsBetweenSelections(int micronsBetweenSelections) {
        this.micronsBetweenSelections = micronsBetweenSelections;
    }

    public OCT getOct() {
        return oct;
    }

    public void setOct(OCT oct) {
        this.oct = oct;
    }

    public void setScale(double axialLength, double nominalScanWidth, int octWidth) {
        double scanLength = (nominalScanWidth * axialLength) / 24D;
        setScale(((scanLength * 1000D) / (double) octWidth));
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     * Changes the mode with which the OCT should be rendered. Calling this
     * method will cause the panel to redraw the OCT and any analysis artifacts
     * using the new mode setting.
     *
     * @param mode th mode to change the display of the OCT image
     */
    public void setOCTMode(OCTMode mode) {
        this.displayMode = mode;
    }

    public double getScale() {
        return scale;
    }

    public OCTMode getDisplayMode() {
        return displayMode;
    }

    /**
     * This method returns the OCT image according to the currently set OCT
     * mode.
     *
     * @return
     */
    public BufferedImage getOctImage() {
        return (displayMode == OCTMode.LOG) ? oct.getLogOctImage() : oct.getLinearOctImage();
    }

    /**
     * Segment the four major layers of the retina from the Log OCT image. An
     * optional {@code ImageOperation} can be applied to the image before the
     * segmentation is performed, to help improve segmentation performance.
     *
     * @param optionalOp an ImageOperation to apply before segmenting an image,
     * or null (indicating tat no operations should be performed before
     * segmenting the OCT)
     * @return segmentation of the OCT
     */
    public Segmentation getSegmentation(ImageOperation optionalOp) {
        //segmentation and image operations can only be done on 8-bit gray scale images, using the OCT we ensure 
        //the image is in useable format which handles this upon creation
        BufferedImage segImg;
        if (optionalOp != null) {
            //apply supplied operation before segmenting OCT
            FloatProcessor tmpFp = new ByteProcessor(oct.getLogOctImage()).convertToFloatProcessor();
            tmpFp.snapshot();//need to create a snapshot before any operations can be performed on image
            optionalOp.performOperation(tmpFp);
            segImg = tmpFp.getBufferedImage();
        } else {
            segImg = oct.getLogOctImage();
        }

        return new Segmentation(segImg, 1);
    }

    /**
     * Determine if the supplied coordinate (excluding itself) in the supplied
     * black and white image is surrounded by white pixels on all sides.
     *
     * @param xStart
     * @param yStart
     * @param sharpOCT
     * @return
     */
    private boolean isSurroundedByWhite(int xStart, int yStart, BufferedImage sharpOCT) {
        boolean allWhite = true;
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                if (x != 0 && y != 0) {
                    allWhite &= Util.calculateGrayScaleValue(sharpOCT.getRGB(xStart + x, yStart + y)) > 0;
                }
            }
        }
        return allWhite;
    }

    /**
     * Recursively search for a contour to the right of the supplied starting
     * point. If the contour returned contains the starting point then the
     * contour traced back to the start point rather than towards the edge of
     * the image.
     *
     * @param searchPoint point to search from
     * @param startPoint start search point
     * @param contourList list to add the contour points to
     * @param sharpOCT OCT to find the contour in
     * @return the next point in the contour after the search point
     */
    private Point findContourRight(Point searchPoint, Cardinality searchDirection, Point startPoint, Cardinality startDirection, LinkedList<Point> contourList, BufferedImage sharpOCT) {
        Point nextPoint;
        Cardinality nextDirection;
        switch (searchDirection) {
            case SOUTH:
                if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x + 1, searchPoint.y)) > 0) {
                    nextPoint = new Point(searchPoint);
                    nextDirection = Cardinality.EAST;
                } else if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x + 1, searchPoint.y + 1)) == 0) {
                    nextPoint = new Point(searchPoint.x + 1, searchPoint.y + 1);
                    nextDirection = Cardinality.WEST;
                } else {
                    nextPoint = new Point(searchPoint.x + 1, searchPoint.y);
                    nextDirection = Cardinality.SOUTH;
                }
                break;
            case EAST:
                if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x, searchPoint.y - 1)) > 0) {
                    nextPoint = new Point(searchPoint);
                    nextDirection = Cardinality.NORTH;
                } else if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x + 1, searchPoint.y - 1)) == 0) {
                    nextPoint = new Point(searchPoint.x + 1, searchPoint.y - 1);
                    nextDirection = Cardinality.SOUTH;
                } else {
                    nextPoint = new Point(searchPoint.x, searchPoint.y - 1);
                    nextDirection = Cardinality.EAST;
                }
                break;
            case WEST:
                if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x, searchPoint.y + 1)) > 0) {
                    nextPoint = new Point(searchPoint);
                    nextDirection = Cardinality.SOUTH;
                } else if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x - 1, searchPoint.y + 1)) == 0) {
                    nextPoint = new Point(searchPoint.x - 1, searchPoint.y + 1);
                    nextDirection = Cardinality.NORTH;
                } else {
                    nextPoint = new Point(searchPoint.x, searchPoint.y + 1);
                    nextDirection = Cardinality.WEST;
                }
                break;
            case NORTH:
                if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x - 1, searchPoint.y)) > 0) {
                    nextPoint = new Point(searchPoint);
                    nextDirection = Cardinality.WEST;
                } else if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x - 1, searchPoint.y - 1)) == 0) {
                    nextPoint = new Point(searchPoint.x - 1, searchPoint.y - 1);
                    nextDirection = Cardinality.EAST;
                } else {
                    nextPoint = new Point(searchPoint.x - 1, searchPoint.y);
                    nextDirection = Cardinality.NORTH;
                }
                break;
            default:
                //will never happen, just placed in to make code compile
                nextPoint = new Point(searchPoint);
                nextDirection = Cardinality.EAST;
                break;
        }
        if (!((nextPoint.equals(startPoint) && nextDirection == startDirection) || nextPoint.x <= 40 || nextPoint.x >= sharpOCT.getWidth() - 40)) {
            contourList.add(findContourRight(nextPoint, nextDirection, startPoint, startDirection, contourList, sharpOCT));
        }
        return nextPoint;
    }

    /**
     * Recursively search for a contour to the left of the supplied starting
     * point. If the contour returned contains the starting point then the
     * contour traced back to the start point rather than towards the edge of
     * the image.
     *
     * @param searchPoint point to search from
     * @param startPoint start search point
     * @param contourList list to add the contour points to
     * @param sharpOCT OCT to find the contour in
     * @return the next point in the contour after the search point
     */
    private Point findContourLeft(Point searchPoint, Cardinality searchDirection, Point startPoint, Cardinality startDirection, LinkedList<Point> contourList, BufferedImage sharpOCT) {
        System.out.println("CL searching: " + searchPoint.x);
        Point nextPoint;
        Cardinality nextDirection;
        switch (searchDirection) {
            case SOUTH:
                if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x - 1, searchPoint.y)) > 0) {
                    nextPoint = new Point(searchPoint);
                    nextDirection = Cardinality.WEST;
                } else if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x - 1, searchPoint.y + 1)) == 0) {
                    nextPoint = new Point(searchPoint.x - 1, searchPoint.y + 1);
                    nextDirection = Cardinality.EAST;
                } else {
                    nextPoint = new Point(searchPoint.x - 1, searchPoint.y);
                    nextDirection = Cardinality.SOUTH;
                }
                break;
            case EAST:
                if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x, searchPoint.y + 1)) > 0) {
                    nextPoint = new Point(searchPoint);
                    nextDirection = Cardinality.SOUTH;
                } else if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x + 1, searchPoint.y + 1)) == 0) {
                    nextPoint = new Point(searchPoint.x + 1, searchPoint.y + 1);
                    nextDirection = Cardinality.NORTH;
                } else {
                    nextPoint = new Point(searchPoint.x, searchPoint.y + 1);
                    nextDirection = Cardinality.EAST;
                }
                break;
            case WEST:
                if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x, searchPoint.y - 1)) > 0) {
                    nextPoint = new Point(searchPoint);
                    nextDirection = Cardinality.NORTH;
                } else if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x - 1, searchPoint.y - 1)) == 0) {
                    nextPoint = new Point(searchPoint.x - 1, searchPoint.y - 1);
                    nextDirection = Cardinality.SOUTH;
                } else {
                    nextPoint = new Point(searchPoint.x, searchPoint.y - 1);
                    nextDirection = Cardinality.WEST;
                }
                break;
            case NORTH:
                if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x + 1, searchPoint.y)) > 0) {
                    nextPoint = new Point(searchPoint);
                    nextDirection = Cardinality.EAST;
                } else if (Util.calculateGrayScaleValue(sharpOCT.getRGB(searchPoint.x + 1, searchPoint.y - 1)) == 0) {
                    nextPoint = new Point(searchPoint.x + 1, searchPoint.y - 1);
                    nextDirection = Cardinality.WEST;
                } else {
                    nextPoint = new Point(searchPoint.x + 1, searchPoint.y);
                    nextDirection = Cardinality.NORTH;
                }
                break;
            default:
                //will never happen, just placed in to make code compile
                nextPoint = new Point(searchPoint);
                nextDirection = Cardinality.EAST;
                break;
        }
        if (!((nextPoint.equals(startPoint) && nextDirection == startDirection) || nextPoint.x <= 40 || nextPoint.x >= sharpOCT.getWidth() - 40)) {
            contourList.add(findContourLeft(nextPoint, nextDirection, startPoint, startDirection, contourList, sharpOCT));
        }
        return nextPoint;
    }

    /**
     * This method grabs the current OCT and sharpens it using a radius (sigma)
     * of 15 and a weight factor of the supplied value. The sharpened image is
     * then returned.
     *
     * @return sharpened image
     */
    private BufferedImage getSharpenedOctImage(float weight) {
        FloatProcessor tmpFp = new ByteProcessor(oct.getLogOctImage()).convertToFloatProcessor();
        tmpFp.snapshot();//need to create a snapshot before any operations can be performed on image
        new SharpenOperation(15, weight).performOperation(tmpFp);
        return tmpFp.getBufferedImage();
    }

    public List<LinePoint> findAbsoluteDiff(UnivariateFunction fa, UnivariateFunction fb, int minX, int maxX) {
        return IntStream.rangeClosed(minX, maxX)
                .mapToObj(x -> new LinePoint(x, Math.abs(fa.value(x) - fb.value(x))))
                .collect(Collectors.toList());
    }

    private static class Diff {

        private LinePoint linePoint1, linePoint2;

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

    private enum Cardinality {

        NORTH,
        SOUTH,
        EAST,
        WEST;
    }
}
