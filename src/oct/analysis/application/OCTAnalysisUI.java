/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.filechooser.FileNameExtensionFilter;
import oct.analysis.application.calc.LRPUtil;
import oct.analysis.application.calc.SelectionUtil;
import oct.analysis.application.dat.OCTAnalysisMetrics;
import oct.analysis.application.dat.OCTMetrics;
import oct.io.TiffReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisUI extends javax.swing.JFrame {

    private boolean selectFoveaMode = false;
    private double micronsBetweenSelections;
    private int selectionWidth;
    private OCTMetrics scale;
    private OCTAnalysisMetrics analysisMetrics = OCTAnalysisMetrics.getInstance();

    static {
        // set a chart theme using the new shadow generator feature available in
        // 1.0.14 - for backwards compatibility it is not enabled by default
        ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow",
                true));
    }

    /**
     * Creates new form OCTAnalysisUI
     */
    public OCTAnalysisUI() {
        initComponents();
        //get current selection width setting
        selectionWidth = widthSlider.getValue();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        openFileChooser = new javax.swing.JFileChooser();
        octImagePanel = new oct.analysis.application.OCTImagePanel();
        jPanel1 = new javax.swing.JPanel();
        selectionWidthSliderPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        widthSlider = new javax.swing.JSlider();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        fileOpenMenuItem = new javax.swing.JMenuItem();
        Exit = new javax.swing.JMenuItem();
        analysisMenu = new javax.swing.JMenu();
        foveaSelectMenuItem = new javax.swing.JCheckBoxMenuItem();
        pixelDistRatioMenuItem = new javax.swing.JMenuItem();
        lrpMenuItem = new javax.swing.JMenuItem();

        openFileChooser.setDialogTitle("Select OCT...");
        openFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileChooserActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        octImagePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                octImagePanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout octImagePanelLayout = new javax.swing.GroupLayout(octImagePanel);
        octImagePanel.setLayout(octImagePanelLayout);
        octImagePanelLayout.setHorizontalGroup(
            octImagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        octImagePanelLayout.setVerticalGroup(
            octImagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 358, Short.MAX_VALUE)
        );

        jPanel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 2, true));

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Selection Width");

        widthSlider.setMajorTickSpacing(2);
        widthSlider.setMaximum(15);
        widthSlider.setMinimum(1);
        widthSlider.setMinorTickSpacing(1);
        widthSlider.setPaintLabels(true);
        widthSlider.setPaintTicks(true);
        widthSlider.setValue(5);
        widthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                widthSliderStateChanged(evt);
            }
        });

        javax.swing.GroupLayout selectionWidthSliderPanelLayout = new javax.swing.GroupLayout(selectionWidthSliderPanel);
        selectionWidthSliderPanel.setLayout(selectionWidthSliderPanelLayout);
        selectionWidthSliderPanelLayout.setHorizontalGroup(
            selectionWidthSliderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(selectionWidthSliderPanelLayout.createSequentialGroup()
                .addComponent(widthSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 858, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        selectionWidthSliderPanelLayout.setVerticalGroup(
            selectionWidthSliderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionWidthSliderPanelLayout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(widthSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectionWidthSliderPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(selectionWidthSliderPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jMenu1.setText("File");

        fileOpenMenuItem.setText("Open");
        fileOpenMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileOpenMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(fileOpenMenuItem);

        Exit.setText("Exit");
        Exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitActionPerformed(evt);
            }
        });
        jMenu1.add(Exit);

        jMenuBar1.add(jMenu1);

        analysisMenu.setText("Analysis");
        analysisMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analysisMenuActionPerformed(evt);
            }
        });

        foveaSelectMenuItem.setText("Select Fovea");
        foveaSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                foveaSelectMenuItemActionPerformed(evt);
            }
        });
        analysisMenu.add(foveaSelectMenuItem);

        pixelDistRatioMenuItem.setText("Set Pixel/Distance Ratio");
        pixelDistRatioMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pixelDistRatioMenuItemActionPerformed(evt);
            }
        });
        analysisMenu.add(pixelDistRatioMenuItem);

        lrpMenuItem.setText("Generate LRPs");
        lrpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lrpMenuItemActionPerformed(evt);
            }
        });
        analysisMenu.add(lrpMenuItem);

        jMenuBar1.add(analysisMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(octImagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(octImagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fileOpenMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileOpenMenuItemActionPerformed
        openFileChooser.setFileFilter(new FileNameExtensionFilter("TIFF files", "tiff", "tif"));
        openFileChooser.setMultiSelectionEnabled(false);
        int returnVal = openFileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File tiffFile = openFileChooser.getSelectedFile();
            try {
                //read in image and keep track of the image for later use
                BufferedImage tiffBI = TiffReader.readTiffImage(tiffFile);
                System.out.println("Read in tiff image!");
                //display the selected image in the display
                octImagePanel.setOct(tiffBI);
                octImagePanel.setSize(new Dimension(tiffBI.getWidth(), tiffBI.getHeight()));
//                octImagePanel.repaint();
                validate();
                pack();
                //get the scale for the image
                scale = promptForOCTMetrics();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Image loading failed for " + tiffFile.getAbsolutePath()
                        + ", reason: " + ex.getMessage(), "Loading error!", JOptionPane.ERROR_MESSAGE
                );
            }
        } else {
            System.out.println("File access cancelled by user.");
        }
    }//GEN-LAST:event_fileOpenMenuItemActionPerformed

    private void ExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_ExitActionPerformed

    private void openFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileChooserActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_openFileChooserActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
    }//GEN-LAST:event_formWindowClosed

    private void foveaSelectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_foveaSelectMenuItemActionPerformed
        //toggle if we are in fovea selection mode
        selectFoveaMode = !selectFoveaMode;
    }//GEN-LAST:event_foveaSelectMenuItemActionPerformed

    private void octImagePanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_octImagePanelMouseClicked
        if (selectFoveaMode) {
            switch (evt.getButton()) {
                case MouseEvent.BUTTON1:
                    OCTSelection fovealSel = new OCTSelection(evt.getX() - (selectionWidth / 2), 0, selectionWidth, octImagePanel.getHeight(), OCTSelection.FOVEAL_SELECTION, "FV");
                    analysisMetrics.setFoveaSelection(fovealSel);
                    System.out.println("Got foveal selection!");
                    int pixelsBetweenSelections = (int) (micronsBetweenSelections * (1D / scale.getScale()));
                    analysisMetrics.setDistanceBetweenSelections(pixelsBetweenSelections);
                    octImagePanel.addOCTSelectionsToPanel();
                    break;
                case MouseEvent.BUTTON3:
                    System.out.println("Right mouse button clicked!");
                    octImagePanel.removeOCTSelection();
                    break;
                default:
                    break;
            }
        }
    }//GEN-LAST:event_octImagePanelMouseClicked

    private void widthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_widthSliderStateChanged
        //set the selection width
        selectionWidth = ((JSlider) evt.getSource()).getValue();
        //redraw the selections based on the new selection width
        OCTSelection oldFoveaSelection = analysisMetrics.getFoveaSelection();
        int foveaSelectionCenter = oldFoveaSelection.getX_position() + (oldFoveaSelection.getWidth()/2);
        OCTSelection fovealSel = new OCTSelection(foveaSelectionCenter - (selectionWidth / 2), oldFoveaSelection.getY_position(), selectionWidth, octImagePanel.getHeight(), OCTSelection.FOVEAL_SELECTION, "FV");
        analysisMetrics.setFoveaSelection(fovealSel);
        System.out.println("Updated foveal selection width!");
        octImagePanel.updateOCTSelections();
        //recalculate the LRPs if already displayed
    }//GEN-LAST:event_widthSliderStateChanged

    private void pixelDistRatioMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pixelDistRatioMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_pixelDistRatioMenuItemActionPerformed

    private void analysisMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analysisMenuActionPerformed

    }//GEN-LAST:event_analysisMenuActionPerformed

    private void lrpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lrpMenuItemActionPerformed
        System.out.println("Generating LRPs...");
        //get the lrps for each selection
        List<XYSeriesCollection> lrps = SelectionUtil.getLRPsFromSelections(octImagePanel.getSelectionList(), octImagePanel.getOct());
        //diplay the LRPs
        LRPUtil.displayLRPs(lrps, this);
    }//GEN-LAST:event_lrpMenuItemActionPerformed

    private OCTMetrics promptForOCTMetrics() {
        //ask for the desired distance between selections
        micronsBetweenSelections = oct.io.Util.parseNumberFromInput((String) JOptionPane.showInputDialog(this, "Enter the desired distance between selections(microns):", "Distance between selections", JOptionPane.QUESTION_MESSAGE));
        //ask how the user would like to convey the scale (microns per pixel)
        //for the image
        Object[] options = {"I have the scale!",
            "I have axial length and scan width!"};
        int n = JOptionPane.showOptionDialog(this,
                "We need to know the scale of the OCT. What information do you have?",
                "Determine OCT Scale...",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, //do not use a custom Icon
                options, //the titles of buttons
                options[0]); //default button title
        switch (n) {
            case JOptionPane.YES_OPTION:
                double scale = oct.io.Util.parseNumberFromInput((String) JOptionPane.showInputDialog(this, "Enter OCT scale (microns per pixel):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                return new OCTMetrics(scale);
            case JOptionPane.NO_OPTION:
                double nominalScanWidth = oct.io.Util.parseNumberFromInput((String) JOptionPane.showInputDialog(this, "Enter OCT nominal scan length(millimeter):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                double axialLength = oct.io.Util.parseNumberFromInput((String) JOptionPane.showInputDialog(this, "Enter OCT scale (millimeter):", "Scale input", JOptionPane.QUESTION_MESSAGE));
                return new OCTMetrics(axialLength, nominalScanWidth, octImagePanel.getWidth());
            default:
                break;
        }
        return null;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OCTAnalysisUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OCTAnalysisUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OCTAnalysisUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OCTAnalysisUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OCTAnalysisUI().setVisible(true);
            }
        });

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem Exit;
    private javax.swing.JMenu analysisMenu;
    private javax.swing.JMenuItem fileOpenMenuItem;
    private javax.swing.JCheckBoxMenuItem foveaSelectMenuItem;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JMenuItem lrpMenuItem;
    private oct.analysis.application.OCTImagePanel octImagePanel;
    private javax.swing.JFileChooser openFileChooser;
    private javax.swing.JMenuItem pixelDistRatioMenuItem;
    private javax.swing.JPanel selectionWidthSliderPanel;
    private javax.swing.JSlider widthSlider;
    // End of variables declaration//GEN-END:variables
}
