/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.OCTLine;
import oct.analysis.application.OCTSelection;
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
import org.apache.commons.math3.exception.OutOfRangeException;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisManager {

    /*
     property change support
     */
    public static final String PROP_FOVEA_CENTER_X_POSITION = "foveaCenterXPosition";
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private double scale;
    private int micronsBetweenSelections = 0;
    private OCT oct = null;
    private OCTMode displayMode = OCTMode.LOG; //default display mode of image is assumed to be a Log OCT image
    private int foveaCenterXPosition = -1;
    private OCTImagePanel imgPanel;
    private String progressMessage = "";
    private int progress = 0;

    private OCTAnalysisManager() {
    }

    public static OCTAnalysisManager getInstance() {
        return OCTAnalysisMetricsHolder.INSTANCE;
    }

    public void setImjPanel(OCTImagePanel imjPanel) {
        this.imgPanel = imjPanel;
    }

    public OCTImagePanel getImgPanel() {
        return imgPanel;
    }

    /**
     * Obtain the X coordinate (relative to the OCT image) of the center of the
     * fovea.
     *
     * @return the X coordinate of the fovea relative to the OCT image supplied
     */
    public int getFoveaCenterXPosition() {
        return foveaCenterXPosition;
    }

    /**
     * Define where the center of the fovea is.
     *
     * @param foveaCenterXPosition
     */
    public void setFoveaCenterXPosition(int foveaCenterXPosition) {
        int oldval = this.foveaCenterXPosition;
        this.foveaCenterXPosition = foveaCenterXPosition;
        propertyChangeSupport.firePropertyChange(PROP_FOVEA_CENTER_X_POSITION, oldval, foveaCenterXPosition);
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
        /*
         first get a sharpened version of the OCT and use that to obtain the segmentation
         of the Bruch's membrane. Use a Loess interpolation algorithm to smooth 
         out imperfetions in the segmentation line.
         */
        UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
        double[][] brmSeg = Util.getXYArraysFromPoints(new ArrayList<>(getSegmentation(new SharpenOperation(15, 0.5F)).getSegment(Segmentation.BrM_SEGMENT)));
        UnivariateFunction brmInterp = interpolator.interpolate(brmSeg[0], brmSeg[1]);
        BufferedImage sharpOCT = getSharpenedOctImage(8.5D, 1.0F);
        /*
         Starting from the identified location of the fovea search northward in 
         the image until the most northern pixels northward (in a 3x3 matrix of 
         pixels arround the the search point (X,Y) ) are black (ie. the search
         matrix is has found that the search point isn't totally surrounded by
         white pixels). Then a recursive search algorithm determines if the 
         black area signifies the seperation between bands or simply represents
         a closed (a black blob entirely surrounded by white pixels) black band.
         It will continue searching northward in the image until it can find an 
         open region of all blak pixels. Once this is found it will find the contour
         of the edge between the black and white pixels along the width of the image.
         */
        int searchY = (int) Math.round(brmInterp.value(foveaCenterXPosition)) + 1;
        do {
            searchY--;
        } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) > 0 || !isContrastPoint(foveaCenterXPosition, searchY, sharpOCT));
        LinkedList<Point> contour = new LinkedList<>();
        Point startPoint = new Point(foveaCenterXPosition, searchY);
        //find contour by searching for white pixel boundary to te right of the fovea
        contour.add(findContourRight(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT, false));
        //search until open black area found (ie. if the search algorithm arrives back at
        //the starting pixel keep moving north to next black area to search)
        while (contour.get(0).equals(startPoint)) {
            contour = new LinkedList<>();
            do {
                searchY--;
            } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) == 0);
            do {
                searchY--;
            } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) > 0 || isSurroundedByWhite(foveaCenterXPosition, searchY, sharpOCT));
            startPoint = new Point(foveaCenterXPosition, searchY);
            contour.add(findContourRight(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT, false));
        }
        //open balck space found, complete contour to left of fovea
        contour.add(findContourLeft(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT, false));
        imgPanel.setDrawPoint(new Point(foveaCenterXPosition, searchY));
        /*
         since the contour can snake around due to aberations and low image density 
         we need to create a single line (represented by points) from left to right
         to represent the countour. This is easily done by building a line of
         points consisting of the point with the largest Y value (furthest from 
         the top of the image) at each X value. This eliminates overhangs from the 
         contour line.
         */
        Map<Double, List<Point>> grouped = contour.stream().collect(Collectors.groupingBy(Point::getX));
        List<Point> refinedEZContour = grouped.values().stream().map((List<Point> points) -> {
            int maxY = points.stream().mapToInt((Point p) -> p.y).min().getAsInt();
            return new Point(points.get(0).x, maxY);
        }).sorted((Point p1, Point p2) -> Integer.compare(p1.x, p2.x)).collect(Collectors.toList());

        /*
         Starting from the identified location of the fovea search southward in 
         the image until the most southern pixels (in a 3x3 matrix of 
         pixels arround the the search point (X,Y) ) are black (ie. the search
         matrix has found that the search point isn't totally surrounded by
         white pixels). Then a recursive search algorithm determines if the 
         black area signifies the bottom of the Bruch's membrane or simply represents
         a closed (a black blob entirely surrounded by white pixels) black band.
         It will continue searching southward in the image until it can find an 
         open region of all black pixels. Once this is found it will find the contour
         of the edge between the black and white pixels, along the width of the image,
         of the bottom of the Bruch's membrane.
         */
//        sharpOCT = getSharpenedOctImage(5D, 1.0F);
        searchY = (int) Math.round(brmInterp.value(foveaCenterXPosition));
        do {
            searchY++;
        } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) > 0 || isSurroundedByWhite(foveaCenterXPosition, searchY, sharpOCT));
        contour = new LinkedList<>();
        startPoint = new Point(foveaCenterXPosition, searchY);
        //find contour by searching for white pixel boundary to te right of the fovea
        contour.add(findContourRight(startPoint, Cardinality.NORTH, startPoint, Cardinality.NORTH, contour, sharpOCT, false));
        //search until open black area found (ie. if the search algorithm arrives back at
        //the starting pixel keep moving south to next black area to search)
        while (contour.get(0).equals(startPoint)) {
            contour = new LinkedList<>();
            do {
                searchY++;
            } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) == 0);
            do {
                searchY++;
            } while (Util.calculateGrayScaleValue(sharpOCT.getRGB(foveaCenterXPosition, searchY)) > 0 || isSurroundedByWhite(foveaCenterXPosition, searchY, sharpOCT));
            startPoint = new Point(foveaCenterXPosition, searchY);
            contour.add(findContourRight(startPoint, Cardinality.NORTH, startPoint, Cardinality.NORTH, contour, sharpOCT, false));
        }
        //open balck space found, complete contour to left of fovea
        System.out.println("OCT width: " + sharpOCT.getWidth());
        contour.add(findContourLeft(startPoint, Cardinality.NORTH, startPoint, Cardinality.NORTH, contour, sharpOCT, false));
        /*
         since the contour can snake around due to aberations and low image density 
         we need to create a single line (represented by points) from left to right
         to represent the countour. This is easily done by building a line of
         points consisting of the point with the smallest Y value (closest to 
         the top of the image) at each X value. This eliminates overhangs from the 
         contour line.
         */
        grouped = contour.stream().collect(Collectors.groupingBy(Point::getX));
        List<Point> refinedBruchsMembraneContour = grouped.values().stream().map((List<Point> points) -> {
            int minY = points.stream().mapToInt((Point p) -> p.y).min().getAsInt();
            return new Point(points.get(0).x, minY);
        }).sorted((Point p1, Point p2) -> Integer.compare(p1.x, p2.x)).collect(Collectors.toList());

        /*
         use a Loess interpolator again to smooth the new contours of the EZ and Bruch's Membrane
         */
        double[][] refinedContourPoints = Util.getXYArraysFromPoints(refinedEZContour);
        UnivariateFunction interpEZContour = interpolator.interpolate(refinedContourPoints[0], refinedContourPoints[1]);
        refinedContourPoints = Util.getXYArraysFromPoints(refinedBruchsMembraneContour);
        UnivariateFunction interpBruchsContour = interpolator.interpolate(refinedContourPoints[0], refinedContourPoints[1]);

        /*
         find the average difference in the distance in the Y between the 10 pixels
         at each end of the Bruch's Membrane contour and the contour created
         along the top of the EZ.
         */
        //since the lines are sorted on X position it is easy to align the lines
        //based on the tails of each line
        int minX = refinedEZContour.get(0).x;
        int maxX;
        //the interpolator can shorten the range of the X values from the original supplied
        //so we need to test where the end of the range occurs since it isn't directly accessible
        for (maxX = refinedEZContour.get(refinedEZContour.size() - 1).x; maxX > minX; maxX--) {
            try {
                double tmp = interpEZContour.value(maxX) - interpBruchsContour.value(maxX);
                //if this break is reached we have found the max value the interpolators will allow
                break;
            } catch (OutOfRangeException oe) {
                //do nothing but let loop continue
            }
        }
        double avgDif = Stream.concat(IntStream.range(minX + 30, minX + 50).boxed(), IntStream.range(maxX - 49, maxX - 28).boxed())
                .mapToDouble(x -> interpBruchsContour.value(x) - interpEZContour.value(x))
                .average()
                .getAsDouble();

        int height = sharpOCT.getHeight();//make to use in lambda expression
        List<LinePoint> ezLine = IntStream.rangeClosed(minX, maxX).mapToObj(x -> new LinePoint(x, height - interpEZContour.value(x) - avgDif)).collect(Collectors.toList());
        List<LinePoint> bmLine = IntStream.rangeClosed(minX, maxX).mapToObj(x -> new LinePoint(x, height - interpBruchsContour.value(x))).collect(Collectors.toList());
        List<LinePoint> bmUnfiltLine = refinedBruchsMembraneContour.stream().map((Point p) -> new LinePoint(p.x, height - p.getY())).collect(Collectors.toList());
        Util.graphPoints(ezLine, bmLine, bmUnfiltLine);
        imgPanel.setDrawnLines(IntStream.rangeClosed(minX, maxX).mapToObj(x -> new LinePoint(x, interpEZContour.value(x))).collect(Collectors.toList()), IntStream.rangeClosed(minX, maxX).mapToObj(x -> new LinePoint(x, interpBruchsContour.value(x))).collect(Collectors.toList()));
        /*
         Find the difference between the two contours (Bruch's membrane and the
         EZ + Bruch's membrane) and use this to determine where the edge of the
         EZ is
         */
        List<LinePoint> diffLine = findDiffWithAdjustment(interpBruchsContour, 0D, interpEZContour, avgDif, minX, maxX);
        List<LinePoint> peaks = Util.findPeaksAndVallies(diffLine);
        Util.graphPoints(diffLine, peaks);

        /*
         Find the first zero crossings of the difference line on both sides of the fovea.
         If a zero crossing can't be found then search for the first crossing of a
         value of 1, then 2, then 3, etc. until an X coordinate of a crossing is
         found on each side of the fovea.
         */
        OptionalInt ezLeftEdge;
        double crossingThreshold = 0.25D;
        do {
            double filtThresh = crossingThreshold;
            System.out.println("Crossing threshold = " + crossingThreshold);
            ezLeftEdge = diffLine
                    .stream()
                    .filter(lp -> lp.getY() <= filtThresh && lp.getX() < foveaCenterXPosition)
                    .mapToInt(LinePoint::getX).max();
            crossingThreshold += 0.25D;
        } while (!ezLeftEdge.isPresent());
        OptionalInt ezRightEdge;
        crossingThreshold = 0.25D;
        do {
            double filtThresh = crossingThreshold;
            System.out.println("Crossing threshold = " + crossingThreshold);
            ezRightEdge = diffLine
                    .stream()
                    .filter(lp -> lp.getY() <= filtThresh && lp.getX() > foveaCenterXPosition)
                    .mapToInt(LinePoint::getX).min();
            crossingThreshold += 0.25D;
        } while (!ezRightEdge.isPresent());
        return new int[]{ezLeftEdge.getAsInt(), ezRightEdge.getAsInt()};
    }

    /**
     * This method will take care of interacting with the user in determining
     * where the fovea is within the OCT. It first lets the user inspect the
     * automatically identified locations where the fovea may be and then choose
     * the selection that is at the fovea. If none of the automated findings are
     * at the fovea the user has the option to manual specify it's location.
     * Finally, the chosen X coordinate (within the OCT) of the fovea is set in
     * the manager and can be obtained via the getFoveaCenterXPosition getter.
     *
     * @param fullAutoMode find the fovea automatically without user input when
     * true, otherwise find the fovea in semi-automatic mode involving user
     * interaction
     */
    public void findCenterOfFovea(boolean fullAutoMode) throws InterruptedException, ExecutionException {
        //disable clicking other components while processing by enabling glass pane
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(imgPanel);
        Component glassPane = topFrame.getGlassPane();
        glassPane.setVisible(true);

        //monitor progress of finding the fovea
        ProgressMonitor pm = new ProgressMonitor(imgPanel,
                "Analyzing OCT for fovea...",
                "", 0, 100);
        pm.setMillisToDecideToPopup(0);
        pm.setMillisToPopup(100);
        pm.setProgress(0);
        FoveaFindingTask fvtask = new FoveaFindingTask(!fullAutoMode, glassPane);
        fvtask.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if ("progress".equals(evt.getPropertyName())) {
                int progress1 = (Integer) evt.getNewValue();
                pm.setProgress(progress1);
            }
        });
        fvtask.execute();
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
     * This method returns the OCT image according to the currently set OCT mode
     * and image operations.
     *
     * @return
     */
    public BufferedImage getOctImage() {
        BufferedImage modeOCT = (displayMode == OCTMode.LOG) ? oct.getLogOctImage() : oct.getLinearOctImage();
        FloatProcessor fp = new ByteProcessor(modeOCT).convertToFloatProcessor();
        fp.snapshot();
        ImageOperationManager.getInstance().getActiveOperationList().forEach(imop -> {
            imop.performOperation(fp);
        });
        return fp.getBufferedImage();
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

    private boolean isContrastPoint(int x, int y, BufferedImage sharpOCT) {
        return Util.calculateGrayScaleValue(sharpOCT.getRGB(x, y)) == 0 && Util.calculateGrayScaleValue(sharpOCT.getRGB(x, y + 1)) > 0;
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
    private Point findContourRight(Point searchPoint, Cardinality searchDirection, Point startPoint, Cardinality startDirection, LinkedList<Point> contourList, BufferedImage sharpOCT, boolean trace) {
        if (trace) {
            System.out.println("CR searching: " + searchPoint.x);
            imgPanel.setDrawPoint(searchPoint);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(OCTAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
        if (!((nextPoint.equals(startPoint) && nextDirection == startDirection) || nextPoint.x <= 20 || nextPoint.x >= sharpOCT.getWidth() - 20)) {
            contourList.add(findContourRight(nextPoint, nextDirection, startPoint, startDirection, contourList, sharpOCT, trace));
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
    private Point findContourLeft(Point searchPoint, Cardinality searchDirection, Point startPoint, Cardinality startDirection, LinkedList<Point> contourList, BufferedImage sharpOCT, boolean trace) {
        if (trace) {
            System.out.println("CR searching: " + searchPoint.x);
            imgPanel.setDrawPoint(searchPoint);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(OCTAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
        if (!((nextPoint.equals(startPoint) && nextDirection == startDirection) || nextPoint.x <= 20 || nextPoint.x >= sharpOCT.getWidth() - 20)) {
            contourList.add(findContourLeft(nextPoint, nextDirection, startPoint, startDirection, contourList, sharpOCT, trace));
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
    private BufferedImage getSharpenedOctImage(double sigma, float weight) {
        FloatProcessor tmpFp = new ByteProcessor(oct.getLogOctImage()).convertToFloatProcessor();
        tmpFp.snapshot();//need to create a snapshot before any operations can be performed on image
        new SharpenOperation(sigma, weight).performOperation(tmpFp);
        return tmpFp.getBufferedImage();
    }

    public List<LinePoint> findDiffWithAdjustment(UnivariateFunction fa, double faYValueAdj, UnivariateFunction fb, double fbYValueAdj, int minX, int maxX) {
        return IntStream.rangeClosed(minX, maxX)
                .mapToObj(x -> new LinePoint(x, (fa.value(x) + faYValueAdj) - (fb.value(x) + fbYValueAdj)))
                .collect(Collectors.toList());
    }

    public List<LinePoint> findAbsoluteDiff(UnivariateFunction fa, UnivariateFunction fb, int minX, int maxX) {
        return IntStream.rangeClosed(minX, maxX)
                .mapToObj(x -> new LinePoint(x, Math.abs(fa.value(x) - fb.value(x))))
                .collect(Collectors.toList());
    }

    public List<LinePoint> findAbsoluteDiff(List<LinePoint> fa, List<LinePoint> fb) {
        ListIterator<LinePoint> faIter, fbIter;
        if (fa.get(0).getX() == fb.get(0).getX()) {
            faIter = fa.listIterator();
            fbIter = fb.listIterator();
        } else if (fa.get(0).getX() > fb.get(0).getX()) {
            faIter = fa.listIterator();
            fbIter = fb.listIterator(fa.get(0).getX() - fb.get(0).getX());
        } else {
            faIter = fa.listIterator(fb.get(0).getX() - fa.get(0).getX());
            fbIter = fb.listIterator();
        }
        LinkedList<LinePoint> retLine = new LinkedList<>();
        while (faIter.hasNext() && fbIter.hasNext()) {
            LinePoint pointA = faIter.next();
            LinePoint pointB = fbIter.next();
            retLine.add(new LinePoint(pointA.getX(), Math.abs(pointA.getY() - pointB.getY())));
        }
        return retLine;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public int getProgress() {
        return progress;
    }

    private enum Cardinality {

        NORTH,
        SOUTH,
        EAST,
        WEST;
    }

    private class FoveaFindingTask extends SwingWorker<List<Integer>, Integer> {

        private final boolean interactiveMode;
        private final Component glassPane;

        public FoveaFindingTask(boolean interactiveMode, Component glassWindow) {
            this.interactiveMode = interactiveMode;
            this.glassPane = glassWindow;
        }

        @Override
        protected List<Integer> doInBackground() throws Exception {
            return findPotentialFoveaSites();
        }

        private List<Integer> findPotentialFoveaSites() {
            //find the fovea since it hasn't been found/defined yet
            UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
            setProgress(5);
            Segmentation octSeg = getSegmentation(new SharpenOperation(15, 0.6F));
            setProgress(10);
            double[][] ilmSeg = Util.getXYArraysFromPoints(new ArrayList<>(octSeg.getSegment(Segmentation.ILM_SEGMENT)));
            UnivariateFunction ilmInterp = interpolator.interpolate(ilmSeg[0], ilmSeg[1]);
            double[][] brmSeg = Util.getXYArraysFromPoints(new ArrayList<>(octSeg.getSegment(Segmentation.BrM_SEGMENT)));
            UnivariateFunction brmInterp = interpolator.interpolate(brmSeg[0], brmSeg[1]);
            double[][] diffLine = Util.getXYArraysFromLinePoints(findAbsoluteDiff(brmInterp, ilmInterp, 0, oct.getLinearOctImage().getWidth() - 1));
            UnivariateFunction diffInerp = interpolator.interpolate(diffLine[0], diffLine[1]);
            FiniteDifferencesDifferentiator differ = new FiniteDifferencesDifferentiator(4, 0.25);
            UnivariateDifferentiableFunction difFunc = differ.differentiate(diffInerp);
            setProgress(20);
            /*
             * collect the first derivative at each pixel in the image
             */
            int numFreeVariablesInFunction = 1;
            int order = 1;
            DerivativeStructure xd;
            DerivativeStructure yd;
            ArrayList<LinePoint> firstDeriv = new ArrayList<>(oct.getLinearOctImage().getWidth() - 1);
            IntStream.range(0, oct.getLinearOctImage().getWidth() - 1).forEach((int i) -> {
                firstDeriv.add(new LinePoint(0, 0));
            });
            for (int xRealValue = 1; xRealValue <= oct.getLinearOctImage().getWidth() - 2; xRealValue++) {
                xd = new DerivativeStructure(numFreeVariablesInFunction, order, 0, xRealValue);
                yd = difFunc.value(xd);
                firstDeriv.set(xRealValue, new LinePoint(xRealValue, yd.getPartialDerivative(1)));
            }
            setProgress(40);
            List<LinePoint> peaks = Util.findMaxAndMins(firstDeriv);
            LinePoint prevPeak = null;
            LinkedList<Diff> diffs = new LinkedList<>();
            for (LinePoint curPeak : peaks) {
                if (prevPeak != null) {
                    diffs.add(new Diff(prevPeak, curPeak));
                }
                prevPeak = curPeak;
            }
            setProgress(60);
            Diff maxDiff = diffs.stream().max(Comparator.comparingDouble((Diff diff) -> diff.getYDiff())).get();
            double sign = Math.signum(maxDiff.getLinePoint1().getY());
            int signChangeXPos = maxDiff.getLinePoint1().getX() + 1;
            setProgress(80);
            while (sign == Math.signum(firstDeriv.get(signChangeXPos).getY())) {
                signChangeXPos++;
            }
            //add the most likely fovea position to list first
            int foveaCenterXPosition = (Math.abs(firstDeriv.get(signChangeXPos).getY()) < Math.abs(firstDeriv.get(signChangeXPos - 1).getY())) ? signChangeXPos : signChangeXPos - 1;
            LinkedList<Integer> positionList = new LinkedList<>();
            positionList.add(foveaCenterXPosition);
            //find other zero crossings
            for (LinePoint curPeak : peaks) {
                if (prevPeak != null && !prevPeak.equals(maxDiff.getLinePoint1())) {
                    sign = Math.signum(prevPeak.getY());
                    signChangeXPos = prevPeak.getX() + 1;
                    try {
                        while (sign == Math.signum(firstDeriv.get(signChangeXPos).getY())) {
                            signChangeXPos++;
                        }
                        //add other possible fovea site to list
                        foveaCenterXPosition = (Math.abs(firstDeriv.get(signChangeXPos).getY()) < Math.abs(firstDeriv.get(signChangeXPos - 1).getY())) ? signChangeXPos : signChangeXPos - 1;
                        positionList.add(foveaCenterXPosition);
                    } catch (IndexOutOfBoundsException ie) {
                        //caused because the first derivative line is shorter than the original
                        //fail sillently
                    }
                }
                prevPeak = curPeak;
            }
            setProgress(100);
            return positionList;
        }

        @Override
        protected void done() {
            SelectionLRPManager selMngr = SelectionLRPManager.getInstance();

            //grab findings
            List<Integer> sites = null;
            try {
                sites = get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(OCTAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
            }

            //process based of of fovea finding user interaction mode
            if (interactiveMode) {
                //since the glass pane is blocking user interation with the UI add
                //a listener that will only pass through clicks over the image panel
                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(imgPanel);
                glassPane.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        /*
                         With the glasspane up we have to listen for click events 
                         and forward them if they fit with this process. In this case
                         we will only forward clicks to the JPanel that displays the
                         OCT.
                         */
                        Point glassPanePoint = e.getPoint();
                        Container container = topFrame.getContentPane();
                        Point containerPoint = SwingUtilities.convertPoint(
                                glassPane,
                                glassPanePoint,
                                container);
                        if (containerPoint.y > 0) {
                            //The mouse event is probably over the content pane.
                            //Find out exactly which component it's over.  
                            Component component
                                    = SwingUtilities.getDeepestComponentAt(
                                            container,
                                            containerPoint.x,
                                            containerPoint.y);

                            if ((component != null)
                                    && (component.equals(imgPanel))) {
                                //process where user clicked over the JPanel that displays the OCT to the panel to process.
                                Point componentPoint = SwingUtilities.convertPoint(
                                        glassPane,
                                        glassPanePoint,
                                        component);
                                //determine if click was over one of the possible fovea selections
                                OCTSelection selection = selMngr.getOverlappingSelection(componentPoint.x, componentPoint.y, imgPanel.getImageOffsetX(), imgPanel.getImageOffsetY());
                                if (selection == null) {
                                    //user decided that none of the automated selections was correct and made their own selection
                                    //check that new selection is what they want
                                    //clear all selections from being displayed
                                    selMngr.removeSelections(true);
                                    //translate component coordinates to OCT coordinates
                                    Point p = imgPanel.translatePanelPointToOctPoint(componentPoint);
                                    selection = new OCTLine(p.x, 0, oct.getImageHeight(), SelectionType.FOVEAL, "Fovea", false);
                                    selMngr.addOrUpdateSelection(selection);
                                    imgPanel.repaint();
                                    if (JOptionPane.showConfirmDialog(imgPanel, "Is this the location of the center of the fovea? If not hit 'No' and click on the image where you believe the center of the fovea resides.", "Center of Fovea?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                                        glassPane.removeMouseListener(this);
                                        glassPane.setVisible(false);
                                        setFoveaCenterXPosition(selection.getXPositionOnOct());
                                        selMngr.removeSelections(true);
                                        imgPanel.repaint();
                                    }
                                } else {
                                    glassPane.removeMouseListener(this);
                                    glassPane.setVisible(false);
                                    setFoveaCenterXPosition(selection.getXPositionOnOct());
                                    selMngr.removeSelections(true);
                                    imgPanel.repaint();
                                }
                            }
                        }
                    }
                });

                //draw potential fovea selections to screen for user to choose from
                if (sites == null) {
                    sites = new LinkedList<>();
                }
                if (sites.isEmpty()) {
                    sites.add(oct.getImageWidth() / 2);
                }
                sites.forEach(x -> {
                    selMngr.addOrUpdateSelection(new OCTLine(x, 0, oct.getImageHeight(), SelectionType.FOVEAL, "Potential Fovea @ " + x, false));
                });
                imgPanel.repaint();

                //notify user of how they can select the selection that is the fovea or make a new selection
                JOptionPane.showMessageDialog(imgPanel,
                        "Please select (by clicking one of the gray boxes at the top of a selection)\n"
                        + " the selection that you believe is the fovea. If none of the\n"
                        + " presented seletions look like the location of the fovea click\n"
                        + " anywhere on the image to assign the location manually.",
                        "Select Fovea",
                        JOptionPane.INFORMATION_MESSAGE);

            } else {
                //auto identification process returns most likely result without user interaction
                if (sites == null || sites.isEmpty()) {
                    setFoveaCenterXPosition(oct.getImageWidth() / 2);
                } else {
                    setFoveaCenterXPosition(sites.get(0));
                }
            }
        }

    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

}
