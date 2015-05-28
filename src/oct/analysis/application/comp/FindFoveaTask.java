/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oct.analysis.application.comp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.SwingWorker;
import oct.analysis.application.dat.LinePoint;
import oct.analysis.application.dat.Diff;
import oct.analysis.application.dat.OCTAnalysisManager;
import oct.util.Segmentation;
import oct.util.Util;
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
public class FindFoveaTask extends SwingWorker<List<Integer>, Integer> {

    @Override
    protected List<Integer> doInBackground() throws Exception {
        OCTAnalysisManager octMngr = OCTAnalysisManager.getInstance();
        //find the fovea since it hasn't been found/defined yet
        UnivariateInterpolator interpolator = new LoessInterpolator(0.1, 0);
        Segmentation octSeg = octMngr.getSegmentation(new SharpenOperation(15, 0.6F));
        double[][] ilmSeg = Util.getXYArraysFromPoints(new ArrayList<>(octSeg.getSegment(Segmentation.ILM_SEGMENT)));
        UnivariateFunction ilmInterp = interpolator.interpolate(ilmSeg[0], ilmSeg[1]);
        double[][] brmSeg = Util.getXYArraysFromPoints(new ArrayList<>(octSeg.getSegment(Segmentation.BrM_SEGMENT)));
        UnivariateFunction brmInterp = interpolator.interpolate(brmSeg[0], brmSeg[1]);
        double[][] diffLine = Util.getXYArraysFromLinePoints(octMngr.findAbsoluteDiff(brmInterp, ilmInterp, 0, octMngr.getOct().getLinearOctImage().getWidth() - 1));
        UnivariateFunction diffInerp = interpolator.interpolate(diffLine[0], diffLine[1]);
        FiniteDifferencesDifferentiator differ = new FiniteDifferencesDifferentiator(4, 0.25);
        UnivariateDifferentiableFunction difFunc = differ.differentiate(diffInerp);
        setProgress(20);
        /*
         * collect the first derivative at each pixel in the image
         */
        int numFreeVariablesInFunction = 1;
        int order = 1;
        DerivativeStructure xd;
        DerivativeStructure yd;
        ArrayList<LinePoint> firstDeriv = new ArrayList<>(octMngr.getOct().getLinearOctImage().getWidth() - 1);
        IntStream.range(0, octMngr.getOct().getLinearOctImage().getWidth() - 1).forEach((int i) -> {
            firstDeriv.add(new LinePoint(0, 0));
        });
        for (int xRealValue = 1; xRealValue <= octMngr.getOct().getLinearOctImage().getWidth() - 2; xRealValue++) {
            xd = new DerivativeStructure(numFreeVariablesInFunction, order, 0, xRealValue);
            yd = difFunc.value(xd);
            firstDeriv.set(xRealValue, new LinePoint(xRealValue, yd.getPartialDerivative(1)));
        }
        setProgress(40);
        List<LinePoint> peaks = Util.findMaxAndMins(firstDeriv);
        LinePoint prevPeak = null;
        LinkedList<Diff> diffs = new LinkedList<>();
        for (LinePoint curPeak : peaks) {
            if (prevPeak != null) {
                diffs.add(new Diff(prevPeak, curPeak));
            }
            prevPeak = curPeak;
        }
        setProgress(60);
        Diff maxDiff = diffs.stream().max(Comparator.comparingDouble((Diff diff) -> diff.getYDiff())).get();
        double sign = Math.signum(maxDiff.getLinePoint1().getY());
        int signChangeXPos = maxDiff.getLinePoint1().getX() + 1;
        setProgress(80);
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
                while (sign == Math.signum(firstDeriv.get(signChangeXPos).getY())) {
                    signChangeXPos++;
                }
                //add other possible fovea site to list
                foveaCenterXPosition = (Math.abs(firstDeriv.get(signChangeXPos).getY()) < Math.abs(firstDeriv.get(signChangeXPos - 1).getY())) ? signChangeXPos : signChangeXPos - 1;
                positionList.add(foveaCenterXPosition);
            }
            prevPeak = curPeak;
        }
        return positionList;
    }

}
