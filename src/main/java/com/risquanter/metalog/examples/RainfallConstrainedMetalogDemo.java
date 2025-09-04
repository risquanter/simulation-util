package com.risquanter.metalog.examples;

import java.util.Arrays;
import java.util.stream.IntStream;
import com.risquanter.metalog.ConstrainedMetalogFitter;
import com.risquanter.metalog.Metalog;

public class RainfallConstrainedMetalogDemo {
    public static void main(String[] args) {
        // 1) Raw daily rainfall (mm)
        double[] obs = {78,65,82,90,120,150,160,140,130,110,95,85};
        Arrays.sort(obs);

        int n = obs.length;
        double[] pVals = new double[n];
        double[] xVals = new double[n];
        for (int i = 0; i < n; i++) {
            pVals[i] = (i + 0.5) / n;  // Hazen plotting positions
            xVals[i] = obs[i];
        }

        // 2) Fitter parameters
        int    terms   = Math.min(n, 9);
        double epsilon = 1e-6;
        double[] gridP = IntStream.rangeClosed(1, 99)
                                  .mapToDouble(i -> i/100.0)
                                  .toArray();

        // 3) Fit via constrained QP
        ConstrainedMetalogFitter fitter =
            new ConstrainedMetalogFitter(pVals, xVals, terms, epsilon, gridP);
        double[] coeffs = fitter.fit();

        // 4) Wrap into a Metalog and emit CDF JSON
        Metalog metalog = new Metalog(coeffs);
        System.out.println("[");
        for (int i = 1; i < 100; i++) {
            double p = i / 100.0;
            double q = metalog.quantile(p);
            System.out.printf("  {\"quantile\":%.3f, \"p\":%.2f}%s%n",
                              q, p, (i < 99 ? "," : ""));
        }
        System.out.println("]");
    }
}
