/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.io;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import javax.swing.JOptionPane;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.dat.OCT;

/**
 *
 * @author Brandon
 */
public class Util {

    public static double parseNumberFromInput(String in) {
        if (in.matches("[0-9]+(\\.[0-9]+)*")) {
            return Double.parseDouble(in);
        } else {
            return -1;
        }
    }

    public static OCT getOCT(BufferedImage octImage, OCTAnalysisUI octAnalysisUI, OCTImagePanel octAnalysisPanel) {
        Object[] options = {"I have the scale!", "I have axial length and scan width!"};
        int n = JOptionPane.showOptionDialog(octAnalysisUI, "We need to know the scale of the OCT. What information do you have?", "Determine OCT Scale...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        switch (n) {
            case JOptionPane.YES_OPTION:
                double scale = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT scale (microns per pixel):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                return new OCT(scale, octImage);
            case JOptionPane.NO_OPTION:
                double nominalScanWidth = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT nominal scan length(millimeter):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                double axialLength = Util.parseNumberFromInput((String) JOptionPane.showInputDialog(octAnalysisUI, "Enter OCT scale (millimeter):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                return new OCT(axialLength, nominalScanWidth, octAnalysisPanel.getWidth(), octImage);
            default:
                break;
        }
        return null;
    }

    public static BufferedImage deepCopyBufferedImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
}
