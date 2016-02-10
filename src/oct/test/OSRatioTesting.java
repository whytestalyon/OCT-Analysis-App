/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.test;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

            if(!oct.getName().contains("JC_0628")){
                continue;
            }
            BufferedImage tiffBI = TiffReader.readTiffImage(oct);
            System.out.println("Getting segs...");
            List<Line> segLines = Segmentation.getSegmentationLines(tiffBI, true, 0.5D, 1.8D, 5D);
            System.out.println("Sorting segs...");
            Collections.sort(segLines, (Line l1, Line l2) -> {
                return Integer.compare(l2.size(), l1.size());
            });

            LinkedList<List<Line>> overlapingLines = new LinkedList<>();
            for (Line curLine : segLines.subList(0, 20)) {
                boolean foundGroup = false;
                //see if line can bee added to already existing group
                OUT:
                for (List<Line> llist : overlapingLines) {
                    for (Line line : llist) {
                        long overlappingPointsCount = Stream.concat(line.stream(), curLine.stream())
                                .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                                .values()
                                .stream()
                                .filter(cnt -> cnt > 1)
                                .count();
                        if (overlappingPointsCount > 10) {
                            llist.add(curLine);
                            foundGroup = true;
                            break OUT;
                        }
                    }
                }

                //if not part of already existing group start group with line
                if (!foundGroup) {
                    LinkedList<Line> nl = new LinkedList<>();
                    nl.add(curLine);
                    overlapingLines.add(nl);
                }
            }

            //merge lines contained within each group into single lines
            List<Line> mergedSegs = overlapingLines.stream()
                    .map(llist -> Util.mergeLines(llist.toArray(new Line[llist.size()])))
                    .collect(Collectors.toList());

            //group lines that are close to each other (50+ points that are 3 or less px apart) and merge them using weighted average
            overlapingLines = new LinkedList<>();
            for (Line curLine : mergedSegs) {
                boolean foundGroup = false;
                //see if line can bee added to already existing group
                OUT:
                for (List<Line> llist : overlapingLines) {
                    for (Line line : llist) {
                        long overlappingPointsCount = Stream.concat(line.stream(), curLine.stream())
                                .collect(Collectors.groupingBy(p -> p.x))
                                .values()
                                .stream()
                                .filter(plist -> plist.size() > 1)
                                .filter(plist -> Math.abs(plist.get(0).y - plist.get(1).y) <= 3)
                                .count();
                        if (overlappingPointsCount > 50) {
                            llist.add(curLine);
                            foundGroup = true;
                            break OUT;
                        }
                    }
                }

                //if not part of already existing group start group with line
                if (!foundGroup) {
                    LinkedList<Line> nl = new LinkedList<>();
                    nl.add(curLine);
                    overlapingLines.add(nl);
                }
            }

            //merge lines contained within each group into single line
            mergedSegs = overlapingLines.stream()
                    .map(llist -> Util.mergeLines(llist.toArray(new Line[llist.size()])))
                    .collect(Collectors.toList());

            Util.graphLines(mergedSegs, true, tiffBI.getHeight(), oct.getName() + " segs");
        }
    }

}
