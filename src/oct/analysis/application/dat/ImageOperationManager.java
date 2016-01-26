/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.util.ArrayList;
import java.util.List;
import oct.util.ip.BlurOperation;
import oct.util.ip.FilterOperation;
import oct.util.ip.FloatProcessorOperation;
import oct.util.ip.MedianFilterOperation;
import oct.util.ip.NormalizationOperation;
import oct.util.ip.SharpenOperation;

/**
 *
 * @author Brandon
 */
public class ImageOperationManager {

    private BlurOperation blur;
    private SharpenOperation sharp;
    private final MedianFilterOperation median;
    private final NormalizationOperation norm;

    private ImageOperationManager() {
        blur = new BlurOperation(0D);
        sharp = new SharpenOperation(0D, 0F);
        median = new MedianFilterOperation();
        norm = new NormalizationOperation();
    }

    public static ImageOperationManager getInstance() {
        return ImageOperationManagerHolder.INSTANCE;
    }

    private static class ImageOperationManagerHolder {

        private static final ImageOperationManager INSTANCE = new ImageOperationManager();
    }

    public void updateBlurOperation(BlurOperation op) {
        blur = op;
    }

    public void updateSharpenOperation(SharpenOperation op) {
        sharp = op;
    }

    public void activateMedianFilter() {
        median.activate();
    }

    public void deactivateMedianFilter() {
        median.deactivate();
    }

    public void activateNormalization() {
        norm.setActive(true);
    }

    public void deactivateNormalization() {
        norm.setActive(false);
    }

    /**
     * Get the set of active operations to apply to the OCT image
     *
     * @return
     */
    public List<FilterOperation> getActiveCustomOperationList() {
        ArrayList<FilterOperation> ret = new ArrayList<>(2);
        if (blur.isActive()) {
            ret.add(blur);
        }
        if (sharp.isActive()) {
            ret.add(sharp);
        }
        if (median.isActive()) {
            ret.add(median);
        }
        if (norm.isActive()) {
            ret.add(norm);
        }
        return ret;
    }

    public BlurOperation getBlur() {
        return blur;
    }

    public SharpenOperation getSharp() {
        return sharp;
    }

    public MedianFilterOperation getMedian() {
        return median;
    }

}
