/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.test;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public interface OCTOverlay {

    public String getName();

    public BufferedImage drawOverLay();

    public int getZValue();

    public boolean display();

    public void addPropertyChangeListener(PropertyChangeListener listener);

}
