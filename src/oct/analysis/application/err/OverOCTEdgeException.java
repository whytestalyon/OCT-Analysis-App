/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.err;

/**
 * An exception indicating that an event tried to do something outside of the
 * bounds of the OCT image.
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OverOCTEdgeException extends RuntimeException {

    public OverOCTEdgeException(String message) {
        super(message);
    }

}
