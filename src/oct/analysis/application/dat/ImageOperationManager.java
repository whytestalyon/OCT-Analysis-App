/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import oct.util.ip.BlurOperation;
import oct.util.ip.ImageOperation;
import oct.util.ip.SharpenOperation;

/**
 *
 * @author Brandon
 */
public class ImageOperationManager {
    private BlurOperation blur;
    private SharpenOperation sharp;

    private ImageOperationManager() {
        blur =  new BlurOperation(0D);
        sharp = new SharpenOperation(0D, 0F);
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

    /**
     * Get the set of active operations to apply to the OCT image
     *
     * @return
     */
    public List<ImageOperation> getActiveOperationList() {
        ArrayList<ImageOperation> ret = new ArrayList<>(2);
        if(blur.isActive()){
            ret.add(blur);
        }
        if(sharp.isActive()){
            ret.add(sharp);
        }
        return ret;
    }
}
