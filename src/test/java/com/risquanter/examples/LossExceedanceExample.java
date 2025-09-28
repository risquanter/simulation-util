package com.risquanter.examples;

import java.util.Arrays;
import java.util.Random;
import java.util.StringJoiner;

import org.apache.commons.math3.distribution.LogNormalDistribution;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

import static org.apache.commons.math3.util.FastMath.log;

/**
 * Demonstrates fitting a metalog to simulated aggregate losses,
 * printing simple LEC lines, and finally emitting a Vega-Lite JSON.
 */

public class LossExceedanceExample {
    public static record QuantileResult(double[] p, double[] x) {
    }

    public static void main(String[] args) {

        //String outputFilename = "vega-lite-lec-stub.json"; // not used in this example at the moment

        int sims = 10_000;
        Random rnd = new Random(42);

        // Log-Normal parameters: we derive a mean of ln(X), standard deviation of ln(X), 
        // from a lower and upper bound on X.
        // Here, a loss between 100 and 200 with 80% confidence.
        double lossLB = 100;
        double lossUB = 200;
        double meanLog = (log(lossUB) + log(lossLB)) / 2.0;
        double varLog = (log(lossUB) - log(lossLB)) / 3.29;

        LogNormalDistribution lnDist = new LogNormalDistribution(meanLog, varLog);

        // 1) Simulate aggregate losses
        // Assume 3 independent events, each with 10% chance of occurrence
        // in a given year, and losses drawn from the above Log-Normal distribution.
        // Simulate 'sims' years and record the total loss each year and the individual losses, given the occurance likelihood.
        int numberOfEvents = 3;
        double singleEventProb = 0.1;
        double[][] losses = new double[numberOfEvents + 1][sims]; // +1 for the total
        for (int i = 0; i < sims; i++) {
            double total = 0.0;
            for (int e = 0; e < numberOfEvents; e++) {
                if (rnd.nextDouble() < singleEventProb) {
                    double loss = lnDist.sample();
                    total += loss;
                    losses[e][i] = loss;
                }
            }
            losses[numberOfEvents][i] = total;
        }

        // 2) Extract K=9 quantiles
        int K = 9;
        QuantileResult qr = extractFixedQuantiles(losses[numberOfEvents]);
        double[] p = qr.p;
        double[] x = qr.x;

        // 3) Fit metalog via QP with a 0 lower bound
        // 0 is a natural lower bound for losses as you cannot have negative losses.
        Metalog metalog = QPFitter.with(p, x, K).lower(0.0).fit();

        // 4) First, print simple LEC lines: “p -> threshold, exceed”
        StringJoiner sjExceedance = new StringJoiner(",\n  ", "[\n  ", "\n]");
        int grid = 100;
        for (int i = 1; i <= grid; i++) {
            double pi = i / (double) (grid + 1);
            double xi = metalog.quantile(pi);
            double exceed = 1.0 - pi;
            sjExceedance.add(String.format(
                    "{\"quantile\": %.4f, \"p\": %.2f}",
                    xi, exceed));
        }

        // 5) Then build the JSON array of {quantile, p}
        StringJoiner sjQuantile = new StringJoiner(",\n  ", "[\n  ", "\n]");
        for (int i = 1; i <= grid; i++) {
            double pi = i / (double) (grid + 1);
            double xi = metalog.quantile(pi);
            sjQuantile.add(String.format(
                    "{\"quantile\": %.4f, \"p\": %.2f}",
                    xi, pi));
        }

        // 6) Finally emit the JSON block you can paste into Vega-Lite
         System.out.println(sjQuantile.toString());

   
        // System.out.println(sjHist.toString());
        // 8) Build and print the theoretical PDF of the log-normal(μ,σ) over a grid of
        // x values
        int pdfGrid = 100;

        // determine the plotting range from your simulated losses
        double xMin = java.util.Arrays.stream(losses[numberOfEvents]).min().orElse(0.0);
        double xMax = java.util.Arrays.stream(losses[numberOfEvents]).max().orElse(1.0);

        StringJoiner sjPdf = new StringJoiner(",\n  ", "[\n  ", "\n]");
        for (int i = 0; i < pdfGrid; i++) {
            // equally spaced bin centers
            double xVal = xMin + (i + 0.5) * (xMax - xMin) / pdfGrid;
            // PDF from the fitted LogNormalDistribution
            double pdfVal = lnDist.density(xVal);
            sjPdf.add(String.format(
                    "{\"quantile\": %.4f, \"p\": %.6f}",
                    xVal, pdfVal));
        }

        System.out.println(sjPdf.toString());

    }

    /**
     * Extracts quantile values at fixed probabilities from the sorted losses array.
     * 
     * @param losses array of simulated losses
     * @return a QuantileResult containing p[] and x[] arrays
     */
    private static QuantileResult extractFixedQuantiles(double[] losses) {
        double[] fixedP = {
                0.001, 0.02, 0.1, 0.25, 0.5, 0.75, 0.9, 0.98, 0.999
        };
        int sims = losses.length;
        Arrays.sort(losses);

        double[] p = new double[fixedP.length];
        double[] x = new double[fixedP.length];

        for (int i = 0; i < fixedP.length; i++) {
            p[i] = fixedP[i];
            int idx = (int) Math.floor(p[i] * sims);
            x[i] = losses[Math.min(idx, sims - 1)];
        }

        return new QuantileResult(p, x);
    }
}
