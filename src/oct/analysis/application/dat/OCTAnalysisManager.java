/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import oct.util.Segmentation;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisManager {

    private int micronsBetweenSelections = 0;
    private OCT oct = null;
    private volatile Segmentation segmentation = null;
    private volatile boolean useSharperImage = false;

    private OCTAnalysisManager() {
    }

    public static OCTAnalysisManager getInstance() {
        return OCTAnalysisMetricsHolder.INSTANCE;
    }

    private static class OCTAnalysisMetricsHolder {

        private static final OCTAnalysisManager INSTANCE = new OCTAnalysisManager();
    }

    public int getDistanceBetweenSelections() {
        return (oct == null) ? -1 : (int) (micronsBetweenSelections * (1D / oct.getScale()));
    }

    public int getMicronsBetweenSelections() {
        return micronsBetweenSelections;
    }

    public void setMicronsBetweenSelections(int micronsBetweenSelections) {
        this.micronsBetweenSelections = micronsBetweenSelections;
    }

    public OCT getOct() {
        return oct;
    }

    public void setOct(OCT oct) {
        this.oct = oct;
    }
    
    public void setUseSharperImage(boolean useSharperImage){
        if(useSharperImage != this.useSharperImage){
            segmentation = null;
        }
        this.useSharperImage = useSharperImage;
    }

    public Segmentation getSegmentation() {
        if (segmentation == null) {
            if (useSharperImage) {
                segmentation = new Segmentation(oct.getPartiallySharpenedOCT(), 1);
            } else {
                segmentation = new Segmentation(oct.getLogOctImage(), 1);
            }
        }
        return segmentation;
    }

}
