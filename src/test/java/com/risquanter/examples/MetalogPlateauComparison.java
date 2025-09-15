package com.risquanter.examples;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

import java.util.ArrayList;
import java.util.List;

public class MetalogPlateauComparison {
    public static void main(String[] args) {
        //
        // 1) Set up the zero‐inflated lognormal parameters
        //
        double lossLB  = 100;   // 80% of lognormal lies in [100,200]
        double lossUB  = 200;
        double meanLog = (Math.log(lossLB) + Math.log(lossUB)) / 2.0;
        double varLog  = Math.pow((Math.log(lossUB) - Math.log(lossLB)) / 3.29, 2);
        double sigma   = Math.sqrt(varLog);
        double mu      = meanLog;
        double pLoss   = 0.1;     // 10% chance of a loss
        double pNoLoss = 1.0 - pLoss;

        // underlying lognormal for the tail
        LogNormalDistribution lnDist = new LogNormalDistribution(mu, sigma);

        //
        // 2) fixed p‐levels where we’ll compare quantiles
        //
        double[] fixedP = {
            0.001, 0.020, 0.100, 0.250,
            0.500, 0.750, 0.900, 0.980, 0.999
        };

        int K = fixedP.length;

        //
        // 3) compute the analytic quantile Q_ana(p)
        //
        double[] xAna = new double[K];
        for (int i = 0; i < K; i++) {
            double p = fixedP[i];
            if (p <= pNoLoss) {
                xAna[i] = 0.0;
            } else {
                // rescale into the lognormal tail
                double scaledP = (p - pNoLoss) / pLoss;
                xAna[i] = lnDist.inverseCumulativeProbability(scaledP);
            }
        }

        //
        // 4) build anchor points for the Metalog:
        //    a) several (p,0) anchors to lock in the plateau
        //    b) tail anchors at the analytic quantiles for p>pNoLoss
        //
        int numZeroAnchors = 5;
        List<Double> pAnchors = new ArrayList<>();
        List<Double> xAnchors = new ArrayList<>();

        // a) plateau anchors: evenly spaced up to pNoLoss
        for (int i = 1; i <= numZeroAnchors; i++) {
            double p = pNoLoss * i / numZeroAnchors;
            pAnchors.add(p);
            xAnchors.add(0.0);
        }

        // b) tail anchors: use analytic Q for p>pNoLoss
        for (int i = 0; i < K; i++) {
            if (fixedP[i] > pNoLoss) {
                pAnchors.add(fixedP[i]);
                xAnchors.add(xAna[i]);
            }
        }

        // convert lists to primitive arrays
        double[] pArr = pAnchors.stream().mapToDouble(Double::doubleValue).toArray();
        double[] xArr = xAnchors.stream().mapToDouble(Double::doubleValue).toArray();

        //
        // 5) fit the single Metalog (lower bound 0)
        //
        Metalog metalog = QPFitter
            .with(pArr, xArr, pArr.length)
            .lower(0.0)
            .fit();

        //
        // 6) compute the Metalog’s quantile at each fixedP
        //
        double[] xFit = new double[K];
        for (int i = 0; i < K; i++) {
            xFit[i] = metalog.quantile(fixedP[i]);
        }

        //
        // 7) emit JSON for Vega-Lite:
        //    [{ "p":…, "analytic":…, "fitted":… }, …]
        //
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < K; i++) {
            sb.append(String.format(
                "  {\"p\": %.3f, \"analytic\": %.4f, \"fitted\": %.4f}%s%n",
                fixedP[i], xAna[i], xFit[i], (i < K - 1 ? "," : "")
            ));
        }
        sb.append("]");
        System.out.println(sb);
    }
}
