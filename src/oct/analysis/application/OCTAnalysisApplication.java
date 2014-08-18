/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import oct.io.TiffReader;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisApplication {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        BufferedImage bi = TiffReader.readTiffImage(new File("C:\\Users\\Brandon\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Zebra Fish OCTs\\7182_OD_L_3_0_0000076_OCT_600x1000_reg_fr41-80.tif"));
        System.out.println("Read in tiff image!");
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(bi)));
        frame.pack();
        frame.setVisible(true);
    }

}
