/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.io;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCT;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.OSLengthResult;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.util.Util;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Brandon
 */
public class AnalysisSaver {

    private static final OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();
    private static final SelectionLRPManager selMngr = SelectionLRPManager.getInstance();
    private static final DecimalFormat df = new DecimalFormat("#.00");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-YY-hh-mm-ss");

    /**
     * Utility method capable of writing analysis information to file.
     *
     * @param saveFile
     */
    public static void saveAnalysis(File saveFile) {
        AnalysisSaveState analysisSaveState = Util.getAnalysisSaveState();
        try (PrintWriter pw = new PrintWriter(saveFile)) {
            Gson gson = new Gson();
            pw.append(gson.toJson(analysisSaveState));
            pw.flush();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(OCTAnalysisUI.class.getName()).log(Level.SEVERE, "Analysis save error.", ex);
        }
    }

    public static AnalysisSaveState readAnalysis(File analysisFile) throws IOException {
        return readAnalysis(new FileReader(analysisFile));
    }

    public static AnalysisSaveState readAnalysis(Reader analysisFileReader) throws IOException {
        Gson gson = new Gson();
        String analysisJson;
        try (BufferedReader br = new BufferedReader(analysisFileReader)) {
            analysisJson = br.lines()
                    .collect(Collectors.joining(""));
        }
        return gson.fromJson(analysisJson, AnalysisSaveState.class);
    }

    public static boolean exportAnalysisData(File outputDir) throws IOException {
        //check if app has permissions to write to supplied directory
        if (!outputDir.canWrite()) {
            return false;
        }
        //get OCT file name without extension
        String fdate = sdf.format(new Date());
        String fileNameStub = octMngr.getOct().getFileName().replaceFirst("\\.[^\\.]+", "");
        saveAnalysis(new File(outputDir,
                fileNameStub + "_" + octMngr.getAnalysisMode().toString().toLowerCase() + "_analysis_" + fdate + ".ora"));
        //grab selection(s) and determine if information about LRPs needs to be exported
        List<OCTSelection> selections = selMngr.getSelections();
        if (!selections.isEmpty()) {
            //save capture of OCT as displayed to user
            File screenFile;
            screenFile = new File(outputDir,
                    fileNameStub + "_" + octMngr.getAnalysisMode().toString().toLowerCase() + "_oct_" + fdate + ".png");
            ImageIO.write(getOCTScreenShot(octMngr.getImgPanel()), "png", screenFile);
            //define output files
            File statsFile;
            statsFile = new File(outputDir,
                    fileNameStub + "_" + octMngr.getAnalysisMode().toString().toLowerCase() + "_stats_" + fdate + ".csv");
            File lrpFile;
            File lrpPeaksFile;
            /*
             Based on the type of analysis that was being performed export the data
             accordingly
             */
            switch (octMngr.getAnalysisMode()) {
                case OS_LENGTH:
                    OSLengthResult oslr = OSLengthResult.getInstance();
                    //export CSV of Segmentation lines information
                    File segFile = new File(outputDir,
                            fileNameStub + "_ez_iz_segmentation_" + fdate + ".csv");
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(segFile)))) {
                        pw.println("x coord.,EZ y coord.,IZ y coord.");
                        //print to file
                        for (int i = 0; i < oslr.getEz().size(); i++) {
                            int x = oslr.getEz().get(i).x;
                            int ezy = oslr.getEz().get(i).y;
                            int izy = oslr.getIz().get(i).y;
                            pw.println(x + "," + ezy + "," + izy);
                        }
                    }
                    //export CSV file with the difference line, gaussian fit(s) and peak difference
                    File fitFile = new File(outputDir,
                            fileNameStub + "_os_length_diff_fit_" + fdate + ".csv");
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fitFile)))) {
                        boolean hasImageJGaussian = oslr.getIjgPoints().getItemCount() != 0;
                        String header = "diff. x coord.,diff y coord.,Apache math gaussian fit x coord.,Apache math gaussian fit y coord.,apache math gaussian fit peak x coord.,apache math gaussian fit peak y coord.";
                        if (hasImageJGaussian) {
                            header += "ImageJ guassian fit x coord.,ImageJ gaussian fit y coord.,ImageJ gaussian fit peak x coord.,ImageJ gaussian fit peak y coord.";
                        }
                        pw.println(header);
                        //print to file
                        int maxpts = Util.getMax(oslr.getAmgPoints().getItemCount(), oslr.getDiffPoints().getItemCount(), oslr.getIjgPoints().getItemCount());
                        XYSeries diffLine = oslr.getDiffPoints();
                        XYSeries amg = oslr.getAmgPoints();
                        XYSeries ijg = oslr.getIjgPoints();
                        for (int i = 0; i < maxpts; i++) {
                            if (i == 0) {
                                if (hasImageJGaussian) {
                                    pw.println(
                                            diffLine.getX(i).intValue() + "," + diffLine.getY(i).intValue() + ","
                                            + amg.getX(i).intValue() + "," + amg.getY(i).intValue() + ","
                                            + oslr.getAmgmPoint().getX(0).intValue() + "," + oslr.getAmgmPoint().getY(i).intValue() + ","
                                            + ijg.getX(i).intValue() + "," + ijg.getY(i).intValue() + ","
                                            + oslr.getIjgmPoint().getX(i).intValue() + "," + oslr.getIjgmPoint().getY(i).intValue()
                                    );
                                } else {
                                    pw.println(
                                            diffLine.getX(i).intValue() + "," + diffLine.getY(i).intValue() + ","
                                            + amg.getX(i).intValue() + "," + amg.getY(i).intValue() + ","
                                            + oslr.getAmgmPoint().getX(0).intValue() + "," + oslr.getAmgmPoint().getY(i).intValue()
                                    );
                                }
                            } else {
                                String diff = (diffLine.getItemCount() > i) ? diffLine.getX(i).intValue() + "," + diffLine.getY(i).intValue() + "," : ",,";
                                String amgs = (amg.getItemCount() > i) ? amg.getX(i).intValue() + "," + amg.getY(i).intValue() + "," : ",,";
                                String ijgs = (ijg.getItemCount() > i) ? ijg.getX(i).intValue() + "," + ijg.getY(i).intValue() + "," : ",,";
                                if (hasImageJGaussian) {
                                    pw.println(diff + amgs + ",," + ijgs);
                                } else {
                                    pw.println(diff + amgs);
                                }
                            }
                        }
                    }
                    OCTSelection sel = selections.get(0);
                    //name files with versioning to ensure that all files are named with same version
                    //and files from ealier analysis exports aren't overwritten
                    lrpFile = new File(outputDir,
                            fileNameStub + "_os_length_lrp_" + fdate + ".csv");
                    lrpPeaksFile = new File(outputDir,
                            fileNameStub + "_os_length_lrp_peaks_" + fdate + ".csv");
                    //export CSV of LRP information
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(lrpFile)))) {
                        //grab lrp reflectivity
                        List<XYDataItem> lrp = (List<XYDataItem>) sel
                                .getLrpSeriesFromOCT(octMngr.getOctImage())
                                .getItems();
                        //print to file
                        lrp.forEach(rp -> {
                            pw.println(rp.getY().intValue() + "," + rp.getX().intValue());
                        });
                    }
                    //export CSV of LRP Peaks information
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(lrpPeaksFile)))) {
                        //grab lrp reflectivity peak values
                        List<XYDataItem> lrp = (List<XYDataItem>) OCTSelection.findMaximums(sel.getLrpSeriesFromOCT(octMngr.getOctImage()), "").getItems();
                        //print to file
                        lrp.forEach(rp -> {
                            pw.println(rp.getY().intValue() + "," + rp.getX().intValue());
                        });
                    }
                    //export stats for the analysis
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(statsFile)))) {
                        //print position of selection
                        pw.println("Max OS Length (Microns)," + df.format(octMngr.pixels2MicronsInY(oslr.getMaxDiff())));
                        pw.println("Max OS Length gaussian fit method," + oslr.getMaxDiffSource());
                        pw.println("Max OS Length distance from left edge of OCT (Pixels)," + sel.getXPositionOnOct());
                        pw.println("Max OS Length distance from left edge of OCT (Microns)," + sel.getXPositionOnOct() * octMngr.getXscale());
                        pw.println("LRP width (Pixels)," + sel.getWidth());
                        pw.println("LRP width (Microns)," + sel.getWidth() * octMngr.getXscale());
                        pw.println("Analysis type," + octMngr.getAnalysisMode());
                        pw.println("LRP file name," + lrpFile.getName());
                        pw.println("LRP Peaks file name," + lrpPeaksFile.getName());
                        pw.println("OCT X scale," + octMngr.getXscale());
                        pw.println("OCT Y scale," + octMngr.getYscale());
                    }
                    break;
                case FIND_FOVEA:
                case SINGLE:
                    sel = selections.get(0);
                    //name files with versioning to ensure that all files are named with same version
                    //and files from ealier analysis exports aren't overwritten
                    lrpFile = new File(outputDir,
                            fileNameStub + "_" + sel.getSelectionName().toLowerCase() + "_lrp_" + fdate + ".csv");
                    lrpPeaksFile = new File(outputDir,
                            fileNameStub + "_" + sel.getSelectionName().toLowerCase() + "_lrp_peaks_" + fdate + ".csv");
                    //export CSV of LRP information
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(lrpFile)))) {
                        //grab lrp reflectivity
                        List<XYDataItem> lrp = (List<XYDataItem>) sel
                                .getLrpSeriesFromOCT(octMngr.getOctImage())
                                .getItems();
                        //print to file
                        lrp.forEach(rp -> {
                            pw.println(rp.getY().intValue() + "," + rp.getX().intValue());
                        });
                    }
                    //export CSV of LRP Peaks information
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(lrpPeaksFile)))) {
                        //grab lrp reflectivity peak values
                        List<XYDataItem> lrp = (List<XYDataItem>) OCTSelection.findMaximums(sel.getLrpSeriesFromOCT(octMngr.getOctImage()), "").getItems();
                        //print to file
                        lrp.forEach(rp -> {
                            pw.println(rp.getY().intValue() + "," + rp.getX().intValue());
                        });
                    }
                    //export stats for the analysis
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(statsFile)))) {
                        //print position of selection
                        pw.println("LRP distance from left edge of OCT (Pixels)," + sel.getXPositionOnOct());
                        pw.println("LRP distance from left edge of OCT (Microns)," + sel.getXPositionOnOct() * octMngr.getXscale());
                        pw.println("LRP width (Pixels)," + sel.getWidth());
                        pw.println("LRP width (Microns)," + sel.getWidth() * octMngr.getXscale());
                        pw.println("Analysis type," + octMngr.getAnalysisMode());
                        pw.println("LRP file name," + lrpFile.getName());
                        pw.println("LRP Peaks file name," + lrpPeaksFile.getName());
                        pw.println("OCT X scale," + octMngr.getXscale());
                        pw.println("OCT Y scale," + octMngr.getYscale());
                    }
                    break;
                case EQUIDISTANT:
                case EZ:
                case MIRROR:
                    ArrayList<String> fnameList = new ArrayList<>(selections.size());
                    ArrayList<String> fpnameList = new ArrayList<>(selections.size());
                    for (OCTSelection selection : selections) {
                        //export CSV of LRP information for each selection
                        lrpFile = new File(outputDir,
                                fileNameStub + "_" + selection.getSelectionName().toLowerCase().replaceAll(" ", "_") + "_lrp_v" + fdate + ".csv");
                        fnameList.add(lrpFile.getName());
                        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(lrpFile)))) {
                            //grab lrp reflectivity
                            List<XYDataItem> lrp = (List<XYDataItem>) selection
                                    .getLrpSeriesFromOCT(octMngr.getOctImage())
                                    .getItems();
                            //print to file
                            lrp.forEach(rp -> {
                                pw.println(rp.getY().intValue() + "," + rp.getX().intValue());
                            });
                        }
                        //export CSV of LRP Peaks information
                        lrpPeaksFile = new File(outputDir,
                                fileNameStub + "_" + selection.getSelectionName().toLowerCase().replaceAll(" ", "_") + "_lrp_peaks_v" + fdate + ".csv");
                        fpnameList.add(lrpPeaksFile.getName());
                        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(lrpPeaksFile)))) {
                            //grab lrp reflectivity peak values
                            List<XYDataItem> lrp = (List<XYDataItem>) OCTSelection.findMaximums(selection.getLrpSeriesFromOCT(octMngr.getOctImage()), "").getItems();
                            //print to file
                            lrp.forEach(rp -> {
                                pw.println(rp.getY().intValue() + "," + rp.getX().intValue());
                            });
                        }
                    }
                    //export stats for the analysis
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(statsFile)))) {
                        //print position of selection
                        selections.forEach((psel) -> {
                            pw.println(psel.getSelectionName() + " LRP distance from fovea (Pixels)," + Math.abs(psel.getXPositionOnOct() - octMngr.getFoveaCenterXPosition()));
                            pw.println(psel.getSelectionName() + " LRP distance from fovea (Microns)," + Math.abs(psel.getXPositionOnOct() - octMngr.getFoveaCenterXPosition()) * octMngr.getXscale());
                            pw.println(psel.getSelectionName() + " LRP width (Pixels)," + psel.getWidth());
                            pw.println(psel.getSelectionName() + " LRP width (Microns)," + psel.getWidth() * octMngr.getXscale());
                        });
                        pw.println("Analysis type," + octMngr.getAnalysisMode());
                        pw.println("LRP file names," + fnameList.stream().collect(Collectors.joining(",")));
                        pw.println("LRP Peaks file names," + fpnameList.stream().collect(Collectors.joining(",")));
                        pw.println("OCT X scale," + octMngr.getXscale());
                        pw.println("OCT Y scale," + octMngr.getYscale());
                    }
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    private static BufferedImage getOCTScreenShot(OCTImagePanel component) {
        OCT oct = OCTAnalysisManager.getInstance().getOct();
        BufferedImage image = new BufferedImage(
                component.getWidth(),
                component.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        // call the Component's paint method, using
        // the Graphics object of the image.
        component.paint(image.getGraphics()); // alternately use .printAll(..)
        return image.getSubimage(component.getImageOffsetX(), component.getImageOffsetY(), oct.getImageWidth(), oct.getImageHeight());
    }
}
