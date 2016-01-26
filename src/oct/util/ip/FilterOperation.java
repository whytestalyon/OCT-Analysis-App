/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util.ip;

import java.awt.image.BufferedImage;

/**
 *
 * @author Brandon
 */
public interface FilterOperation {

    /**
     * Performs the specific image operation on the image represented by the
     * supplied float processor.
     *
     * @param bi the image to apply the given operation to
     * @return the modified image
     */
    public BufferedImage performOperation(BufferedImage bi);

    /**
     * Check to indicate if the filter is active and should be used or not.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive();
}
