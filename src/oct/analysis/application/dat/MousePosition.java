/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.beans.*;
import java.io.Serializable;

/**
 *
 * @author Brandon
 */
public class MousePosition implements Serializable {

    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    public static final String PROP_X_POSITION = "xPosition";
    public static final String PROP_Y_POSITION = "yPosition";

    private String sampleProperty;
    private int xPosition;
    private int yPosition;

    private PropertyChangeSupport propertySupport;

    public MousePosition() {
        propertySupport = new PropertyChangeSupport(this);
    }

    public String getSampleProperty() {
        return sampleProperty;
    }

    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }

    public int getxPosition() {
        return xPosition;
    }

    public void setxPosition(int value) {
        int tmpy = this.xPosition;
        this.xPosition = value;
        propertySupport.firePropertyChange(PROP_X_POSITION, tmpy, xPosition);
    }

    public int getyPosition() {
        return yPosition;
    }

    public void setyPosition(int value) {
        int tmpy = this.yPosition;
        this.yPosition = value;
        propertySupport.firePropertyChange(PROP_Y_POSITION, tmpy, yPosition);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

}
