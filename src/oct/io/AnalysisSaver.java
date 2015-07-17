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
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import oct.analysis.application.OCTAnalysisUI;
import oct.analysis.application.OCTSelection;
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
        ImageIO.write(getScreenShot(octMngr.getImgPanel()), "png", new File(outputDir, fileNameStub + "_ora.png"));
        /*
         Based on the type of analysis that was being performed export the data
         accordingly
         */
        switch (octMngr.getAnalysisMode()) {
            case FIND_FOVEA:
            case SINGLE:
                //export CSV of LRP information
                try (PrintWriter pw = new PrintWriter(
                        new BufferedWriter(
                                new FileWriter(
                                        new File(outputDir,
                                                fileNameStub + "_" + selections.get(0).getSelectionName().toLowerCase() + "_lrp.csv"))))) {
                                    //grab lrp reflectivity
                                    List<XYDataItem> lrp = (List<XYDataItem>) selections.get(0)
                                            .getLrpSeriesFromOCT(octMngr.getOctImage())
                                            .getItems();
                                    //print to file
                                    lrp.forEach(rp -> {
                                        pw.println(rp.getY().intValue() + "," + rp.getX().intValue());
                                    });
                                }
                //export stats for the analysis
                try (PrintWriter pw = new PrintWriter(
                        new BufferedWriter(
                                new FileWriter(
                                        new File(outputDir,
                                                fileNameStub + "_" + selections.get(0).getSelectionName().toLowerCase() + "_stats.csv"))))) {
                                    //print position of selection
                                    OCTSelection sel = selections.get(0);
                                    pw.println("Selection distance from left edge of OCT (Pixels)," + sel.getXPositionOnOct());
                                    pw.println("Selection distance from left edge of OCT (Microns)," + sel.getXPositionOnOct() * octMngr.getScale());
                                    pw.println("Selection width (Pixels)," + sel.getWidth());
                                    pw.println("Selection width (Microns)," + sel.getWidth() * octMngr.getScale());
                                    pw.println("Analysis type," + octMngr.getAnalysisMode());
                                }
            case EQUIDISTANT:
                break;
            case EZ:
                break;
            case MIRROR:
                break;
            default:
                break;
        }

    }

    private static BufferedImage getScreenShot(Component component) {

        BufferedImage image = new BufferedImage(
                component.getWidth(),
                component.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        // call the Component's paint method, using
        // the Graphics object of the image.
        component.paint(image.getGraphics()); // alternately use .printAll(..)
        return image;
    }
}
