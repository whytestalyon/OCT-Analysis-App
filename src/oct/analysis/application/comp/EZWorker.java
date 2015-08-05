/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.SwingWorker;
import oct.analysis.application.OCTLine;
import oct.analysis.application.dat.Cardinality;
import oct.analysis.application.dat.EZEdgeCoord;
import oct.analysis.application.dat.LinePoint;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.analysis.application.dat.SelectionType;
import oct.util.Segmentation;
import oct.util.Util;
import oct.util.ip.SharpenOperation;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.exception.OutOfRangeException;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class EZWorker extends SwingWorker<EZEdgeCoord, Point> {

    private final OCTAnalysisManager analysisManager = OCTAnalysisManager.getInstance();
    private final SelectionLRPManager selMngr = SelectionLRPManager.getInstance();
    private final boolean debug = true;

    @Override
    protected EZEdgeCoord doInBackground() throws Exception {
        int foveaCenterXPosition = analysisManager.getFoveaCenterXPosition();
        /*
         first get a sharpened version of the OCT and use that to obtain the segmentation
         of the Bruch's membrane. Use a Loess interpolation algorithm to smooth 
         out imperfetions in the segmentation line.
         */
        UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
        double[][] brmSeg = Util.getXYArraysFromPoints(new ArrayList<>(analysisManager.getSegmentation(new SharpenOperation(15, 0.5F)).getSegment(Segmentation.BrM_SEGMENT)));
        UnivariateFunction brmInterp = interpolator.interpolate(brmSeg[0], brmSeg[1]);
        BufferedImage sharpOCT = analysisManager.getSharpenedOctImage(8.5D, 1.0F);
        setProgress(10);
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
        contour.add(findContourRight(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT));
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
            contour.add(findContourRight(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT));
        }
        setProgress(20);
        //open balck space found, complete contour to left of fovea
        contour.add(findContourLeft(startPoint, Cardinality.SOUTH, startPoint, Cardinality.SOUTH, contour, sharpOCT));
        analysisManager.getImgPanel().setDrawPoint(new Point(foveaCenterXPosition, searchY));
        setProgress(30);
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
        setProgress(35);
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
        contour.add(findContourRight(startPoint, Cardinality.NORTH, startPoint, Cardinality.NORTH, contour, sharpOCT));
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
            contour.add(findContourRight(startPoint, Cardinality.NORTH, startPoint, Cardinality.NORTH, contour, sharpOCT));
        }
        setProgress(45);
        //open balck space found, complete contour to left of fovea
        System.out.println("OCT width: " + sharpOCT.getWidth());
        contour.add(findContourLeft(startPoint, Cardinality.NORTH, startPoint, Cardinality.NORTH, contour, sharpOCT));
        setProgress(55);
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
        setProgress(70);

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
        analysisManager.getImgPanel().setDrawnLines(IntStream.rangeClosed(minX, maxX).mapToObj(x -> new LinePoint(x, interpEZContour.value(x))).collect(Collectors.toList()), IntStream.rangeClosed(minX, maxX).mapToObj(x -> new LinePoint(x, interpBruchsContour.value(x))).collect(Collectors.toList()));
        /*
         Find the difference between the two contours (Bruch's membrane and the
         EZ + Bruch's membrane) and use this to determine where the edge of the
         EZ is
         */
        List<LinePoint> diffLine = findDiffWithAdjustment(interpBruchsContour, 0D, interpEZContour, avgDif, minX, maxX);
        setProgress(90);
//        List<LinePoint> peaks = Util.findPeaksAndVallies(diffLine);
//        Util.graphPoints(diffLine, peaks);

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
        setProgress(100);
        //return findings
        return new EZEdgeCoord(ezLeftEdge.getAsInt(), ezRightEdge.getAsInt());
    }

    @Override
    protected void process(List<Point> chunks) {
        if (debug) {
            analysisManager.getImgPanel().setDrawPoint(chunks.get(chunks.size() - 1));
        }
    }

    @Override
    protected void done() {
        //place selection at the center of the fovea
        int fv = analysisManager.getFoveaCenterXPosition();
        OCTLine foveaSelection = new OCTLine(fv, 0, analysisManager.getOct().getImageHeight(), SelectionType.FOVEAL, "Fovea", false);
        selMngr.addOrUpdateSelection(foveaSelection);
        //second, automatically find the X position of each EZ edge
        try {
            EZEdgeCoord ez = get();
            selMngr.addOrUpdateSelection(new OCTLine(ez.getLeftXCoord(), 0, analysisManager.getOct().getImageHeight(), SelectionType.NONFOVEAL, "EZ Left", true));
            selMngr.addOrUpdateSelection(new OCTLine(ez.getRightXCoord(), 0, analysisManager.getOct().getImageHeight(), SelectionType.NONFOVEAL, "EZ Right", true));
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EZWorker.class.getName()).log(Level.SEVERE, "Automatic detection of EZ edges failed!", ex);
        }
        analysisManager.getImgPanel().repaint();
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
    public boolean isSurroundedByWhite(int xStart, int yStart, BufferedImage sharpOCT) {
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

    public boolean isContrastPoint(int x, int y, BufferedImage sharpOCT) {
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
    public Point findContourRight(Point searchPoint, Cardinality searchDirection, Point startPoint, Cardinality startDirection, LinkedList<Point> contourList, BufferedImage sharpOCT) throws InterruptedException {
        if (debug) {
            publish(searchPoint);
            Thread.sleep(200);
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
    public Point findContourLeft(Point searchPoint, Cardinality searchDirection, Point startPoint, Cardinality startDirection, LinkedList<Point> contourList, BufferedImage sharpOCT) throws InterruptedException {
        if (debug) {
            publish(searchPoint);
            Thread.sleep(200);
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
            contourList.add(findContourLeft(nextPoint, nextDirection, startPoint, startDirection, contourList, sharpOCT));
        }
        return nextPoint;
    }

    public List<LinePoint> findDiffWithAdjustment(UnivariateFunction fa, double faYValueAdj, UnivariateFunction fb, double fbYValueAdj, int minX, int maxX) {
        return IntStream.rangeClosed(minX, maxX)
                .mapToObj(x -> new LinePoint(x, (fa.value(x) + faYValueAdj) - (fb.value(x) + fbYValueAdj)))
                .collect(Collectors.toList());
    }

}
