package com.risquanter.examples;

import java.util.Random;
import java.util.StringJoiner;

import org.apache.commons.math3.distribution.LogNormalDistribution;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.estimate.QPFitter;

/**
 * Demonstrates fitting a metalog to simulated aggregate losses,
 * printing simple LEC lines, and finally emitting a JSON array.
 */
public class LossExceedanceExample {
    public static void main(String[] args) {
        int sims = 10_000;
        Random rnd = new Random(42);

        // Log-Normal parameters: mean of ln(X), standard deviation of ln(X)
        double mu    = 1.0;
        double sigma = 0.5;
        LogNormalDistribution dist = new LogNormalDistribution(mu, sigma);

        // 1) Simulate aggregate losses
        double[] losses = new double[sims];
        for (int i = 0; i < sims; i++) {
            double total = 0.0;
            for (int e = 0; e < 5; e++) {
                if (rnd.nextDouble() < 0.3) {
                    total += dist.sample();
                }
            }
            losses[i] = total;
        }

        // 2) Extract K=9 quantiles
        int K = 9;
        double[] p = new double[K], x = new double[K];
        java.util.Arrays.sort(losses);
        double[] fixedP = {0.001, 0.02, 0.1, 0.25, 0.5, 0.75, 0.9, 0.98, 0.999};
        for (int i = 0; i < K; i++) {
            p[i] = fixedP[i];
            int idx = (int)Math.floor(p[i] * sims);
            x[i] = losses[Math.min(idx, sims - 1)];
        }

        // 3) Fit metalog via QP with a 0 lower bound
        Metalog metalog  = QPFitter.with(p, x, K).lower(0.0).fit();

        // 4) First, print simple LEC lines: “p -> threshold, exceed”
        int grid = 100;
        for (int i = 1; i <= grid; i++) {
            double pi      = i / (double)(grid + 1);
            double xi      = metalog.quantile(pi);
            double exceed  = 1.0 - pi;
            System.out.printf(
              "%.3f -> threshold=%.4f, exceed=%.4f%n",
              pi, xi, exceed
            );
        }

        // 5) Then build the JSON array of {quantile, p}
        StringJoiner sj = new StringJoiner(",\n  ", "[\n  ", "\n]");
        for (int i = 1; i <= grid; i++) {
            double pi = i / (double)(grid + 1);
            double xi = metalog.quantile(pi);
            sj.add(String.format(
              "{\"quantile\": %.4f, \"p\": %.2f}",
              xi, pi
            ));
        }

        // 6) Finally emit the JSON block you can paste into Vega-Lite
        System.out.println(sj.toString());
    }
}
