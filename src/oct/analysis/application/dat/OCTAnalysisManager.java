/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.dat;

import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.OCTLine;
import oct.analysis.application.OCTSelection;
import oct.util.Segmentation;
import oct.util.Util;
import oct.util.ip.FilterOperation;
import oct.util.ip.FloatProcessorOperation;
import oct.util.ip.SharpenOperation;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

/**
 *
 * @author Brandon
 */
public class OCTAnalysisManager {

    /*
     property change support
     */
    public static final String PROP_FOVEA_CENTER_X_POSITION = "foveaCenterXPosition";
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private double xscale;
    private double yscale;
    private int micronsBetweenSelections = 0;
    private OCT oct = null;
    private OCTMode displayMode = OCTMode.LOG; //default display mode of image is assumed to be a Log OCT image
    private int foveaCenterXPosition = -1;
    private AnalysisMode analysisMode = null;
    private OCTImagePanel imgPanel;

    private OCTAnalysisManager() {
    }

    public static OCTAnalysisManager getInstance() {
        return OCTAnalysisManagerHolder.INSTANCE;
    }

    private static class OCTAnalysisManagerHolder {

        private static final OCTAnalysisManager INSTANCE = new OCTAnalysisManager();
    }

    public double getYscale() {
        return yscale;
    }

    public void setYscale(double yscale) {
        this.yscale = yscale;
    }

    public void setImjPanel(OCTImagePanel imjPanel) {
        this.imgPanel = imjPanel;
    }

    public OCTImagePanel getImgPanel() {
        return imgPanel;
    }

    public AnalysisMode getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(AnalysisMode analysisMode) {
        this.analysisMode = analysisMode;
    }

    /**
     * Obtain the X coordinate (relative to the OCT image) of the center of the
     * fovea.
     *
     * @return the X coordinate of the fovea relative to the OCT image supplied
     */
    public int getFoveaCenterXPosition() {
        return foveaCenterXPosition;
    }

    /**
     * Get the distance from the supplied X position on the OCT to the fovea (in
     * microns).
     *
     * @param xPos X position on OCT
     * @return distance between xPos and center of fovea in microns, returns -1
     * if the center of fovea hasn't be identified yet.
     */
    public double getDistanceFromFovea(int xPos) {
        if (foveaCenterXPosition < 0) {
            return -1D;
        } else {
            return (double) Math.abs(xPos - foveaCenterXPosition) * xscale;
        }
    }

    /**
     * Define where the center of the fovea is.
     *
     * @param foveaCenterXPosition
     */
    public void setFoveaCenterXPosition(int foveaCenterXPosition) {
        int oldval = this.foveaCenterXPosition;
        this.foveaCenterXPosition = foveaCenterXPosition;
        propertyChangeSupport.firePropertyChange(PROP_FOVEA_CENTER_X_POSITION, oldval, foveaCenterXPosition);
    }

    /**
     * This method will take care of interacting with the user in determining
     * where the fovea is within the OCT. It first lets the user inspect the
     * automatically identified locations where the fovea may be and then choose
     * the selection that is at the fovea. If none of the automated findings are
     * at the fovea the user has the option to manual specify it's location.
     * Finally, the chosen X coordinate (within the OCT) of the fovea is set in
     * the manager and can be obtained via the getFoveaCenterXPosition getter.
     *
     * @param fullAutoMode find the fovea automatically without user input when
     * true, otherwise find the fovea in semi-automatic mode involving user
     * interaction
     */
    public void findCenterOfFovea(boolean fullAutoMode) throws InterruptedException, ExecutionException {
        //disable clicking other components while processing by enabling glass pane
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(imgPanel);
        Component glassPane = topFrame.getGlassPane();
        glassPane.setVisible(true);

        //monitor progress of finding the fovea
        ProgressMonitor pm = new ProgressMonitor(imgPanel,
                "Analyzing OCT for fovea...",
                "", 0, 100);
        pm.setMillisToDecideToPopup(0);
        pm.setMillisToPopup(100);
        pm.setProgress(0);
        FoveaFindingTask fvtask = new FoveaFindingTask(!fullAutoMode, glassPane);
        fvtask.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if ("progress".equals(evt.getPropertyName())) {
                int progress1 = (Integer) evt.getNewValue();
                pm.setProgress(progress1);
            }
        });
        fvtask.execute();
    }

    /**
     * Using the previously supplied values for the number of microns between
     * selections calculate the distance from the center of the fovea based on
     * the selection order distance away from the fovea. That is, the distance
     * returned by this method will indicate how far from the center of the
     * fovea a selection should be placed based on the number of selections
     * between it and the fovea.
     *
     * <p>
     * For example, a multiplier of 1 indicates the first selection set a
     * distance away from the fovea. A multiplier of 2 will be the second
     * selection encountered when counting selections outward from the fovea.
     * This continues for any multiplier, irrespective of direction.
     * </p>
     *
     * @param multiplier
     * @return
     */
    public int getNumPixelFromFovea(int multiplier) {
        return (oct == null) ? -1 : (int) Math.round((double) (micronsBetweenSelections * multiplier) * (1D / xscale));
    }

    public int getMicronsBetweenSelections() {
        return micronsBetweenSelections;
    }

    public void setMicronsBetweenSelections(int micronsBetweenSelections) {
        this.micronsBetweenSelections = micronsBetweenSelections;
    }

    public OCT getOct() {
        return oct;
    }

    public void setOct(OCT oct) {
        this.oct = oct;
    }

    public void setScale(double axialLength, double nominalScanWidth, int octWidth) {
        double scanLength = (nominalScanWidth * axialLength) / 24D;
        setXscale(((scanLength * 1000D) / (double) octWidth));
    }

    public void setXscale(double xscale) {
        this.xscale = xscale;
    }

    /**
     * Changes the mode with which the OCT should be rendered. Calling this
     * method will cause the panel to redraw the OCT and any analysis artifacts
     * using the new mode setting.
     *
     * @param mode th mode to change the display of the OCT image
     */
    public void setOCTMode(OCTMode mode) {
        this.displayMode = mode;
    }

    public double getXscale() {
        return xscale;
    }

    public OCTMode getDisplayMode() {
        return displayMode;
    }

    /**
     * This method returns the OCT image according to the currently set OCT mode
     * and image operations.
     *
     * @return
     */
    public BufferedImage getOctImage() {
        BufferedImage modeOCT = (displayMode == OCTMode.LOG) ? oct.getLogOctImage() : oct.getLinearOctImage();
//        JOptionPane.showMessageDialog(null, new JLabel(new ImageIcon(modeOCT)));
        for (FilterOperation imop : ImageOperationManager.getInstance().getActiveCustomOperationList()) {
            modeOCT = imop.performOperation(modeOCT);
        }
        return modeOCT;
    }

    /**
     * Segment the four major layers of the retina from the Log OCT image. An
     * optional {@code ImageOperation} can be applied to the image before the
     * segmentation is performed, to help improve segmentation performance.
     *
     * @param optionalOp an ImageOperation to apply before segmenting an image,
     * or null (indicating tat no operations should be performed before
     * segmenting the OCT)
     * @return segmentation of the OCT
     */
    public Segmentation getSegmentation(FloatProcessorOperation optionalOp) {
        //segmentation and image operations can only be done on 8-bit gray xscale images, using the OCT we ensure 
        //the image is in useable format which handles this upon creation
        BufferedImage segImg;
        if (optionalOp != null) {
            //apply supplied operation before segmenting OCT
            FloatProcessor tmpFp = new ByteProcessor(oct.getLogOctImage()).convertToFloatProcessor();
            tmpFp.snapshot();//need to create a snapshot before any operations can be performed on image
            optionalOp.performOperation(tmpFp);
            segImg = tmpFp.getBufferedImage();
        } else {
            segImg = oct.getLogOctImage();
        }

        return new Segmentation(segImg, 1);
    }

    /**
     * Segment the four major layers of the retina from the Log OCT image. An
     * optional {@code ImageOperation} can be applied to the image before the
     * segmentation is performed, to help improve segmentation performance.
     *
     * @param optionalOp an ImageOperation to apply before segmenting an image,
     * or null (indicating tat no operations should be performed before
     * segmenting the OCT)
     * @return segmentation of the OCT
     */
    public Segmentation getSegmentation(FilterOperation optionalOp) {
        //segmentation and image operations can only be done on 8-bit gray xscale images, using the OCT we ensure 
        //the image is in useable format which handles this upon creation
        BufferedImage segImg;
        if (optionalOp != null) {
            segImg = optionalOp.performOperation(oct.getLogOctImage());
        } else {
            segImg = oct.getLogOctImage();
        }

        return new Segmentation(segImg, 1);
    }

    /**
     * This method grabs the current OCT and sharpens it using a radius (sigma)
     * of 15 and a weight factor of the supplied value. The sharpened image is
     * then returned.
     *
     * @return sharpened image
     */
    public BufferedImage getSharpenedOctImage(double sigma, float weight) {
        FloatProcessor tmpFp = new ByteProcessor(oct.getLogOctImage()).convertToFloatProcessor();
        tmpFp.snapshot();//need to create a snapshot before any operations can be performed on image
//        new SharpenOperation(sigma, weight).performOperation(tmpFp);
        return tmpFp.getBufferedImage();
    }

    public List<LinePoint> findAbsoluteDiff(UnivariateFunction fa, UnivariateFunction fb, int minX, int maxX) {
        return IntStream.rangeClosed(minX, maxX)
                .mapToObj(x -> new LinePoint(x, Math.abs(fa.value(x) - fb.value(x))))
                .collect(Collectors.toList());
    }

    public List<LinePoint> findAbsoluteDiff(List<LinePoint> fa, List<LinePoint> fb) {
        ListIterator<LinePoint> faIter, fbIter;
        if (fa.get(0).getX() == fb.get(0).getX()) {
            faIter = fa.listIterator();
            fbIter = fb.listIterator();
        } else if (fa.get(0).getX() > fb.get(0).getX()) {
            faIter = fa.listIterator();
            fbIter = fb.listIterator(fa.get(0).getX() - fb.get(0).getX());
        } else {
            faIter = fa.listIterator(fb.get(0).getX() - fa.get(0).getX());
            fbIter = fb.listIterator();
        }
        LinkedList<LinePoint> retLine = new LinkedList<>();
        while (faIter.hasNext() && fbIter.hasNext()) {
            LinePoint pointA = faIter.next();
            LinePoint pointB = fbIter.next();
            retLine.add(new LinePoint(pointA.getX(), Math.abs(pointA.getY() - pointB.getY())));
        }
        return retLine;
    }

    private class FoveaFindingTask extends SwingWorker<List<Integer>, Integer> {

        private final boolean interactiveMode;
        private final Component glassPane;

        public FoveaFindingTask(boolean interactiveMode, Component glassWindow) {
            this.interactiveMode = interactiveMode;
            this.glassPane = glassWindow;
        }

        @Override
        protected List<Integer> doInBackground() throws Exception {
            return findPotentialFoveaSites();
        }

        private List<Integer> findPotentialFoveaSites() {
            //find the fovea since it hasn't been found/defined yet
            UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
            setProgress(5);
            Segmentation octSeg = getSegmentation(new SharpenOperation(15, 0.6F));
            setProgress(50);
            double[][] ilmSeg = Util.getXYArraysFromPoints(new ArrayList<>(octSeg.getSegment(Segmentation.ILM_SEGMENT)));
            UnivariateFunction ilmInterp = interpolator.interpolate(ilmSeg[0], ilmSeg[1]);
            LinkedList<LinePoint> ilmLine = new LinkedList<>();
            IntStream.range(0, oct.getImageWidth() - 1).forEach((int i) -> {
                ilmLine.add(new LinePoint(i, ilmInterp.value(i)));
            });
            double[][] brmSeg = Util.getXYArraysFromPoints(new ArrayList<>(octSeg.getSegment(Segmentation.BrM_SEGMENT)));
            UnivariateFunction brmInterp = interpolator.interpolate(brmSeg[0], brmSeg[1]);
            LinkedList<LinePoint> brmLine = new LinkedList<>();
            IntStream.range(0, oct.getImageWidth() - 1).forEach((int i) -> {
                brmLine.add(new LinePoint(i, brmInterp.value(i)));
            });
            imgPanel.addDrawnLine(ilmLine, brmLine);
            double[][] diffLine = Util.getXYArraysFromLinePoints(findAbsoluteDiff(brmInterp, ilmInterp, 0, oct.getLinearOctImage().getWidth() - 1));
            UnivariateFunction diffInerp = interpolator.interpolate(diffLine[0], diffLine[1]);
            FiniteDifferencesDifferentiator differ = new FiniteDifferencesDifferentiator(4, 0.25);
            UnivariateDifferentiableFunction difFunc = differ.differentiate(diffInerp);
            setProgress(80);
            /*
             * collect the first derivative at each pixel in the image
             */
            int numFreeVariablesInFunction = 1;
            int order = 1;
            DerivativeStructure xd;
            DerivativeStructure yd;
            ArrayList<LinePoint> firstDeriv = new ArrayList<>(oct.getLinearOctImage().getWidth() - 1);
            IntStream.range(0, oct.getLinearOctImage().getWidth() - 1).forEach((int i) -> {
                firstDeriv.add(new LinePoint(0, 0));
            });
            for (int xRealValue = 1; xRealValue <= oct.getLinearOctImage().getWidth() - 2; xRealValue++) {
                xd = new DerivativeStructure(numFreeVariablesInFunction, order, 0, xRealValue);
                yd = difFunc.value(xd);
                firstDeriv.set(xRealValue, new LinePoint(xRealValue, yd.getPartialDerivative(1)));
            }
            setProgress(90);
            List<LinePoint> peaks = Util.findMaxAndMins(firstDeriv);
            LinePoint prevPeak = null;
            LinkedList<Diff> diffs = new LinkedList<>();
            for (LinePoint curPeak : peaks) {
                if (prevPeak != null) {
                    diffs.add(new Diff(prevPeak, curPeak));
                }
                prevPeak = curPeak;
            }
            Diff maxDiff = diffs.stream().max(Comparator.comparingDouble((Diff diff) -> diff.getYDiff())).get();
            double sign = Math.signum(maxDiff.getLinePoint1().getY());
            int signChangeXPos = maxDiff.getLinePoint1().getX() + 1;
            while (sign == Math.signum(firstDeriv.get(signChangeXPos).getY())) {
                signChangeXPos++;
            }
            //add the most likely fovea position to list first
            int foveaCenterXPosition = (Math.abs(firstDeriv.get(signChangeXPos).getY()) < Math.abs(firstDeriv.get(signChangeXPos - 1).getY())) ? signChangeXPos : signChangeXPos - 1;
            LinkedList<Integer> positionList = new LinkedList<>();
            positionList.add(foveaCenterXPosition);
            //find other zero crossings
            for (LinePoint curPeak : peaks) {
                if (prevPeak != null && !prevPeak.equals(maxDiff.getLinePoint1())) {
                    sign = Math.signum(prevPeak.getY());
                    signChangeXPos = prevPeak.getX() + 1;
                    try {
                        while (sign == Math.signum(firstDeriv.get(signChangeXPos).getY())) {
                            signChangeXPos++;
                        }
                        //add other possible fovea site to list
                        foveaCenterXPosition = (Math.abs(firstDeriv.get(signChangeXPos).getY()) < Math.abs(firstDeriv.get(signChangeXPos - 1).getY())) ? signChangeXPos : signChangeXPos - 1;
                        positionList.add(foveaCenterXPosition);
                    } catch (IndexOutOfBoundsException ie) {
                        //caused because the first derivative line is shorter than the original
                        //fail sillently
                    }
                }
                prevPeak = curPeak;
            }
            setProgress(100);
            return positionList;
        }

        @Override
        protected void done() {
            SelectionLRPManager selMngr = SelectionLRPManager.getInstance();

            //grab findings
            List<Integer> sites = null;
            try {
                sites = get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(OCTAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
            }

            //process based of of fovea finding user interaction mode
            if (interactiveMode) {
                //since the glass pane is blocking user interation with the UI add
                //a listener that will only pass through clicks over the image panel
                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(imgPanel);
                glassPane.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        /*
                         With the glasspane up we have to listen for click events 
                         and forward them if they fit with this process. In this case
                         we will only forward clicks to the JPanel that displays the
                         OCT.
                         */
                        Point glassPanePoint = e.getPoint();
                        Container container = topFrame.getContentPane();
                        Point containerPoint = SwingUtilities.convertPoint(
                                glassPane,
                                glassPanePoint,
                                container);
                        if (containerPoint.y > 0) {
                            //The mouse event is probably over the content pane.
                            //Find out exactly which component it's over.  
                            Component component
                                    = SwingUtilities.getDeepestComponentAt(
                                            container,
                                            containerPoint.x,
                                            containerPoint.y);

                            if ((component != null)
                                    && (component.equals(imgPanel))) {
                                //process where user clicked over the JPanel that displays the OCT to the panel to process.
                                Point componentPoint = SwingUtilities.convertPoint(
                                        glassPane,
                                        glassPanePoint,
                                        component);
                                //determine if click was over the OCT image
                                if (imgPanel.coordinateOverlapsOCT(componentPoint)) {
                                    //determine if click was over one of the possible fovea selections
                                    OCTSelection selection = selMngr.getSelection(imgPanel.translatePanelPointToOctPoint(componentPoint), false);
                                    if (selection == null) {
                                        //user decided that none of the automated selections was correct and made their own selection
                                        //check that new selection is what they want
                                        //clear all selections from being displayed
                                        selMngr.removeSelections(true);
                                        //translate component coordinates to OCT coordinates
                                        Point p = imgPanel.translatePanelPointToOctPoint(componentPoint);
                                        selection = new OCTLine(p.x, 0, oct.getImageHeight(), SelectionType.FOVEAL, "Fovea", false);
                                        selMngr.addOrUpdateSelection(selection);
                                        imgPanel.repaint();
                                        if (JOptionPane.showConfirmDialog(imgPanel, "Is this the location of the center of the fovea? If not hit 'No' and click on the image where you believe the center of the fovea resides.", "Center of Fovea?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                                            glassPane.removeMouseListener(this);
                                            glassPane.setVisible(false);
                                            setFoveaCenterXPosition(selection.getXPositionOnOct());
                                            selMngr.removeSelections(true);
                                            imgPanel.repaint();
                                        }
                                    } else {
                                        glassPane.removeMouseListener(this);
                                        glassPane.setVisible(false);
                                        setFoveaCenterXPosition(selection.getXPositionOnOct());
                                        selMngr.removeSelections(true);
                                        imgPanel.repaint();
                                    }
                                }
                            }
                        }
                    }
                });

                //draw potential fovea selections to screen for user to choose from
                if (sites == null) {
                    sites = new LinkedList<>();
                }
                if (sites.isEmpty()) {
                    sites.add(oct.getImageWidth() / 2);
                }
                sites.forEach(x -> {
                    selMngr.addOrUpdateSelection(new OCTLine(x, 0, oct.getImageHeight(), SelectionType.FOVEAL, "Potential Fovea @ " + x, false));
                });
                imgPanel.repaint();

                //notify user of how they can select the selection that is the fovea or make a new selection
                JOptionPane.showMessageDialog(imgPanel,
                        "Please select (by clicking one of the gray boxes at the top of a selection)\n"
                        + " the selection that you believe is the fovea. If none of the\n"
                        + " presented seletions look like the location of the fovea click\n"
                        + " anywhere on the image to assign the location manually.",
                        "Select Fovea",
                        JOptionPane.INFORMATION_MESSAGE);

            } else {
                //auto identification process returns most likely result without user interaction
                if (sites == null || sites.isEmpty()) {
                    setFoveaCenterXPosition(oct.getImageWidth() / 2);
                } else {
                    setFoveaCenterXPosition(sites.get(0));
                }
            }
        }

    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

}
