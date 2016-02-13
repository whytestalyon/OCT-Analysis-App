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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import oct.analysis.application.dat.LinePoint;
import oct.analysis.application.dat.OCT;
import oct.util.ip.BlurOperation;
import oct.util.ip.NormalizationOperation;
import oct.util.ip.SharpenOperation;

/**
 *
 * @author Brandon
 */
public class Segmentation {

    public static final int ILM_SEGMENT = 0;
    public static final int BrM_SEGMENT = 3;
    public static final int peakPixelDiff = 5;
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

    public static List<Line> getSegmentationLines(BufferedImage image, boolean skipNorm, double... blurFactors) {
        //calculate segmentation lines by blurring the image first
        LinkedList<Line> retblurredLines = new LinkedList<>(Arrays.stream(blurFactors)
                .mapToObj(bf -> {
                    BufferedImage modImg = image;
                    //normalize image to make identification of ILM and BrM easier
                    if (!skipNorm) {
                        modImg = new NormalizationOperation().performOperation(image);
                    }
                    //smooth image to simplify segmentation
                    modImg = new BlurOperation(bf).performOperation(modImg);
                    //get LRP for each X position, find peaks, group peaks by x
                    BufferedImage tmpimg = modImg;
                    List<List<Point>> lrpPeaks = IntStream.range(2, tmpimg.getWidth() - 2)
                    .parallel()
                    .mapToObj(x -> {
                        ArrayList<LinePoint> intensityValues = new ArrayList<>(tmpimg.getHeight());
                        IntStream.range(0, tmpimg.getHeight()).forEach(y -> {
                            double intesity = IntStream.rangeClosed(x - 2, x + 2).map(xval -> Util.calculateGrayScaleValue(tmpimg.getRGB(xval, y))).average().getAsDouble();
                            intensityValues.add(new LinePoint(y, intesity));
                        });
                        return Util.getMaximums(intensityValues).stream()
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

//                        int curX = curPeaks.get(0).x;
                        //retire lines that had no new peaks linked to them
                        ListIterator<Line> aliter = activeLines.listIterator();
                        while (aliter.hasNext()) {
                            Line activeLine = aliter.next();
                            if (line2PointLinks.stream().noneMatch(l2p -> l2p.line.getId() == activeLine.getId())) {
//                            && curX - activeLine.getLastPoint().x > 4) {
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
                })
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        /*
         //get segmentation lines by shappening the image next
         BufferedImage tmpimg = new SharpenOperation(8D, 0.6F).performOperation(image);
         //get LRP for each X position, find peaks, group peaks by x
         List<List<Point>> lrpPeaks = IntStream.range(2, tmpimg.getWidth() - 2)
         .parallel()
         .mapToObj(x -> {
         ArrayList<LinePoint> intensityValues = new ArrayList<>(tmpimg.getHeight());
         IntStream.range(0, tmpimg.getHeight()).forEach(y -> {
         double intesity = IntStream.rangeClosed(x - 2, x + 2).map(xval -> Util.calculateGrayScaleValue(tmpimg.getRGB(xval, y))).average().getAsDouble();
         intensityValues.add(new LinePoint(y, intesity));
         });
         return Util.getMaximums(intensityValues).stream()
         .map(lp -> new Point(x, lp.getX()))
         .collect(Collectors.toList());
         })
         .collect(Collectors.toList());

         //calulate all possible lines that can be generated from the peaks of the LRPs
         LinkedList<Line> activeLines = new LinkedList<>();

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

         //                        int curX = curPeaks.get(0).x;
         //retire lines that had no new peaks linked to them
         ListIterator<Line> aliter = activeLines.listIterator();
         while (aliter.hasNext()) {
         Line activeLine = aliter.next();
         if (line2PointLinks.stream().noneMatch(l2p -> l2p.line.getId() == activeLine.getId())) {
         //                            && curX - activeLine.getLastPoint().x > 4) {
         retblurredLines.add(activeLine);
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
         retblurredLines.addAll(activeLines);
         */
        return retblurredLines;
    }

    public static List<Line> getBestSegmentationLines(OCT oct) {
        LinkedList<Line> segLines = new LinkedList<>(Segmentation.getSegmentationLines(oct.getLogOctImage(), true, 0.5D, 1.8D, 5D));
        segLines.addAll(Segmentation.getSegmentationLines(oct.getLinearOctImage(), true, 0.1D));
        Collections.sort(segLines, (Line l1, Line l2) -> {
            return Integer.compare(l2.size(), l1.size());
        });

        LinkedList<List<Line>> overlapingLines = new LinkedList<>();
        for (Line curLine : segLines.subList(0, 50)) {
            boolean foundGroup = false;
            //see if line can bee added to already existing group
            OUT:
            for (List<Line> llist : overlapingLines) {
                for (Line line : llist) {
                    long overlappingPointsCount = Stream.concat(line.stream(), curLine.stream())
                            .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                            .values()
                            .stream()
                            .filter(cnt -> cnt > 1)
                            .count();
                    if (overlappingPointsCount > 20) {
                        llist.add(curLine);
                        foundGroup = true;
                        break OUT;
                    }
                }
            }

            //if not part of already existing group start group with line
            if (!foundGroup) {
                LinkedList<Line> nl = new LinkedList<>();
                nl.add(curLine);
                overlapingLines.add(nl);
            }
        }

        //merge lines contained within each group into single lines
        List<Line> mergedSegs = overlapingLines.stream()
                .map(llist -> Util.mergeLines(llist.toArray(new Line[llist.size()])))
                .collect(Collectors.toList());

        //group lines that are close to each other (50+ points that are 3 or less px apart) and merge them using weighted average
        overlapingLines = new LinkedList<>();
        for (Line curLine : mergedSegs) {
            boolean foundGroup = false;
            //see if line can bee added to already existing group
            OUT:
            for (List<Line> llist : overlapingLines) {
                for (Line line : llist) {
                    long overlappingPointsCount = Stream.concat(line.stream(), curLine.stream())
                            .collect(Collectors.groupingBy(p -> p.x))
                            .values()
                            .stream()
                            .filter(plist -> plist.size() > 1)
                            .filter(plist -> Math.abs(plist.get(0).y - plist.get(1).y) <= 3)
                            .count();
                    if (overlappingPointsCount > 50) {
                        llist.add(curLine);
                        foundGroup = true;
                        break OUT;
                    }
                }
            }

            //if not part of already existing group start group with line
            if (!foundGroup) {
                LinkedList<Line> nl = new LinkedList<>();
                nl.add(curLine);
                overlapingLines.add(nl);
            }
        }

        //merge lines contained within each group into single line
        List<Line> singleLines = overlapingLines.stream()
                .map(llist -> Util.mergeLines(llist.toArray(new Line[llist.size()])))
                .collect(Collectors.toList());

        //find lines that are slightly seperated that could be joined
        LinkedList<Line> retLines = new LinkedList<>();
        ListIterator<Line> sgIter = singleLines.listIterator();
        while (sgIter.hasNext()) {
            Line line = sgIter.next();
            if (!singleLines.stream().anyMatch(l -> l.linesCouldConnect(line))) {
                retLines.add(line);
                sgIter.remove();
            }
        }

        //merge lines
        LinkedList<Line> doMergeLines = new LinkedList<>(singleLines);
        while (!doMergeLines.isEmpty()) {
            Line evalLine = doMergeLines.removeFirst();
            if (doMergeLines.stream().noneMatch(l -> l.linesCouldConnect(evalLine))) {
                retLines.add(evalLine);
            } else {
                Line toMergeLine = doMergeLines.stream().filter(l -> l.linesCouldConnect(evalLine)).findFirst().get();
                doMergeLines.remove(toMergeLine);
                doMergeLines.add(Util.mergeDisconnetedLines(evalLine, toMergeLine));
            }
        }

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
