/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Point;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import oct.analysis.application.dat.LinePoint;
import oct.analysis.application.dat.OCT;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SelectionLRPManager;

/**
 *
 * @author Brandon
 */
public class OCTImagePanel extends JPanel {

    private final OCTAnalysisManager analysisData = OCTAnalysisManager.getInstance();
    private final SelectionLRPManager selectionLrpMngr = SelectionLRPManager.getInstance();
    /*
     Image offsets must be tracked to manage drawing the selections in the same place
     over the OCT despite the image panel resizing.
     */
    private int imageOffsetY = 0;
    private int imageOffsetX = 0;
    private Point drawPoint = null;
    private LinkedList<List<LinePoint>> linesToDraw = null;
    private boolean drawLines;
    private boolean drawSelections = true;

    public OCTImagePanel(LayoutManager lm, boolean bln) {
        super(lm, bln);
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public OCTImagePanel(LayoutManager lm) {
        super(lm);
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public OCTImagePanel(boolean bln) {
        super(bln);
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public OCTImagePanel() {
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    @Override
    public Dimension getPreferredSize() {
        if (analysisData.getOct() == null) {
            return super.getPreferredSize(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return new Dimension(analysisData.getOct().getImageWidth(), analysisData.getOct().getImageHeight());
        }
    }

    public void resetImageOffsets() {
        imageOffsetY = imageOffsetX = 0;
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        OCT oct = analysisData.getOct();
        if (oct != null) {
            //center the image within the panel, make sure the selection draw method takes this into account
            int imageWidth = oct.getImageWidth();
            int panelWidth = this.getWidth();
            if (panelWidth > imageWidth) {
                imageOffsetX = (panelWidth - imageWidth) / 2;
            }
            int imageHeight = oct.getImageHeight();
            int panelHeight = this.getHeight();
            if (panelHeight > imageHeight) {
                imageOffsetY = (panelHeight - imageHeight) / 2;
            }
            //draw OCT to the JPanel
            grphcs.drawImage(analysisData.getOctImage(), imageOffsetX, imageOffsetY, null);
            //draw the selections to the panel if available
            if (drawSelections) {
                selectionLrpMngr.getSelections().stream().forEach((selection) -> {
                    selection.drawSelection(grphcs, imageOffsetX, imageOffsetY);
                });
            }
            //draw point on oct if available
            if (drawPoint != null && drawLines) {
                grphcs.setColor(Color.red);
                grphcs.drawRect(imageOffsetX + drawPoint.x - 1, imageOffsetY + drawPoint.y - 1, 3, 3);
            }
            //draw lines on oct if available
            if (linesToDraw != null && drawLines) {
                grphcs.setColor(Color.red);
                linesToDraw.stream().flatMap(line -> line.stream()).forEach(p -> {
                    int y = imageOffsetY + (int) Math.round(p.getY());
                    grphcs.drawLine(imageOffsetX + p.getX(), y, imageOffsetX + p.getX(), y);
                });
            }
        }
    }

    public void setDrawPoint(Point p) {
        drawPoint = p;
        repaint();
    }

    public void setDrawnLines(List<LinePoint>... linesToDraw) {
        this.linesToDraw = new LinkedList<>();
        addDrawnLine(linesToDraw);
    }

    public void addDrawnLine(List<LinePoint>... linesToDraw) {
        Arrays.stream(linesToDraw).forEach(r -> {
            this.linesToDraw.add(r);
        });
    }

    public void showSelections() {
        this.drawSelections = true;
        repaint();
    }

    public void hideSelections() {
        this.drawSelections = false;
        repaint();
    }

    public void hideLines() {
        drawLines = false;
        repaint();
    }

    public void showLines() {
        drawLines = true;
        repaint();
    }

    public boolean isDoDraw() {
        return drawLines;
    }

    public void clearDrawnLines() {
        this.linesToDraw = null;
        this.drawPoint = null;
    }

    public int getImageOffsetY() {
        return imageOffsetY;
    }

    public int getImageOffsetX() {
        return imageOffsetX;
    }

    public Point getDrawPoint() {
        return drawPoint;
    }

    public LinkedList<List<LinePoint>> getLinesToDraw() {
        return linesToDraw;
    }

    public boolean isDrawLines() {
        return drawLines;
    }

    public boolean isDrawSelections() {
        return drawSelections;
    }

    /**
     * Determine if the supplied coordinate overlaps with the area of this panel
     * that displays the OCT image
     *
     * @param x
     * @param y
     * @return true if the coordinate is within the bounds of the displayed OCT,
     * false if it isn't or if the OCT image isn't displayed already
     */
    public boolean coordinateOverlapsOCT(int x, int y) {
        OCT oct = analysisData.getOct();
        if (oct != null) {
            boolean withinX = ((imageOffsetX + oct.getImageWidth()) - x) * (x - imageOffsetX) > -1;
            boolean withinY = ((imageOffsetY + oct.getImageHeight()) - y) * (y - imageOffsetY) > -1;
            return withinX && withinY;
        } else {
            return false;
        }
    }

    /**
     * Determine if the supplied coordinate overlaps with the area of this panel
     * that displays the OCT image
     *
     * @param x
     * @param y
     * @return true if the coordinate is within the bounds of the displayed OCT,
     * false if it isn't or if the OCT image isn't displayed already
     */
    public boolean coordinateOverlapsOCT(Point p) {
        return coordinateOverlapsOCT(p.x, p.y);
    }

    /**
     * Utility method used to translate a point (i.e. the location of an event,
     * like a mouse click) in the coordinate space of this panel to the
     * coordinate space of the OCT being displayed in the panel.
     *
     * @param p The point to be translated
     * @return translated point from Panel coordinates -> OCT coordinates, or
     * null if the OCT isn't present
     */
    public Point translatePanelPointToOctPoint(Point p) {
        if (analysisData.getOct() == null) {
            return null;
        } else {
            return new Point(p.x - imageOffsetX, p.y - imageOffsetY);
        }
    }
}
