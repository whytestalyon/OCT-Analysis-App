package oct.test;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Collection;

import chuiSegmentation.CSegImage;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import oct.io.TiffReader;

public class TestClass {

    public static void main(String[] args) throws IOException {
//        ImagePlus imp = NewImage.createShortImage("Lines", 400, 400, 1, NewImage.FILL_BLACK);
//        ImageProcessor ip = imp.getProcessor();
//        ip.setLineWidth(4);
//        ip.setColor(Color.WHITE);
//        Line line_30deg = new Line(10.0, 412.0, 446.4102, 112.0);
//        Line line_30deg2 = new Line(10.0, 312.0, 446.4102, 12.0);
//        Line line_m60deg = new Line(10.0, 10, 300.0, 446.4102);
//        Line[] rois = new Line[]{line_30deg, line_30deg2, line_m60deg};
//        for (Line roi : rois) {
//            ip.draw(roi);
//        }
//        GaussianBlur smoother = new GaussianBlur();
//        smoother.blurGaussian(ip, 2.0, 2.0, 1e-2);
//        imp.show();
        getSurfaceSegment(TiffReader.readTiffImage(new File("D:\\Documents\\IdependentContracting\\Carrol Lab\\LRP Analysis App\\Example Human OCTs\\DiseasedOCTs\\DH_10160_OD_L_7_0_02_529disp_fr66reg_fr47-83_AL24p11.tif")), 10).stream().forEach((seg) -> {
            System.out.println(seg.x + "\t" + seg.y);
        });
    }

    public static Collection<Point> getSurfaceSegment(BufferedImage image, int distanceBetweenPoints) {
		// Only tested with BufferedImage.TYPE_BYPE_GRAY. 
        // It might work with other types but no promises. 
        CSegImage segImg = new CSegImage(image);
        segImg.getFlatImage(false);
        return segImg.getSegments()
                .getCurve(0, distanceBetweenPoints)
                .getPointCollection();
    }
}
