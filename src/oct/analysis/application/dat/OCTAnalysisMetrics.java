/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import oct.analysis.application.OCTSelection;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisMetrics {
    
    private int distanceBetweenSelections = 0;
    private OCTSelection foveaSelection = null;
    
    private OCTAnalysisMetrics() {
    }
    
    public static OCTAnalysisMetrics getInstance() {
        return OCTAnalysisMetricsHolder.INSTANCE;
    }
    
    private static class OCTAnalysisMetricsHolder {

        private static final OCTAnalysisMetrics INSTANCE = new OCTAnalysisMetrics();
    }

    public int getDistanceBetweenSelections() {
        return distanceBetweenSelections;
    }

    public void setDistanceBetweenSelections(int distanceBetweenSelections) {
        this.distanceBetweenSelections = distanceBetweenSelections;
    }

    public OCTSelection getFoveaSelection() {
        return foveaSelection;
    }

    public void setFoveaSelection(OCTSelection foveaSelection) {
        this.foveaSelection = foveaSelection;
    }
}
