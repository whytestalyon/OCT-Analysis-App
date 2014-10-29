/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.calc.SelectionUtil;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisDAO {

    private int micronsBetweenSelections = 0;
    private OCT oct = null;
    private List<OCTSelection> octSelections = null;
    private int selectionWidth = 5;
    private OCTImagePanel octip = null;
    private int foveaCenterXPosition = 0;
    private static final String PROP_OCTSELECTIONS = "octSelections";
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private OCTAnalysisDAO() {
    }

    public static OCTAnalysisDAO getInstance() {
        return OCTAnalysisMetricsHolder.INSTANCE;
    }

    private static class OCTAnalysisMetricsHolder {

        private static final OCTAnalysisDAO INSTANCE = new OCTAnalysisDAO();
    }

    public int getDistanceBetweenSelections() {
        return (int) (micronsBetweenSelections * (1D / oct.getScale()));
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

    public void setOctSelections(int foveaXPosition, int octOffsetX, int octOffsetY) {
        foveaCenterXPosition = foveaXPosition;
        List<OCTSelection> oldSelections = this.octSelections;
        this.octSelections = SelectionUtil.getSelections(foveaXPosition, octOffsetX, octOffsetY);
        propertyChangeSupport.firePropertyChange(PROP_OCTSELECTIONS, oldSelections, octSelections);
    }

    public List<OCTSelection> getOctSelections() {
        return octSelections;
    }

    /**
     * Get the value of selectionWidth
     *
     * @return the value of selectionWidth
     */
    public int getSelectionWidth() {
        return selectionWidth;
    }

    /**
     * Set the value of selectionWidth
     *
     * @param selectionWidth new value of selectionWidth
     */
    public void setSelectionWidth(int selectionWidth) {
        this.selectionWidth = selectionWidth;
        setOctSelections(foveaCenterXPosition, octip.getImageOffsetX(), octip.getImageOffsetY());
    }

    public int getFoveaCenterXPosition() {
        return foveaCenterXPosition;
    }

    public OCTImagePanel getOctip() {
        return octip;
    }

    public void setOctip(OCTImagePanel octip) {
        this.octip = octip;
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
