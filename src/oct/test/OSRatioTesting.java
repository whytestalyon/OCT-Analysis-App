/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import oct.io.TiffReader;
import oct.util.Line;
import oct.util.Segmentation;
import oct.util.Util;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OSRatioTesting {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        File[] octs = new File("D:\\Documents\\ContractWork\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\OS_Ratio\\Cropped").listFiles();

        for (File oct : octs) {
            BufferedImage tiffBI = TiffReader.readTiffImage(oct);
            List<Line> segLines = Segmentation.getSegmentationLines(tiffBI, true, 0.5D, 1.8D, 5D);
            Collections.sort(segLines, (Line l1, Line l2) -> {
                return Integer.compare(l2.size(), l1.size());
            });
            
            List<Line> bestSegs = segLines.subList(0, 20);
            
            
            Util.graphLines(segLines.subList(0, 20), true, tiffBI.getHeight(), oct.getName() + " segs");
        }
    }

}
