/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.awt.Point;
import java.util.LinkedList;
import oct.util.Line;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class SegmentationManager extends LinkedList<Line> {

    private SegmentationManager() {
    }

    public static SegmentationManager getInstance() {
        return SegmentationManagerHolder.INSTANCE;
    }

    private static class SegmentationManagerHolder {

        private static final SegmentationManager INSTANCE = new SegmentationManager();
    }

    public Line getLineIfNearPoint(Point p) {
        return stream()
                .filter(line -> {
                    return line.stream().anyMatch(lp -> Math.sqrt(Math.pow(p.x - lp.x, 2) + Math.pow(p.y - lp.y, 2)) < 3);
                })
                .findFirst()
                .orElse(null);
    }
    
}
