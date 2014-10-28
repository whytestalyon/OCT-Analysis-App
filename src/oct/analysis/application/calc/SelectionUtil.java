/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.calc;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCTAnalysisDAO;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author Brandon
 */
public class SelectionUtil {

    private static final OCTAnalysisDAO analysisMetrics = OCTAnalysisDAO.getInstance();

    /**
     * Given a X coordinate of the fovea, provide a list of selections
     * (including one centered on the fovea) based on the user supplied scale
     * and desired width of each LRP selection.
     *
     * @param foveaXPosition the X position corresponding to the center of the
     * fovea
     * @param octOffsetX the number of pixels between the left edge of the OCT
     * image and the panel displaying the image
     * @param octOffsetY the number of pixels between the top edge of the OCT
     * image and the panel displaying the image
     * @return list containing all of the OCT image selections based on the
     * foveal selection and the desired distance between selections
     */
    public static List<OCTSelection> getSelections(int foveaXPosition, int octOffsetX, int octOffsetY) {
        OCTSelection fovealSel = new OCTSelection(foveaXPosition - (analysisMetrics.getSelectionWidth() / 2), octOffsetY, analysisMetrics.getSelectionWidth(), analysisMetrics.getOCT().getOctImage().getHeight(), OCTSelection.FOVEAL_SELECTION, "FV");
        return getSelectionsFromFoveaSelection(fovealSel, octOffsetX, octOffsetY);
    }

    /**
     * Build out the OCT selections to the right and the left of the Foveal
     * Selection. This method will place a selection ever
     * @code{analysisMetrics.getDistanceBetweenSelections()} pixels (center to
     * center) to the right and left of the foveal selection. These selections
     * continue until the edge of the OCT image is reached in each direction.
     *
     * @param foveaSelection initial selection denoting the fovea on the OCT
     * @param octOffsetX the distance between the OCT image and the left side of
     * the containing panel
     * @param octOffsetY the distance between the OCT image and the top of the
     * containing panel
     * @return list containing all of the OCT image selections based on the
     * foveal selection and the desired distance between selections
     */
    public static List<OCTSelection> getSelectionsFromFoveaSelection(OCTSelection foveaSelection, int octOffsetX, int octOffsetY) {
        LinkedList<OCTSelection> selections = new LinkedList<>();
        //add foveal selction to list of selections
        selections.add(foveaSelection);
        //build selection list to the right of center
        for (int selX = foveaSelection.getX_position() + analysisMetrics.getDistanceBetweenSelections(), selCnt = 1; (selX + foveaSelection.getWidth()) <= analysisMetrics.getOCT().getOctImage().getWidth() + octOffsetX; selX += analysisMetrics.getDistanceBetweenSelections(), selCnt++) {
            selections.add(new OCTSelection(selX, octOffsetY, foveaSelection.getWidth(), foveaSelection.getHeight(), OCTSelection.PERIPHERAL_SELECTION, "R" + selCnt));
        }
        //build selection list to the left of the center
        for (int selX = foveaSelection.getX_position() - analysisMetrics.getDistanceBetweenSelections(), selCnt = 1; selX >= octOffsetX; selX -= analysisMetrics.getDistanceBetweenSelections(), selCnt++) {
            selections.add(new OCTSelection(selX, octOffsetY, foveaSelection.getWidth(), foveaSelection.getHeight(), OCTSelection.PERIPHERAL_SELECTION, "L" + selCnt));
        }

        return selections;
    }

    /**
     * Based on the supplied list of @code{OCTSelection}s create a collection of
     * series of cartesian coordinates that represent the LRPs for the different
     * selections.
     *
     * @param selections the list of selections on the OCT image that need LRPs
     * generated
     * @param oct the OCT image under analysis
     * @return an @code{List} of @code{XYSeriesCollection}s containing one LRP
     * per @code{XYSeriesCollection} for output display in a graph
     */
    public static List<XYSeriesCollection> getLRPsFromSelections(List<OCTSelection> selections, BufferedImage oct) {
        ArrayList<XYSeriesCollection> lrpSeries = new ArrayList<>(selections.size());
        ListIterator<OCTSelection> lrpSelIter = selections.listIterator();
        while (lrpSelIter.hasNext()) {
            XYSeriesCollection lrps = new XYSeriesCollection();
            OCTSelection selection = lrpSelIter.next();
            XYSeries s = selection.getLrpSeriesFromOCT(oct);
            if (s == null) {
                continue;
            }
            s.setKey(selection.getSelectionName());
            lrps.addSeries(s);
            lrpSeries.add(lrps);
        }
        return lrpSeries;
    }
}
