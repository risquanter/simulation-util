package com.risquanter.examples;

import java.util.Arrays;
import java.util.StringJoiner;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.estimate.QPFitter;

public class ObservationalMetalogExactFitExample {
    public static void main(String[] args) {
        // 1) Raw observations
        double[] observations = {78, 65, 82, 90, 120, 160, 143, 127, 110};
        int n = observations.length;

        // 2) Sort and compute plotting‐positions p_i = (i+0.5)/n
        Arrays.sort(observations);
        double[] pValues = new double[n];
        for (int i = 0; i < n; i++) {
            pValues[i] = (i + 0.5) / n; // Hazen plotting positions
        }

        // 3) Fit an n‐term metalog (exact through each point)
        Metalog metalog = QPFitter.with(pValues, observations, n).fit();

        // 4) Build JSON for the original data points
        StringJoiner obsSj = new StringJoiner(",\n  ", "[\n  ", "\n]");
        for (int i = 0; i < n; i++) {
            obsSj.add(String.format(
                "{\"quantile\": %.1f, \"p\": %.3f}",
                observations[i], pValues[i]
            ));
        }
        String obsJson = obsSj.toString();

        // 5) Build JSON for the fitted CDF at p=0.01…0.99
        int grid = 99;
        StringJoiner fitSj = new StringJoiner(",\n  ", "[\n  ", "\n]");
        for (int i = 1; i <= grid; i++) {
            double p = i / 100.0;
            double q = metalog.quantile(p);
            fitSj.add(String.format(
                "{\"quantile\": %.3f, \"p\": %.2f}",
                q, p
            ));
        }
        String fitJson = fitSj.toString();

        // 6) Print both arrays
        System.out.println("// === original observations ===");
        System.out.println(obsJson);
        System.out.println();
        System.out.println("// === fitted Metalog CDF ===");
        System.out.println(fitJson);
    }
}
