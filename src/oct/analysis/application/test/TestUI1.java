/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.test;

import java.awt.Container;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import oct.io.TiffReader;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class TestUI1 extends JFrame {

    int cntr = 0;

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
        TestUI1 ui = new TestUI1("ORA");
        SwingUtilities.invokeLater(() -> {
            ui.setVisible(true);
        });
    }

    public TestUI1(String title) throws HeadlessException {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.PAGE_AXIS));

        //set up the menu bar
        setJMenuBar(new ORAMenuBar());
        //load initial image
        JLabel imgLabel = new JLabel(new ImageIcon(loadImage(new File("D:\\Documents\\ContractWork\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Diseased\\KS_10238_OS_L_7_90_05_529disp_reg_fr1-25_AL21p35.tif"))));
        imgLabel.setAlignmentX(CENTER_ALIGNMENT);
        add(imgLabel);
        //add change image button
        JButton b = new JButton("new image");
        b.addActionListener((ActionEvent e) -> {
            switch (cntr % 2) {
                case 1:
                    BufferedImage img = loadImage(new File("D:\\Documents\\ContractWork\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Diseased\\KS_10238_OS_L_7_90_05_529disp_reg_fr1-25_AL21p35.tif"));
                    imgLabel.setIcon(new ImageIcon(img));
                    break;
                case 0:
                    img = loadImage(new File("D:\\Documents\\ContractWork\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\Diseased\\JC_10073_OS_L_7_0_0000027_OCT_1000x600_reg_6fr_resized_810x460.tif"));
                    imgLabel.setIcon(new ImageIcon(img));
                    break;
            }
            cntr++;
            pack();
        });
        add(b);

        //ready for display of the window.
        pack();
    }

    final BufferedImage loadImage(File tiffFile) {
        BufferedImage img = null;
        try {
            //read in image and keep track of the image for later use
            img = TiffReader.readTiffImage(tiffFile);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Image loading failed for " + tiffFile.getAbsolutePath()
                    + ", reason: " + ex.getMessage(), "Loading error!", JOptionPane.ERROR_MESSAGE
            );
        }
        return img;
    }

}
