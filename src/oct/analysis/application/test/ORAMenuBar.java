/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class ORAMenuBar extends JMenuBar {

    //file menu options
    JMenu fileMenu = new JMenu();
    JMenuItem newAnalysis = new JMenuItem();
    JMenuItem open = new JMenuItem();
    JMenuItem save = new JMenuItem();
    JMenuItem exportResults = new JMenuItem();
    JMenuItem exit = new JMenuItem();
    //analysis menu options
    JMenu analysisMenu = new JMenu();
    JMenuItem autoEquidist = new JMenuItem();
    JMenuItem interEquidist = new JMenuItem();
    JMenuItem autoEz = new JMenuItem();
    JMenuItem interEz = new JMenuItem();
    JMenuItem single = new JMenuItem();
    JMenuItem autoMirror = new JMenuItem();
    JMenuItem interMirror = new JMenuItem();
    JMenuItem autoFovea = new JMenuItem();
    JMenuItem interFovea = new JMenuItem();
    //tools menu options
    JMenu toolsMenu = new JMenu();
    JCheckBoxMenuItem foveaSelect = new JCheckBoxMenuItem();
    JCheckBoxMenuItem singleSelect = new JCheckBoxMenuItem();
    JMenuItem showLrp = new JMenuItem();
    //tool bars menu options
    JMenu toolbarsMenu = new JMenu();
    JCheckBoxMenuItem filters = new JCheckBoxMenuItem();

    int cntr = 0;

    public ORAMenuBar() {
        initMenus();
    }

    private void initMenus() {
        /*
         Add different menus to the menu bar, order added determines order listed in bar
         */
        initFileMenu();
        initAnalysisMenu();
        initToolsMenu();
        initToolbarsMenu();
    }

    private void initFileMenu() {
        fileMenu.setText("File");

        newAnalysis.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newAnalysis.setText("New Analysis");
        newAnalysis.addActionListener((evt) -> {
            //operation hook-up here
        });
        fileMenu.add(newAnalysis);

        open.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        open.setText("Open Analysis");
        open.addActionListener((evt) -> {
            //operation hook-up here
        });
        fileMenu.add(open);

        save.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        save.setText("Save Analysis");
        save.setEnabled(false);
        save.addActionListener((evt) -> {
            //operation hook-up here
        });
        fileMenu.add(save);

        exportResults.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        exportResults.setText("Export Analysis Results");
        exportResults.setEnabled(false);
        exportResults.addActionListener((evt) -> {
            //operation hook-up here
        });
        fileMenu.add(exportResults);

        exit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        exit.setText("Quit");
        exit.addActionListener((evt) -> {
            //operation hook-up here
        });
        fileMenu.add(exit);

        this.add(fileMenu);
    }

    private void initAnalysisMenu() {

        analysisMenu.setText("Analysis");
        analysisMenu.addActionListener((evt) -> {
            //operation hook-up here
        });

        autoEquidist.setText("Equidistant (automatic)");
        autoEquidist.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(autoEquidist);

        interEquidist.setText("Equidistant (interactive)");
        interEquidist.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(interEquidist);

        autoEz.setText("EZ (automatic)");
        autoEz.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(autoEz);

        interEz.setText("EZ (interactive)");
        interEz.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(interEz);

        single.setText("Single");
        single.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(single);

        autoMirror.setText("Mirror (automatic)");
        autoMirror.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(autoMirror);

        interMirror.setText("Mirror (interactive)");
        interMirror.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(interMirror);

        autoFovea.setText("Find Fovea (automatic)");
        autoFovea.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(autoFovea);

        interFovea.setText("Find Fovea (interactive)");
        interFovea.addActionListener((evt) -> {
            //operation hook-up here
        });
        analysisMenu.add(interFovea);

        this.add(analysisMenu);
    }

    private void initToolsMenu() {
        toolsMenu.setText("Tools");
        toolsMenu.addActionListener((evt) -> {
            //operation hook-up here
        });

        foveaSelect.setText("Select Fovea");
        foveaSelect.setEnabled(false);
        foveaSelect.addActionListener((evt) -> {
            //operation hook-up here
        });
        toolsMenu.add(foveaSelect);

        singleSelect.setText("Select Single");
        singleSelect.setEnabled(false);
        singleSelect.addActionListener((evt) -> {
            //operation hook-up here
        });
        toolsMenu.add(singleSelect);

        showLrp.setText("Generate LRPs");
        showLrp.setEnabled(false);
        showLrp.addActionListener((evt) -> {
            //operation hook-up here
        });
        toolsMenu.add(showLrp);

        this.add(toolsMenu);
    }

    private void initToolbarsMenu() {
        toolbarsMenu.setText("Toolbars");

        filters.setSelected(true);
        filters.setText("Filters Toolbar");
        filters.addActionListener((evt) -> {
            //operation hook-up here
        });
        toolbarsMenu.add(filters);

        this.add(toolbarsMenu);
    }
}
