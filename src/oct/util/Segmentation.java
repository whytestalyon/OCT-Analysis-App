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
import java.util.Collection;

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
     * Retrieves the segmentation of a specific feature as a collection of points with
     * coordinates relative to the image.
     *
     * @param segment the integer representation for the desired feature to get
     * the segmentation for
     * @return
     */
    public Collection<Point> getSegment(int segment) {
        return segments.getCurve(segment, distanceBetweenPoints).getPointCollection();
    }

}
