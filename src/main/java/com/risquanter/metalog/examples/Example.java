package com.risquanter.metalog.examples;

import java.util.Random;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.MetalogFitter;

/**
 * Demonstrates fitting a metalog to simulated aggregate losses
 * and generating a smooth LEC curve.
 */
public class Example {
    public static void main(String[] args) {
        // 1) Simulate aggregate loss data (pilot run)
        int sims = 10_000;
        Random rnd = new Random(42);
        double[] losses = new double[sims];
        for (int i = 0; i < sims; i++) {
            double total = 0.0;
            for (int e = 0; e < 5; e++) {
                if (rnd.nextDouble() < 0.3) { // example p_i=0.3
                    total += Math.exp(rnd.nextGaussian() * 0.5 + 1.0);
                }
            }
            losses[i] = total;
        }

        // 2) Extract K=9 quantiles
        int K = 9;
        double[] p = new double[K], x = new double[K];
        java.util.Arrays.sort(losses);
        for (int i = 0; i < K; i++) {
            p[i] = new double[]{0.001,0.02,0.1,0.25,0.5,0.75,0.9,0.98,0.999}[i];
            int idx = (int)Math.floor(p[i] * sims);
            x[i] = losses[Math.min(idx, sims-1)];
        }

        // 3) Fit metalog
        MetalogFitter fitter = new MetalogFitter(p, x, K);
        double[] coeffs = fitter.fit();
        Metalog metalog = new Metalog(coeffs);

        // 4) Generate and print LEC
        int grid = 100;
        for (int i = 1; i <= grid; i++) {
            double pi = i / (double)(grid + 1);
            double xi = metalog.quantile(pi);
            double lec = 1.0 - pi;
            System.out.printf("%.3f -> threshold=%.4f, exceed=%.4f%n", pi, xi, lec);
        }
    }
}
