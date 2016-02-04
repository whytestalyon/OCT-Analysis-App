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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
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
    public static final int peakPixelDiff = 7;
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

    public static LinkedList<Line> getSegmentationLines(BufferedImage image, boolean skipNorm) {
        //normalize image to make identification of ILM and BrM easier
        if (!skipNorm) {
            image = new NormalizationOperation().performOperation(image);
        }
        //smooth image to simplify segmentation
        image = new BlurOperation(1.5D).performOperation(image);
        //get LRP for each X position, find peaks, group peaks by x
        BufferedImage tmpimg = image;
        List<List<Point>> lrpPeaks = IntStream.range(0, tmpimg.getWidth())
                .parallel()
                .mapToObj(x -> {
                    ArrayList<LinePoint> intensityValues = new ArrayList<>(tmpimg.getHeight());
                    IntStream.range(0, tmpimg.getHeight()).forEach(y -> {
                        intensityValues.add(new LinePoint(y, Util.calculateGrayScaleValue(tmpimg.getRGB(x, y))));
                    });
                    return Util.getMaximums(intensityValues)
                    .stream()
                    .map(lp -> new Point(x, lp.getX()))
                    .collect(Collectors.toList());
                })
                .collect(Collectors.toList());

        //calulate all possible lines that can be generated from the peaks of the LRPs
        LinkedList<Line> activeLines = new LinkedList<>();
        LinkedList<Line> retLines = new LinkedList<>();

        //init the start of the first lines (i.e. first peaks from LRP on left side)
        ListIterator<List<Point>> lrpPeaksIter = lrpPeaks.listIterator();
        lrpPeaksIter.next()
                .forEach(p -> {
                    Line l = new Line(1024);
                    l.add(p);
                    activeLines.add(l);
                });
        lrpPeaksIter.remove();

        //grow lines
        while (lrpPeaksIter.hasNext()) {
            List<Point> curPeaks = lrpPeaksIter.next();
            //link curPeaks to closest line(s)
            List<Line2PointLink> line2PointLinks = curPeaks.stream()
                    .filter(peak -> activeLines.stream().anyMatch(line -> Line2PointLink.getDistnaceBetween(line, peak) <= peakPixelDiff))
                    .map(peak -> {
                        List<Line2PointLink> lineMatchCandidates = activeLines.stream().map(line -> new Line2PointLink(line, peak)).filter(l2p -> l2p.getDistnaceBetween() <= peakPixelDiff).collect(Collectors.toList());
                        int mindist = lineMatchCandidates.stream().mapToInt(Line2PointLink::getDistnaceBetween).min().getAsInt();
                        int maxLineLength = lineMatchCandidates.stream().filter(l2p -> l2p.getDistnaceBetween() == mindist).mapToInt(l2p -> l2p.line.size()).max().getAsInt();
                        return lineMatchCandidates.stream().filter(l2p -> l2p.getDistnaceBetween() == mindist).filter(l2p -> l2p.line.size() == maxLineLength).findFirst().get();
                    })
                    .collect(Collectors.toList());
            //retire lines that had no new peaks linked to them
            ListIterator<Line> aliter = activeLines.listIterator();
            while (aliter.hasNext()) {
                Line activeLine = aliter.next();
                if (line2PointLinks.stream().noneMatch(l2p -> l2p.line.getId() == activeLine.getId())) {
                    retLines.add(activeLine);
                    aliter.remove();
                }
            }
            //add linked points to lines
            line2PointLinks.forEach(l2p -> {
                l2p.line.add(l2p.point);
            });
            //start new lines for all peaks that weren't matched to a line
            curPeaks.stream()
                    .filter(peak -> line2PointLinks.stream().noneMatch(l2p -> l2p.point.equals(peak)))
                    .forEach(peak -> {
                        Line l = new Line(1024);
                        l.add(peak);
                        activeLines.add(l);
                    });
        }
        //add the last of the active lines to the return lines
        retLines.addAll(activeLines);
        return retLines;
    }

    private static class Line2PointLink {

        Line line;
        Point point;

        public Line2PointLink(Line line, Point point) {
            this.line = line;
            this.point = point;
        }

        public int getDistnaceBetween() {
            return Math.abs(line.getLastPoint().y - point.y);
        }

        public static int getDistnaceBetween(Line line, Point point) {
            return Math.abs(line.getLastPoint().y - point.y);
        }
    }
}
