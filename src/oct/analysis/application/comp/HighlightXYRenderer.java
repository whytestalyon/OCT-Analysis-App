/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.awt.Color;
import java.awt.Paint;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class HighlightXYRenderer extends XYLineAndShapeRenderer {

    /**
     * The index of the series to highlight (-1 for none).
     */
    private int highlightSeries = -1;

    /**
     * The index of the item to highlight (-1 for none).
     */
    private int highlightItem = -1;

    /**
     * Sets the item to be highlighted (use (-1, -1) for no highlight).
     *
     * @param seriesIndex
     * @param itemIndex
     */
    public void setHighlightedItem(int seriesIndex, int itemIndex) {
        if (highlightSeries == seriesIndex && highlightItem == itemIndex) {
            return;  // nothing to do
        }
        highlightSeries = seriesIndex;
        highlightItem = itemIndex;
        notifyListeners(new RendererChangeEvent(this));
    }

    /**
     * Return a special color for the highlighted item.
     *
     * @param seriesIndex the series index.
     * @param itemIndex the item index.
     *
     * @return The outline paint.
     */
    @Override
    public Paint getItemOutlinePaint(int seriesIndex, int itemIndex) {
//        System.out.println("Looking for (" + seriesIndex + "," + itemIndex + ")");
        if (seriesIndex == highlightSeries && itemIndex == highlightItem) {
            return Color.yellow;
        }
        return super.getItemOutlinePaint(seriesIndex, itemIndex);
    }
}
