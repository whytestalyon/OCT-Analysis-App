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

    private final ConcurrentHashMap<String, ImageOperation> activeImgOpList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ImageOperation> inactiveImgOpList = new ConcurrentHashMap<>();

    private ImageOperationManager() {
        inactiveImgOpList.put(BlurOperation.class.getName(), new BlurOperation(0D));
        inactiveImgOpList.put(SharpenOperation.class.getName(), new SharpenOperation(0D, 0F));
    }

    public static ImageOperationManager getInstance() {
        return ImageOperationManagerHolder.INSTANCE;
    }

    private static class ImageOperationManagerHolder {

        private static final ImageOperationManager INSTANCE = new ImageOperationManager();
    }

    public void updateBlurOperation(double blurFactor) {
        BlurOperation op;
        if (inactiveImgOpList.contains(BlurOperation.class.getName())) {
            if (blurFactor > 0D) {
                op = ((BlurOperation) inactiveImgOpList.get(BlurOperation.class.getName()));
                op.setBlurFactor(blurFactor);
                inactiveImgOpList.remove(BlurOperation.class.getName());
                activeImgOpList.put(BlurOperation.class.getName(), op);
            }
        } else {
            op = ((BlurOperation) activeImgOpList.get(BlurOperation.class.getName()));
            op.setBlurFactor(blurFactor);
            if (blurFactor <= 0.000001D) {
                op.setBlurFactor(0D);
                activeImgOpList.remove(BlurOperation.class.getName());
                inactiveImgOpList.put(BlurOperation.class.getName(), op);
            }
        }
    }

    public void updateSharpenOperationWeight(float sharpenWeight) {
        SharpenOperation op;
        if (inactiveImgOpList.contains(SharpenOperation.class.getName())) {
            op = ((SharpenOperation) inactiveImgOpList.get(SharpenOperation.class.getName()));
            op.setSharpenWeight(sharpenWeight);
            if (sharpenWeight > 0F && op.getSharpenSigma() > 0D) {
                inactiveImgOpList.remove(SharpenOperation.class.getName());
                activeImgOpList.put(SharpenOperation.class.getName(), op);
            }
        } else {
            op = ((SharpenOperation) activeImgOpList.get(SharpenOperation.class.getName()));
            op.setSharpenWeight(sharpenWeight);
            if (sharpenWeight <= 0.00001F) {
                op.setSharpenWeight(0F);
                activeImgOpList.remove(SharpenOperation.class.getName());
                inactiveImgOpList.put(SharpenOperation.class.getName(), op);
            }
        }
    }

    public void updateSharpenOperationSigma(double sharpenSigma) {
        SharpenOperation op;
        if (inactiveImgOpList.contains(SharpenOperation.class.getName())) {
            op = ((SharpenOperation) inactiveImgOpList.get(SharpenOperation.class.getName()));
            op.setSharpenSigma(sharpenSigma);
            if (sharpenSigma > 0D && op.getSharpenWeight() > 0F) {
                inactiveImgOpList.remove(SharpenOperation.class.getName());
                activeImgOpList.put(SharpenOperation.class.getName(), op);
            }
        } else {
            op = ((SharpenOperation) activeImgOpList.get(SharpenOperation.class.getName()));
            op.setSharpenSigma(sharpenSigma);
            if (sharpenSigma <= 0.000001D) {
                op.setSharpenSigma(0D);
                activeImgOpList.remove(SharpenOperation.class.getName());
                inactiveImgOpList.put(SharpenOperation.class.getName(), op);
            }
        }
    }

    /**
     * Get the set of active operations to apply to the OCT image
     *
     * @return
     */
    public List<ImageOperation> getActiveOperationList() {
        return new ArrayList<>(activeImgOpList.values());
    }
}
