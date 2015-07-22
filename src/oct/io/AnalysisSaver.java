/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.io;

import com.google.gson.Gson;
import java.awt.Component;
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
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTImagePanel;
import oct.analysis.application.OCTSelection;
import oct.analysis.application.dat.OCT;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.analysis.application.dat.OCTMode;
import oct.analysis.application.dat.SelectionLRPManager;
import oct.util.Util;
import org.jfree.data.xy.XYDataItem;

/**
 *
 * @author Brandon
 */
public class AnalysisSaver {

    private static final OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();
    private static final SelectionLRPManager selMngr = SelectionLRPManager.getInstance();
    private static final DecimalFormat df = new DecimalFormat("#.00");

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

    public static void exportAnalysisData(File outputDir) throws IOException {
        //grab selection(s)
        List<OCTSelection> selections = selMngr.getSelections();
        if (selections.isEmpty()) {
            return;
        }
        //get OCT file name without extension
        String fileNameStub = octMngr.getOct().getFileName().replaceFirst("\\.[^\\.]+", "");
        //save capture of OCT as displayed to user
        int fnameCntr = 0;
        File screenFile;
        do {
            fnameCntr++;
            screenFile = new File(outputDir,
                    fileNameStub + "_" + octMngr.getAnalysisMode().toString().toLowerCase() + "_ora_v" + fnameCntr + ".png");
        } while (screenFile.exists());
        ImageIO.write(getOCTScreenShot(octMngr.getImgPanel()), "png", screenFile);
        //define output files
        File statsFile;
        fnameCntr = 0;
        do {
            fnameCntr++;
            statsFile = new File(outputDir,
                    fileNameStub + "_" + octMngr.getAnalysisMode().toString().toLowerCase() + "_stats_v" + fnameCntr + ".csv");
        } while (statsFile.exists());
        File lrpFile;
        File lrpPeaksFile;
        /*
         Based on the type of analysis that was being performed export the data
         accordingly
         */
        switch (octMngr.getAnalysisMode()) {
            case FIND_FOVEA:
            case SINGLE:
                //export CSV of LRP information
                OCTSelection sel = selections.get(0);
                fnameCntr = 0;
                do {
                    fnameCntr++;
                    lrpFile = new File(outputDir,
                            fileNameStub + "_" + sel.getSelectionName().toLowerCase() + "_lrp_v" + fnameCntr + ".csv");
                } while (lrpFile.exists());
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
                fnameCntr = 0;
                do {
                    fnameCntr++;
                    lrpPeaksFile = new File(outputDir,
                            fileNameStub + "_" + sel.getSelectionName().toLowerCase() + "_lrp_peaks_v" + fnameCntr + ".csv");
                } while (lrpPeaksFile.exists());
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
                    pw.println("LRP distance from left edge of OCT (Microns)," + sel.getXPositionOnOct() * octMngr.getScale());
                    pw.println("LRP width (Pixels)," + sel.getWidth());
                    pw.println("LRP width (Microns)," + sel.getWidth() * octMngr.getScale());
                    pw.println("Analysis type," + octMngr.getAnalysisMode());
                    pw.println("LRP file names," + lrpFile.getName());
                    pw.println("LRP Peaks file names," + lrpPeaksFile.getName());
                }
                break;
            case EQUIDISTANT:
            case EZ:
            case MIRROR:
                ArrayList<String> fnameList = new ArrayList<>(selections.size());
                ArrayList<String> fpnameList = new ArrayList<>(selections.size());
                for (OCTSelection selection : selections) {
                    //export CSV of LRP information for each selection
                    fnameCntr = 0;
                    do {
                        fnameCntr++;
                        lrpFile = new File(outputDir,
                                fileNameStub + "_" + selection.getSelectionName().toLowerCase() + "_lrp_v" + fnameCntr + ".csv");
                    } while (lrpFile.exists());
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
                    fnameCntr = 0;
                    do {
                        fnameCntr++;
                        lrpPeaksFile = new File(outputDir,
                                fileNameStub + "_" + selection.getSelectionName().toLowerCase() + "_lrp_peaks_v" + fnameCntr + ".csv");
                    } while (lrpPeaksFile.exists());
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
                        pw.println(psel.getSelectionName() + " LRP distance from fovea (Microns)," + Math.abs(psel.getXPositionOnOct() - octMngr.getFoveaCenterXPosition()) * octMngr.getScale());
                        pw.println(psel.getSelectionName() + " LRP width (Pixels)," + psel.getWidth());
                        pw.println(psel.getSelectionName() + " LRP width (Microns)," + psel.getWidth() * octMngr.getScale());
                    });
                    pw.println("Analysis type," + octMngr.getAnalysisMode());
                    pw.println("LRP file names," + fnameList.stream().collect(Collectors.joining(",")));
                    pw.println("LRP Peaks file names," + fpnameList.stream().collect(Collectors.joining(",")));
                }
                break;
            default:
                break;
        }

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
