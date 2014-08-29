/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import oct.io.TiffReader;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisUI extends javax.swing.JFrame {

    private boolean selectFoveaMode = false;

    /**
     * Creates new form OCTAnalysisUI
     */
    public OCTAnalysisUI() {
        initComponents();
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
        lrpJScrollPane = new javax.swing.JScrollPane();
        octImagePanel = new oct.analysis.application.OCTImagePanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        fileOpenMenuItem = new javax.swing.JMenuItem();
        Exit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        foveaSelectMenuItem = new javax.swing.JCheckBoxMenuItem();

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
            .addGap(0, 277, Short.MAX_VALUE)
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

        jMenu2.setText("Analysis");

        foveaSelectMenuItem.setText("Select Fovea");
        foveaSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                foveaSelectMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(foveaSelectMenuItem);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lrpJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 686, Short.MAX_VALUE)
            .addComponent(octImagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(octImagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lrpJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                octImagePanel.repaint();
                validate();
                pack();
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
        if (selectFoveaMode && evt.getButton() == MouseEvent.BUTTON1) {
            octImagePanel.addLrpSelection(evt.getX() - 3, 0, 5, octImagePanel.getHeight());
        }
    }//GEN-LAST:event_octImagePanelMouseClicked

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
    private javax.swing.JMenuItem fileOpenMenuItem;
    private javax.swing.JCheckBoxMenuItem foveaSelectMenuItem;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane lrpJScrollPane;
    private oct.analysis.application.OCTImagePanel octImagePanel;
    private javax.swing.JFileChooser openFileChooser;
    // End of variables declaration//GEN-END:variables
}
