package com.risquanter.examples;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.random.RandomGenerator;
import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RiskHierarchyMetalogPGrid {
    // how many p-steps to print on [0,1]
    private static final int GRID_STEPS = 100;

    public static void main(String[] args) {
        // --- 1) User inputs: [lossLB, lossUB, pLoss] for each leaf
        double[][] inputs = {
            {100, 200, 0.10},  // A
            { 50, 150, 0.20},  // B
            { 80, 300, 0.05}   // C
        };

        // one fixed seed for everything
        long seed = 42L;
        RandomGenerator hdrRng = new Well19937c(seed);
        Random      uniRng = new Random(seed);

        // --- 2) Fit *only* the continuous tail metalog at each leaf
        Metalog[] leafTailMetalog = new Metalog[inputs.length];
        for (int r = 0; r < inputs.length; r++) {
            double lb    = inputs[r][0];
            double ub    = inputs[r][1];
            double pLoss = inputs[r][2];
            double pNo   = 1.0 - pLoss;

            // pick 9 points in the (0,1) → tail  
            // we only anchor the nonzero portion at normalized probabilities
            double[] tailP = { 0.001,0.02,0.10,0.25,0.50,0.75,0.90,0.98,0.999 };
            List<Double> pAnch = new ArrayList<>();
            List<Double> xAnch = new ArrayList<>();

            // build a LogNormal that covers [lb,ub] at central 80%
            double meanLog = (Math.log(lb) + Math.log(ub))/2.0;
            double varLog  = Math.pow((Math.log(ub)-Math.log(lb))/3.29, 2);
            LogNormalDistribution lnDist = 
                new LogNormalDistribution(hdrRng, meanLog, Math.sqrt(varLog));

            for (double tp : tailP) {
                pAnch.add(tp);
                xAnch.add( lnDist.inverseCumulativeProbability(tp) );
            }

            double[] pArr = pAnch.stream().mapToDouble(Double::doubleValue).toArray();
            double[] xArr = xAnch.stream().mapToDouble(Double::doubleValue).toArray();

            // fit a pure‐tail metalog
            leafTailMetalog[r] = QPFitter
                .with(pArr, xArr, pArr.length)
                .fit();
        }

        // --- 3) Monte Carlo: build AB and ABC by coin-flip + tail‐sample
        int sims = 10_000;
        double[] ab  = new double[sims];
        double[] abc = new double[sims];

        for (int i = 0; i < sims; i++) {
            // A+B
            double sumAB = 0.0;
            for (int r = 0; r < 2; r++) {
                double pLoss = inputs[r][2];
                double flip  = uniRng.nextDouble();
                if (flip < pLoss) {
                    // sample tail
                    double uTail = uniRng.nextDouble();
                    sumAB += leafTailMetalog[r].quantile(uTail);
                }
                // else zero
            }
            ab[i] = sumAB;

            // then add C
            double sumABC = sumAB;
            {
                double pLoss = inputs[2][2];
                double flip  = uniRng.nextDouble();
                if (flip < pLoss) {
                    double uTail = uniRng.nextDouble();
                    sumABC += leafTailMetalog[2].quantile(uTail);
                }
            }
            abc[i] = sumABC;
        }

        // sort once
        java.util.Arrays.sort(ab);
        java.util.Arrays.sort(abc);

        // --- 4) Print full loss-exceedance curve on p-grid
        System.out.println("[");
        for (int i = 1; i <= GRID_STEPS; i++) {
            double p = i/(double)GRID_STEPS;
            int idx = (int)Math.floor(p * sims);
            if (idx >= sims) idx = sims-1;

            double qAB  = ab[idx];
            double qABC = abc[idx];
            String comma = (i < GRID_STEPS ? "," : "");

            System.out.printf(
              "  {\"p\": %.3f, \"AB\": %.4f, \"ABC\": %.4f}%n",
               p, qAB, qABC, comma
            );
        }
        System.out.println("]");
    }
}
