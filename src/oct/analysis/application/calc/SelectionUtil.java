/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.calc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import oct.analysis.application.OCTSelection;

/**
 *
 * @author Brandon
 */
public class SelectionUtil {

    /**
     * Build out the OCT selections to the right and the left of the Foveal
     * Selection. This method will place a selection ever @code{dbs} pixels
     * (center to center) to the right and left of the foveal selection. These
     * selections continue until the edge of the OCT image is reached in each
     * direction.
     *
     * @param foveaSelection initial selection denoting the fovea on the OCT
     * @param windowWidth width (in pixels) of the OCT image
     * @param dbs distance (in pixels) between OCT selections
     * @return list containing all of the OCT image selections based on the
     * foveal selection and the desired distance between selections
     */
    public static List<OCTSelection> getSelectionsFromFoveaSelection(OCTSelection foveaSelection, int windowWidth, int dbs) {
        LinkedList<OCTSelection> selections = new LinkedList<>();
        //add foveal selction to list of selections
        selections.add(foveaSelection);
        //build selection list to the right of center
        for (int selX = foveaSelection.getX_position() + dbs; (selX + foveaSelection.getWidth()) <= windowWidth; selX += dbs) {
            selections.add(new OCTSelection(selX, 0, foveaSelection.getWidth(), foveaSelection.getHeight()));
        }
        //build selection list to the left of the center
        for (int selX = foveaSelection.getX_position() - dbs; selX >= 0; selX -= dbs) {
            selections.add(new OCTSelection(selX, 0, foveaSelection.getWidth(), foveaSelection.getHeight()));
        }
        return selections;
    }

}
