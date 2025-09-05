package com.risquanter.metalog.examples;

import java.util.Arrays;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.estimate.SVDFitter;

public class ObservationalMetalogFitExample {
    public static void main(String[] args) {
        // 1) Raw observations (n = 12)
        double[] observations = {
            78, 65, 82, 90, 120, 150, 160, 140, 130, 110, 95, 85
        };
        Arrays.sort(observations);

        // 2) Compute plotting positions p_i = (i - 0.5)/n
        int n = observations.length;
        double[] pValues = new double[n];
        for (int i = 0; i < n; i++) {
            pValues[i] = (i + 0.5) / n;
        }
        double[] xValues = observations.clone();

        // 3) Choose at most 9 terms
        int maxTerms = 3;
        int terms    = Math.min(n, maxTerms);

        // 4) Fit a "9-term" metalog to our 12 (p_i, x_i) points
        SVDFitter fitter = new SVDFitter(pValues, xValues, terms);
        double[] coeffs = fitter.fit();

        // 5) Wrap into a Metalog distribution
        Metalog metalog = new Metalog(coeffs);

        // 6) Emit fitted quantiles for p = 0.01 … 0.99
        System.out.println("[");
        for(int i = 1; i < 100; i++) {
            double p = i / 100.0;
            double q = metalog.quantile(p);
            System.out.printf("  {\"quantile\":%.3f, \"p\":%.2f}%s%n",
                              q, p, (i < 99 ? "," : ""));
        }
        System.out.println("]");
    }
}
