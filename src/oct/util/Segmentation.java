/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util;

import chuiSegmentation.CSegImage;
import chuiSegmentation.SegmentGroup;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import oct.analysis.application.dat.LinePoint;
import oct.util.ip.BlurOperation;
import oct.util.ip.NormalizationOperation;

/**
 *
 * @author Brandon
 */
public class Segmentation {

    public static final int ILM_SEGMENT = 0;
    public static final int BrM_SEGMENT = 3;
    private final SegmentGroup segments;
    private final int distanceBetweenPoints;

    /**
     * Given an OCT image this class provides support for segmenting different
     * features of retinal structure from the image.
     *
     * @param image the OCT image to segment
     * @param distanceBetweenPoints the distance between points in the
     * segmentation line
     */
    public Segmentation(BufferedImage image, int distanceBetweenPoints) {
        this.distanceBetweenPoints = distanceBetweenPoints;
        CSegImage segImg = new CSegImage(image);
        segImg.getFlatImage(false);
        segments = segImg.getSegments();
    }

    /**
     * Retrieves the segmentation of a specific feature as a collection of
     * points with coordinates relative to the image.
     *
     * @param segment the integer representation for the desired feature to get
     * the segmentation for
     * @return
     */
    public Collection<Point> getSegment(int segment) {
        return segments.getCurve(segment, distanceBetweenPoints).getPointCollection();
    }

    public LinkedList<ArrayList<LinePoint>> getSegmentationLines(BufferedImage image) {
        //normalize image to make identification of ILM and BrM easier
        image = new NormalizationOperation().performOperation(image);
        //smooth image to simplify segmentation
        image = new BlurOperation(1.8D).performOperation(image);
        //get image as gray scale matrix
        ArrayList<ArrayList<LinePoint>> lrpPeaks = new ArrayList<>(image.getWidth());
        for (int i = 0; i < image.getWidth(); i++) {
            lrpPeaks.add(null);
        }
        BufferedImage tmpimg = image;
        IntStream.range(0, tmpimg.getWidth())
                .parallel()
                .forEach(x -> {
                    ArrayList<LinePoint> intensityValues = new ArrayList<>(tmpimg.getHeight());
                    IntStream.range(0, tmpimg.getHeight()).forEach(y -> {
                        intensityValues.add(new LinePoint(y, Util.calculateGrayScaleValue(tmpimg.getRGB(x, y))));
                    });
                    lrpPeaks.set(x, new ArrayList<>(Util.getMaximums(intensityValues)));
                });

        //calulate all possible lines that can be generated from the peaks of the LRPs
        LinkedList<ArrayList<LinePoint>> activeLines = new LinkedList<>();
        LinkedList<ArrayList<LinePoint>> retLines = new LinkedList<>();
        int firstXwithPeaks = lrpPeaks.indexOf(lrpPeaks.stream().filter(l -> !l.isEmpty()).findFirst().get());
        //init first lines
        lrpPeaks.get(firstXwithPeaks).forEach(peakPoint -> {
            ArrayList<LinePoint> l = new ArrayList<>(1024);
            l.add(peakPoint);
            activeLines.add(l);
        });
        //check for addition to current lines, add new if detected, retire lines if finished
        for (int x = firstXwithPeaks + 1; x < image.getWidth(); x++) {
            for (ListIterator<LinePoint> peakIter = lrpPeaks.get(x).listIterator(); peakIter.hasNext();) {
                LinePoint curPeak = peakIter.next();
                //find line with previous point existing 3 or less pixels away
                if (activeLines.stream().anyMatch(line -> Math.abs(line.get(line.size() - 1).getY() - (double) curPeak.getX()) <= 3D)) {
                    List<ArrayList<LinePoint>> potMatchLines = activeLines.stream().filter(line -> Math.abs(line.get(line.size() - 1).getY() - (double) curPeak.getX()) <= 3D).collect(Collectors.toList());
                    if(potMatchLines.size() == 1){
                        potMatchLines.get(0).add(curPeak);
                    }else{
                    }
                    peakIter.remove();
                }
            }

        }

        return null;
    }

}
