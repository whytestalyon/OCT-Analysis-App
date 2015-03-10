/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util.ip;

import ij.process.FloatProcessor;
import java.util.List;

/**
 *
 * @author Brandon
 */
public interface ImageOperation {
    
    /**
     * Performs the specific image operation on the image represented by the
     * supplied float processor.
     *
     * @param fp the image to apply the given operation to
     */
    public abstract void performOperation(FloatProcessor fp);

    /**
     * Check to indicate if the filter is active and should be used or not.
     *
     * @return true if active, false otherwise
     */
    public abstract boolean isActive();
}
