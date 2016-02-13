/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.io;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import oct.analysis.application.OCTLine;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.AnalysisMode;
import oct.analysis.application.dat.LinePoint;
import oct.analysis.application.dat.OCTMode;
import oct.util.Line;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class AnalysisSaveState {
    /*
     Analysis info
     */

    private AnalysisMode analysisMode = null;
    /*
     Selection info
     */
    private List<OCTLine> lineSegs = null;
    private List<OCTSelection> selSegs = null;
    /*
     OCT dispaly panel info
     */
    private Point drawPoint = null;
    private LinkedList<Line> linesToDraw = null;
    private boolean drawLines = false;
    private boolean drawSelections = true;
    private boolean drawScaleBars = false;
    /*
     OCT and OCT analysis manager data
     */
    private double xscale;
    private double yscale;
    private int micronsBetweenSelections = 0;
    private byte[] logOCT = null;
    private String octFileName = null;
    private OCTMode displayMode = null; //default display mode of image is assumed to be a Log OCT image
    private int foveaCenterXPosition = -1;
    /*
     Selection Mngr data
     */
    private String selectedSelectionName = "";
    private int selectionWidth = 5;
    private int lrpSmoothingFactor = 5;
    /*
     image operations
     */
    private double blurFactor = 0D;
    private double sharpenSigma = 0D; //"Radius (Sigma)" is the standard deviation (blur radius) of the Gaussian blur that is subtracted.
    private float sharpenWeight = 0F; //Mask Weight" determines the strength of filtering, where "Mask Weight"=1 would be an infinite weight of the high-pass filtered image that is added.

    /*
     Accessor methods for building the bean
     */
    public AnalysisMode getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(AnalysisMode analysisMode) {
        this.analysisMode = analysisMode;
    }

    public List<OCTLine> getLineSegs() {
        return lineSegs;
    }

    public void setLineSegs(List<OCTLine> lineSegs) {
        this.lineSegs = lineSegs;
    }

    public List<OCTSelection> getSelSegs() {
        return selSegs;
    }

    public void setSelSegs(List<OCTSelection> selSegs) {
        this.selSegs = selSegs;
    }

    public Point getDrawPoint() {
        return drawPoint;
    }

    public void setDrawPoint(Point drawPoint) {
        this.drawPoint = drawPoint;
    }

    public LinkedList<Line> getLinesToDraw() {
        return linesToDraw;
    }

    public void setLinesToDraw(LinkedList<Line> linesToDraw) {
        this.linesToDraw = linesToDraw;
    }

    public boolean isDrawLines() {
        return drawLines;
    }

    public void setDrawLines(boolean drawLines) {
        this.drawLines = drawLines;
    }

    public boolean isDrawSelections() {
        return drawSelections;
    }

    public void setDrawSelections(boolean drawSelections) {
        this.drawSelections = drawSelections;
    }

    public double getXscale() {
        return xscale;
    }

    public void setXscale(double xscale) {
        this.xscale = xscale;
    }

    public int getMicronsBetweenSelections() {
        return micronsBetweenSelections;
    }

    public void setMicronsBetweenSelections(int micronsBetweenSelections) {
        this.micronsBetweenSelections = micronsBetweenSelections;
    }

    public byte[] getLogOCT() {
        return logOCT;
    }

    public void setLogOCT(byte[] logOCT) {
        this.logOCT = logOCT;
    }

    public OCTMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(OCTMode displayMode) {
        this.displayMode = displayMode;
    }

    public int getFoveaCenterXPosition() {
        return foveaCenterXPosition;
    }

    public void setFoveaCenterXPosition(int foveaCenterXPosition) {
        this.foveaCenterXPosition = foveaCenterXPosition;
    }

    public String getSelectedSelectionName() {
        return selectedSelectionName;
    }

    public void setSelectedSelectionName(String selectedSelectionName) {
        this.selectedSelectionName = selectedSelectionName;
    }

    public int getSelectionWidth() {
        return selectionWidth;
    }

    public void setSelectionWidth(int selectionWidth) {
        this.selectionWidth = selectionWidth;
    }

    public int getLrpSmoothingFactor() {
        return lrpSmoothingFactor;
    }

    public void setLrpSmoothingFactor(int lrpSmoothingFactor) {
        this.lrpSmoothingFactor = lrpSmoothingFactor;
    }

    public double getBlurFactor() {
        return blurFactor;
    }

    public void setBlurFactor(double blurFactor) {
        this.blurFactor = blurFactor;
    }

    public double getSharpenSigma() {
        return sharpenSigma;
    }

    public void setSharpenSigma(double sharpenSigma) {
        this.sharpenSigma = sharpenSigma;
    }

    public float getSharpenWeight() {
        return sharpenWeight;
    }

    public void setSharpenWeight(float sharpenWeight) {
        this.sharpenWeight = sharpenWeight;
    }

    public String getOctFileName() {
        return octFileName;
    }

    public void setOctFileName(String octFileName) {
        this.octFileName = octFileName;
    }

    public double getYscale() {
        return yscale;
    }

    public void setYscale(double yscale) {
        this.yscale = yscale;
    }

    public boolean isDrawScaleBars() {
        return drawScaleBars;
    }

    public void setDrawScaleBars(boolean drawScaleBars) {
        this.drawScaleBars = drawScaleBars;
    }

}
