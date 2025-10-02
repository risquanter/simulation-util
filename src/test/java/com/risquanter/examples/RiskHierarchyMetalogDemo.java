package com.risquanter.examples;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.random.Well19937c;

import com.risquanter.simulation.Metalog;
import com.risquanter.simulation.QPFitter;

import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

/**
 * Demonstrates building a three-level risk hierarchy [AB] and [[AB]C]
 * using zero-inflated lognormal leaf risks, fitted Metalog quantile
 * functions, and Monte Carlo via F-Inverse sampling.
 */
public class RiskHierarchyMetalogDemo {
    // fixed p-levels for fitting leaf Metalogs
    private static final double[] FIT_P = {
        0.001, 0.02, 0.10, 0.25, 0.50, 0.75, 0.90, 0.98, 0.999
    };

    public static void main(String[] args) {
        //
        // 1) User inputs for A, B, C: {lossLB, lossUB, pLoss}
        //
        double[][] inputs = {
            {100, 200, 0.10},   // Risk A
            { 50, 150, 0.20},   // Risk B
            { 80, 300, 0.05}    // Risk C
        };
        int numRisks = inputs.length;

        // seed for reproducibility
        long seed = 42L;
        RandomGenerator hdrRng = new Well19937c(seed);
        Random      uniRng = new Random(seed);

        // storage for leaf Metalogs
        Metalog[] leafMetalog = new Metalog[numRisks];

        //
        // 2) Fit a lower-bounded Metalog to each leaf zero-inflated lognormal
        //
        for (int r = 0; r < numRisks; r++) {
            double lossLB  = inputs[r][0];
            double lossUB  = inputs[r][1];
            double pLoss   = inputs[r][2];
            double pNoLoss = 1.0 - pLoss;

            // convert [lossLB,lossUB] @80% into (mu,sigma)
            double meanLog = (Math.log(lossLB) + Math.log(lossUB)) / 2.0;
            double varLog  = Math.pow((Math.log(lossUB) - Math.log(lossLB)) / 3.29, 2);
            double sigma   = Math.sqrt(varLog);
            double mu      = meanLog;

            // analytic mixture inverse‐CDF
            LogNormalDistribution lnDist = new LogNormalDistribution(hdrRng, mu, sigma);

            // build quantile anchors
            List<Double> pAnchors = new ArrayList<>();
            List<Double> xAnchors = new ArrayList<>();
            for (double p : FIT_P) {
                pAnchors.add(p);
                if (p <= pNoLoss) {
                    xAnchors.add(0.0);
                } else {
                    double tailP = (p - pNoLoss) / pLoss;
                    xAnchors.add(lnDist.inverseCumulativeProbability(tailP));
                }
            }

            // fit the Metalog with lower bound 0
            double[] pArr = pAnchors.stream().mapToDouble(Double::doubleValue).toArray();
            double[] xArr = xAnchors.stream().mapToDouble(Double::doubleValue).toArray();
            leafMetalog[r] = QPFitter
                .with(pArr, xArr, pArr.length)
                .lower(0.0)
                .fit();
        }

        //
        // 3) Monte Carlo: simulate A, B, C → AB → ABC over N years
        //
        int sims = 10_000;
        double[] abSamples  = new double[sims];
        double[] abcSamples = new double[sims];

        for (int i = 0; i < sims; i++) {
            // sample each leaf by F-Inverse on its Metalog
            double sumAB = 0.0;
            for (int r = 0; r < 2; r++) {
                double u     = uniRng.nextDouble();
                double lossR = leafMetalog[r].quantile(u);
                sumAB += lossR;
            }
            double uC     = uniRng.nextDouble();
            double lossC  = leafMetalog[2].quantile(uC);
            double sumABC = sumAB + lossC;

            abSamples[i]  = sumAB;
            abcSamples[i] = sumABC;
        }

        //
        // 4) Compute empirical CDF & exceedance curves
        //
        Arrays.sort(abSamples);
        Arrays.sort(abcSamples);

        // print JSON object with two series: "AB" and "ABC"
        System.out.println("{");

        printSeries("AB",  abSamples,  sims);
        System.out.println(",");
        printSeries("ABC", abcSamples, sims);

        System.out.println("}");
    }

    /**
     * Prints a JSON array of { loss, cdf, exceedance } for one node.
     */
    private static void printSeries(String name, double[] samples, int sims) {
        System.out.println("  \"" + name + "\": [");
        for (int i = 0; i < sims; i++) {
            // empirical CDF at sample i
            double cdf        = (double)(i + 1) / sims;
            double exceedance = 1.0 - cdf;
            System.out.printf(
                "    {\"loss\": %.4f, \"cdf\": %.4f, \"exceedance\": %.4f}%s%n",
                samples[i],
                cdf,
                exceedance,
                (i < sims - 1 ? "," : "")
            );
        }
        System.out.print("  ]");
    }
}
