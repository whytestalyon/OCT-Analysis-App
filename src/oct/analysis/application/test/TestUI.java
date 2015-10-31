/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.test;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import oct.analysis.application.dat.OCT;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.io.TiffReader;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class TestUI extends JFrame {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        //set the look and feel of the application (use system L&F if possible)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            //fail silently and allow application to use default
        }

        //bring up UI
        TestUI ui = new TestUI("ORA");
        SwingUtilities.invokeLater(() -> {
            ui.setVisible(true);
        });

        Thread.sleep(5000);
        ui.loadImage(new File("D:\\Documents\\ContractWork\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Diseased\\KS_10175_OS_L_7_0_24_529disp_fr38reg_fr28-70_AL24p2.tif"));
        Thread.sleep(5000);
        ui.loadImage(new File("D:\\Documents\\ContractWork\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Diseased\\JC_10073_OS_L_7_0_0000027_OCT_1000x600_reg_6fr_resized_810x460.tif"));
        Thread.sleep(5000);
        ui.loadImage(new File("D:\\Documents\\ContractWork\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Diseased\\KS_10238_OS_L_7_90_05_529disp_reg_fr1-25_AL21p35.tif"));
    }

    public TestUI(String title) throws HeadlessException {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //set the layout manager
//        Container pane = getContentPane();
//        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

        //set up the menu bar
        setJMenuBar(new ORAMenuBar());

        //Set up the content pane.
        add(new OCTDisplayPanel());

        this.loadImage(new File("D:\\Documents\\ContractWork\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Diseased\\KS_10238_OS_L_7_90_05_529disp_reg_fr1-25_AL21p35.tif"));

        //monitor for changes in the OCT, repaint component if change detected
        OCTAnalysisManager.getInstance().addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (OCTAnalysisManager.PROP_OCT.equals(evt.getPropertyName())) {
                //OCT has changed, repaint component to reflect new OCT
                System.out.println("Resizing component!");
                resizeUI();
            }
        });

        //ready for display of the window.
        pack();
    }

    final void loadImage(File tiffFile) {
        try {
            //read in image and keep track of the image for later use
            BufferedImage octImage = TiffReader.readTiffImage(tiffFile);
            OCT oct = new OCT(octImage, tiffFile.getName());
            if (oct == null) {
                throw new IOException("OCT information missing, couldn't load OCT for analysis.");
            }
            //add the OCT to the analysis manager, it will take care of making it available to the OCT image panel for drawing
            OCTAnalysisManager.getInstance().setOct(oct);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Image loading failed for " + tiffFile.getAbsolutePath()
                    + ", reason: " + ex.getMessage(), "Loading error!", JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void resizeUI() {
        //reset the size calculation, this must be done other wise a cached value is used and UI won't resize to new OCT
        System.out.println("Content pane minimum size: "+getContentPane().getLayout().minimumLayoutSize(getContentPane()));
//        setPreferredSize(getContentPane().getLayout().preferredLayoutSize(getContentPane()));
        //force calculation of new UI size and adjust UI display
        pack();
        System.out.println("Window Prefered size = " + getPreferredSize());
        System.out.println("Window Min size = " + getMinimumSize());
        System.out.println("Window size = " + getSize());
    }

}
