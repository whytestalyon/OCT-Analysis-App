/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.beans.*;
import java.io.Serializable;

/**
 * A simple bean used for tracking the current width of LRP selections displayed
 * on the screen.
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class LRPSelectionWidthBean implements Serializable {

    public static final String LRP_SELECTION_WIDTH_PROPERTY = "lrpSelectionWidth";

    private int lrpSelectionWidth;

    private final PropertyChangeSupport propertySupport;

    public LRPSelectionWidthBean() {
        propertySupport = new PropertyChangeSupport(this);
    }

    public Object getLrpSelectionWidth() {
        return lrpSelectionWidth;
    }

    public int getLrpSelectionWidthAsInt() {
        return lrpSelectionWidth;
    }

    public void setLrpSelectionWidth(Object value) {
        //check input for data type
        int lrpw = 5;
        if(value instanceof Long){
            lrpw = (int)((long)value);
        }else if(value instanceof Integer){
            lrpw = (int) value;
        }
        
        if (lrpw < 1) {
            lrpw = 1;
        } else if (lrpw > 15) {
            lrpw = 15;
        }
        int oldValue = lrpSelectionWidth;
        lrpSelectionWidth = lrpw;
        propertySupport.firePropertyChange(LRP_SELECTION_WIDTH_PROPERTY, oldValue, lrpSelectionWidth);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

}
