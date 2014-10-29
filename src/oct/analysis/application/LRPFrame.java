/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.HeadlessException;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Brandon
 */
public class LRPFrame extends JFrame{
    private JPanel lrpPanel;

    public LRPFrame(JPanel lrpPanel) throws HeadlessException {
        this.lrpPanel = lrpPanel;
    }

    public LRPFrame(JPanel lrpPanel, String title) throws HeadlessException {
        super(title);
        this.lrpPanel = lrpPanel;
    }
    
    public void updateLRP(JPanel newLRP){
        this.remove(lrpPanel);
        this.add(newLRP);
        lrpPanel = newLRP;
    }
}
