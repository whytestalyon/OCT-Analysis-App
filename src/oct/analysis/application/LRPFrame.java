/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import oct.analysis.application.lrp.LRPPanel;

/**
 *
 * @author Brandon
 */
public class LRPFrame extends JFrame implements Runnable {

    private LRPPanel lrpPanel;
    private Component relativeTo;

    public LRPFrame(LRPPanel lrpPanel) throws HeadlessException {
        this.lrpPanel = lrpPanel;
        add(lrpPanel, BorderLayout.CENTER);
    }

    public LRPFrame(LRPPanel lrpPanel, String title) throws HeadlessException {
        super(title);
        add(lrpPanel, BorderLayout.CENTER);
        this.lrpPanel = lrpPanel;
    }

    public LRPPanel getLrpPanel() {
        return lrpPanel;
    }
    
    public Component getRelativeTo() {
        return relativeTo;
    }

    public void setRelativeTo(Component relativeTo) {
        this.relativeTo = relativeTo;
    }

    @Override
    public void run() {
        this.setSize(800, 800);
        this.setLocationRelativeTo(relativeTo);
        this.setVisible(true);
    }
}
