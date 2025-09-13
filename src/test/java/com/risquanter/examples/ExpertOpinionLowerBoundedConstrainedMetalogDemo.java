package com.risquanter.examples;

import java.util.stream.IntStream;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

public class ExpertOpinionLowerBoundedConstrainedMetalogDemo {
    public static void main(String[] args) {

        // 1) Expert’s quantiles
        double[] pVals = { 0.10, 0.50, 0.90 };
        double[] xVals = { 17.0, 24.0, 35.0 };
        int terms = pVals.length + 1; // = 4 for smoother fitting

        // 2) Fitting parameters - same as defaults, but explicit here to present the API
        double epsilon = 1e-6;
        double[] gridP = IntStream.rangeClosed(1, 99)
                .mapToDouble(i -> i / 100.0)
                .toArray();

        Double lowerBound = 10.0;
        Double upperBound = 40.0;

        // 3) Fit via constrained QP
        Metalog metalog = QPFitter.with(pVals, xVals, terms)
                .epsilon(epsilon)
                .grid(gridP)
                .lower(lowerBound)
                .upper(upperBound)
                .fit();

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
