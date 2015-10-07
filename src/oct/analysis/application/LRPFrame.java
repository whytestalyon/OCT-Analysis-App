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

/**
 *
 * @author Brandon
 */
public class LRPFrame extends JFrame implements Runnable {

    private JPanel lrpPanel;
    private Component relativeTo;

    public LRPFrame(JPanel lrpPanel) throws HeadlessException {
        this.lrpPanel = lrpPanel;
        add(lrpPanel, BorderLayout.CENTER);
    }

    public LRPFrame(JPanel lrpPanel, String title) throws HeadlessException {
        super(title);
        add(lrpPanel, BorderLayout.CENTER);
        this.lrpPanel = lrpPanel;
    }

    public void updateLRP(JPanel newLRP) {
        this.remove(lrpPanel);
        this.add(newLRP);
        this.invalidate();
        this.validate();
        lrpPanel = newLRP;
    }

    public Component getRelativeTo() {
        return relativeTo;
    }

    public void setRelativeTo(Component relativeTo) {
        this.relativeTo = relativeTo;
    }

    @Override
    public void run() {
        this.setSize(400, 400);
        this.setLocationRelativeTo(relativeTo);
        this.setVisible(true);
    }
}
