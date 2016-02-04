/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class Line extends ArrayList<Point> {

    private static final AtomicInteger idcntr = new AtomicInteger(0);
    final int id;

    public Line(int initialCapacity) {
        super(initialCapacity);
        id = idcntr.getAndIncrement();
    }

    public Point getLastPoint() {
        return get(size() - 1);
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean add(Point e) {
        if (size() == 0) {
            return super.add(e);
        }
        Point lp = getLastPoint();
        if (lp.x == e.x) {
            lp.y = (lp.y + e.y) / 2;
            return true;
        } else {
            return super.add(e); //To change body of generated methods, choose Tools | Templates.
        }
    }

}
