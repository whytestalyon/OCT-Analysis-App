/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import oct.util.Line;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class SegmentationManager extends LinkedList<Line> {

    private LinkedList<Line> selectedSegLines = new LinkedList<>();
    public static final String PROP_SELECTEDSEGLINES = "selectedSegLines";
    public static final String PROP_SELECTEDSEGLINES_SIZE = "selectedSegLinesSize";
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

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

    /**
     * Get the value of selectedSegLines
     *
     * @return the value of selectedSegLines
     */
    public LinkedList<Line> getSelectedSegLines() {
        return selectedSegLines;
    }

    /**
     * Set the value of selectedSegLines
     *
     * @param selectedSegLines new value of selectedSegLines
     */
    public void setSelectedSegLines(LinkedList<Line> selectedSegLines) {
        LinkedList<Line> oldSelectedSegLines = this.selectedSegLines;
        this.selectedSegLines = selectedSegLines;
        propertyChangeSupport.firePropertyChange(PROP_SELECTEDSEGLINES, oldSelectedSegLines, selectedSegLines);
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

    public void resetSelectedSegs() {
        selectedSegLines = new LinkedList<>();
    }

    public void addSelectedSeg(Line line) {
        selectedSegLines.add(line);
        if (selectedSegLines.size() > 1) {
            propertyChangeSupport.firePropertyChange(PROP_SELECTEDSEGLINES_SIZE, 1, 2);
        }
    }

    public void clearSegs() {
        this.removeRange(0, this.size() - 1);
    }
}
