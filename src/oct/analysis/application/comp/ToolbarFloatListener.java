/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 *
 * @author Brandon
 */
public class ToolbarFloatListener implements AncestorListener {

    private final JToolBar listenBar;
    private final JFrame window;

    public ToolbarFloatListener(JToolBar listenBar, JFrame window) {
        this.listenBar = listenBar;
        this.window = window;
    }

    @Override
    public void ancestorAdded(AncestorEvent event) {
        SwingUtilities.invokeLater(() -> {
            window.pack();
        });
//        if (SwingUtilities.getWindowAncestor(listenBar).equals(window)) {
//            System.out.println("In Main Frame");
//        } else {
//            System.out.println("Floating");
//        }
    }

    @Override
    public void ancestorRemoved(AncestorEvent event) {
        SwingUtilities.invokeLater(() -> {
            window.pack();
        });
//        if (SwingUtilities.getWindowAncestor(listenBar).equals(window)) {
//            System.out.println("...In Main Frame: remove");
//        } else {
//            System.out.println("...Maybe floating: remove");
//        }
    }

    @Override
    public void ancestorMoved(AncestorEvent event) {
    }
}
