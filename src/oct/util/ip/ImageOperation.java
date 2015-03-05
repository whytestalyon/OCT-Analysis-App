/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.util.ip;

import ij.process.FloatProcessor;

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
    public void performOperation(FloatProcessor fp);
}
