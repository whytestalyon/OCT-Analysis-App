/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.calc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author Brandon
 */
public class LRPUtil {

    /**
     * Creates a panel for the demo (used by SuperDemo.java).
     *
     * @param lrp
     * @return A panel.
     */
    public static JPanel createLRPPanel(XYSeriesCollection lrp) {
        JFreeChart chart = ChartFactory.createScatterPlot("LRP", "Avg. Pixel Intensity", "Pixel Height", lrp);
        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(200, 200));
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    public static void displayLRPs(List<XYSeriesCollection> lrps, Component relativeTo) {
        lrps.stream().forEach((lrpSeries) -> {
            JPanel lrp = createLRPPanel(lrpSeries);
            System.out.println("Adding lrp: " + ((ChartPanel) lrp).getChart().getTitle().getText());
            SwingUtilities.invokeLater(() -> {
                JFrame popup = new JFrame(((ChartPanel) lrp).getChart().getTitle().getText());
                popup.add(lrp, BorderLayout.CENTER);
                popup.setSize(400, 400);
                popup.setLocationRelativeTo(relativeTo);
                popup.setVisible(true);
                System.out.println("Popping up " + popup.getTitle() + " window!");
            });
        });
    }
}
