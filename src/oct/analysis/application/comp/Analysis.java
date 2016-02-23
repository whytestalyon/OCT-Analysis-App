/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTLine;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.SegmentationManager;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.analysis.application.dat.SelectionType;
import oct.util.Line;
import oct.util.Segmentation;

/**
 *
 * @author Brandon
 */
public class Analysis {

    private static final OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();
    private static final SelectionLRPManager selectionLRPManager = SelectionLRPManager.getInstance();
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    public static void findEZ(boolean interactive) {
        //based off of interactive mode specified, find the fovea, and then trigger EZ edge detection
        try {
            octMngr.findCenterOfFovea(!interactive);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(OCTAnalysisUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        octMngr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //if the change that occured was the setting (or update ) of the position of the fovea
                //then trigger the identification of the EZ edges
                if (OCTAnalysisManager.PROP_FOVEA_CENTER_X_POSITION.equals(evt.getPropertyName())) {
//                    SwingUtilities.invokeLater(() -> {
//                        int fv = octMngr.getFoveaCenterXPosition();
//                        OCTLine foveaSelection = new OCTLine(fv, 0, octMngr.getOct().getImageHeight(), SelectionType.FOVEAL, "Fovea", false);
//                        selectionLRPManager.addOrUpdateSelection(foveaSelection);
//                        octMngr.getImgPanel().repaint();
//                        //second, automatically find the X position of each EZ edge
//                        int[] ez = octMngr.getEZEdgeCoords();
//                        selectionLRPManager.addOrUpdateSelection(new OCTLine(ez[0], 0, octMngr.getOct().getImageHeight(), SelectionType.NONFOVEAL, "EZ Left", true));
//                        selectionLRPManager.addOrUpdateSelection(new OCTLine(ez[1], 0, octMngr.getOct().getImageHeight(), SelectionType.NONFOVEAL, "EZ Right", true));
//                        octMngr.getImgPanel().repaint();
//                    });

                    //execute the EZ edge detection as a seperate thread that will update the UI when the edges of the EZ are found
                    //monitor progress of finding the fovea
                    ProgressMonitor pm = new ProgressMonitor(octMngr.getImgPanel(),
                            "Analyzing OCT for edge of EZ...",
                            "", 0, 100);
                    pm.setMillisToDecideToPopup(0);
                    pm.setMillisToPopup(100);
                    pm.setProgress(0);
                    EZWorker ezw = new EZWorker(pm);
                    ezw.addPropertyChangeListener((PropertyChangeEvent ev) -> {
                        if ("progress".equals(ev.getPropertyName())) {
                            int progress1 = (Integer) ev.getNewValue();
                            pm.setProgress(progress1);
                        }
                    });
                    ezw.execute();

                    //remove the listener since it's no longer needed
                    octMngr.removePropertyChangeListener(this);
                }
            }
        });
    }

    public static void findFovea(boolean interactive) {
        try {
            octMngr.findCenterOfFovea(!interactive);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(OCTAnalysisUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        octMngr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //if the change that occured was the setting (or update ) of the position of the fovea
                //then paint the fovea to the screen
                if (OCTAnalysisManager.PROP_FOVEA_CENTER_X_POSITION.equals(evt.getPropertyName())) {
                    SwingUtilities.invokeLater(() -> {
                        int fv = octMngr.getFoveaCenterXPosition();
                        OCTSelection foveaSelection = selectionLRPManager.getFoveaSelection(fv, false);
                        selectionLRPManager.addOrUpdateSelection(foveaSelection);
                        octMngr.getImgPanel().repaint();
                    });
                    //remove the listener since it's no longer needed
                    octMngr.removePropertyChangeListener(this);
                }
            }
        });
    }

    public static void performEquidistant(boolean interactive) {
        //ask for the desired distance between selections
        int micronsBetweenSelections = (int) Math.round(oct.util.Util.parseNumberFromInput((String) JOptionPane.showInputDialog(null, "Enter the desired distance between selections(microns):", "Distance between selections", JOptionPane.QUESTION_MESSAGE)));
        //calculate the center of the fovea
        try {
            octMngr.findCenterOfFovea(!interactive);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(OCTAnalysisUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        octMngr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //if the change that occured was the setting (or update ) of the position of the fovea
                if (OCTAnalysisManager.PROP_FOVEA_CENTER_X_POSITION.equals(evt.getPropertyName())) {
                    SwingUtilities.invokeLater(() -> {
                        int fv = octMngr.getFoveaCenterXPosition();
                        //add equidistant selections from fovea (including the fovea selections)
                        selectionLRPManager.addOrUpdateEquidistantSelections(fv, micronsBetweenSelections);
                        octMngr.getImgPanel().repaint();
                    });
                    //remove the listener since it's no longer needed
                    octMngr.removePropertyChangeListener(this);
                }
            }
        });
    }

    public static void performMirror(boolean interactive) {
        findFovea(interactive);
    }

    public static void performWilkSpot() {
        JOptionPane.showMessageDialog(OCTAnalysisUI.getInstance(), "Please select location on the OCT that represent the greatest distance between the EZ and IZ bands.");
        WilkSpotClickListener wilkSpotClickListener = new WilkSpotClickListener();
        octMngr.getImgPanel().addMouseListener(wilkSpotClickListener);
    }

    public static void performOSRatio(boolean interactive) {
        //perform segmentation
        SegmentationManager segMngr = SegmentationManager.getInstance();
        segMngr.addAll(Segmentation.getBestSegmentationLines(octMngr.getOct()));

        //display segmentation
        octMngr.getImgPanel().addDrawnLines(segMngr);
        octMngr.getImgPanel().showLines();
        OCTAnalysisUI.getInstance().getDispSegmentationCheckBox().setSelected(true);

        //request user to select segmentation lines for the EZ and IZ
        JOptionPane.showMessageDialog(OCTAnalysisUI.getInstance(), "Please select the segmenation lines that represent the EZ and IZ bands.");

        OSRatioClickListener seglistener = new OSRatioClickListener();
        octMngr.getImgPanel().addMouseListener(seglistener);

        segMngr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (SegmentationManager.PROP_SELECTEDSEGLINES_SIZE.equals(evt.getPropertyName())) {
                    //user selected two segmentation lines
                    //find the 'Wilk Spot' a.k.a. the greatest distance between the 
                    //EZ and IZ (not necessarily the bottom of the foveal pit)
                    octMngr.getImgPanel().removeMouseListener(seglistener);
                    PointDiff maxDifPoint = segMngr.getSelectedSegLines()
                            .stream()
                            .flatMap(List::stream)
                            .collect(Collectors.groupingBy(p -> p.x))
                            .values()
                            .stream()
                            .filter(diffList -> diffList.size() > 1)
                            .map(plist -> {
                                int miny = plist.stream().mapToInt(p -> p.y).min().getAsInt();
                                int maxy = plist.stream().mapToInt(p -> p.y).max().getAsInt();
                                return new PointDiff(plist.get(0).x, maxy - miny);
                            })
                            .max((pdif1, pdif2) -> {
                                return Integer.compare(pdif1.distance, pdif2.distance);
                            })
                            .get();
                    SwingUtilities.invokeLater(() -> {
                        OCTSelection foveaSelection = selectionLRPManager.getFoveaSelection(maxDifPoint.x, false);
                        selectionLRPManager.addOrUpdateSelection(foveaSelection);
                        octMngr.setFoveaCenterXPosition(maxDifPoint.x);
                        segMngr.getSelectedSegLines()
                                .forEach(l -> {
                                    l.setDrawColor(Line.DEFAULT_LINE_COLOR);
                                });
                        //clear out the selected segmentation lines
                        segMngr.resetSelectedSegs();
                        octMngr.getImgPanel().repaint();
                        //notify user of max distance between EZ and IZ
                        double diffInMicrons = (double) maxDifPoint.distance * octMngr.getYscale();
                        JOptionPane.showMessageDialog(null, "Max distance between IZ and EZ: " + df.format(diffInMicrons) + "\u00B5m");
                    });
                    //remove the listener since it's no longer needed
                    segMngr.removePropertyChangeListener(this);
                }
            }
        });

    }

    private static class PointDiff {

        int x, distance;

        public PointDiff(int x, int distance) {
            this.x = x;
            this.distance = distance;
        }

        public int getX() {
            return x;
        }

        public int getDistance() {
            return distance;
        }

    }
}
