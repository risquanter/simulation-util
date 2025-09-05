package com.risquanter.metalog.examples;

import java.util.stream.IntStream;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.estimate.QPLowerBoundedConstrainedFitter;

public class ExpertOpinionLowerBoundedConstrainedMetalogDemo {
    public static void main(String[] args) {

        // 1) Expert’s quantiles
        double[] pVals     = { 0.10, 0.50, 0.90 };
        double[] xVals     = { 17.0, 24.0, 35.0 };
        int      terms      = pVals.length + 1;  // = 3

        double epsilon = 1e-6;
        double[] gridP = IntStream.rangeClosed(1, 99)
                                  .mapToDouble(i -> i/100.0)
                                  .toArray();

        double lowerBound = 10.0;

        // 3) Fit via constrained QP
        QPLowerBoundedConstrainedFitter fitter =
            new QPLowerBoundedConstrainedFitter(pVals, xVals, terms, epsilon, gridP,lowerBound) ;
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
