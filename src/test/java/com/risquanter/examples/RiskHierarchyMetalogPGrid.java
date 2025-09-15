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
    // p-grid for plotting: 0%, 1%, …, 100%
    private static final int GRID_STEPS = 100;

    public static void main(String[] args) {
        // --- 1) User inputs A, B, C as {lossLB, lossUB, pLoss}
        double[][] inputs = {
            {100, 200, 0.10},  // A
            { 50, 150, 0.20},  // B
            { 80, 300, 0.05}   // C
        };

        // seeds for reproducibility
        long seed = 42L;
        RandomGenerator hdrRng = new Well19937c(seed);
        Random      uniRng = new Random(seed);

        // --- 2) Fit a zero-inflated Metalog for each leaf risk
        Metalog[] leafMetalog = new Metalog[inputs.length];
        // common p-levels for fitting
        double[] fitP = {0.001,0.02,0.10,0.25,0.50,0.75,0.90,0.98,0.999};

        for (int r = 0; r < inputs.length; r++) {
            double lossLB = inputs[r][0];
            double lossUB = inputs[r][1];
            double pLoss  = inputs[r][2];
            double pNo    = 1.0 - pLoss;

            // map [lossLB,lossUB] @80% to lognormal mu/sigma
            double meanLog = (Math.log(lossLB) + Math.log(lossUB)) / 2;
            double varLog  = Math.pow((Math.log(lossUB) - Math.log(lossLB)) / 3.29, 2);
            double sigma   = Math.sqrt(varLog);
            double mu      = meanLog;

            // analytic tail CDF
            LogNormalDistribution lnDist = 
                new LogNormalDistribution(mu, sigma);

            // build quantile anchors at fitP
            List<Double> pAnch = new ArrayList<>();
            List<Double> xAnch = new ArrayList<>();
            for (double p : fitP) {
                pAnch.add(p);
                if (p <= pNo) {
                    xAnch.add(0.0);
                } else {
                    double tailP = (p - pNo) / pLoss;
                    xAnch.add(lnDist.inverseCumulativeProbability(tailP));
                }
            }

            double[] pArr = pAnch.stream().mapToDouble(Double::doubleValue).toArray();
            double[] xArr = xAnch.stream().mapToDouble(Double::doubleValue).toArray();

            leafMetalog[r] = QPFitter
                .with(pArr, xArr, pArr.length)
                .lower(0.0)
                .fit();
        }

        // --- 3) Monte Carlo generate [AB] & [[AB]C] samples
        int sims = 10_000;
        double[] ab = new double[sims], abc = new double[sims];

        for (int i = 0; i < sims; i++) {
            // sample A + B
            double sumAB = 0;
            for (int r = 0; r < 2; r++) {
                double u = uniRng.nextDouble();
                sumAB += leafMetalog[r].quantile(u);
            }
            // sample C
            double uC = uniRng.nextDouble();
            double sumABC = sumAB + leafMetalog[2].quantile(uC);

            ab[i]  = sumAB;
            abc[i] = sumABC;
        }

        // sort once for quantile lookup
        java.util.Arrays.sort(ab);
        java.util.Arrays.sort(abc);

        // --- 4) Print JSON on p-grid instead of all 10k points
        System.out.println("[");
        for (int i = 0; i <= GRID_STEPS; i++) {
            double p = i / (double) GRID_STEPS;
            // clamp p=1.0 → index = sims-1
            int idx = (int) Math.floor(p * sims);
            if (idx >= sims) idx = sims - 1;

            double qAB  = ab[idx];
            double qABC = abc[idx];

            String comma = (i < GRID_STEPS ? "," : "");
            System.out.printf(
                "  {\"p\": %.3f, \"AB\": %.4f, \"ABC\": %.4f}%s%n",
                p, qAB, qABC, comma
            );
        }
        System.out.println("]");
    }
}
