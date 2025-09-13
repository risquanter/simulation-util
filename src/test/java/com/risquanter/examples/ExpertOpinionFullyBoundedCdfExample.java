package com.risquanter.examples;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

/**
 * Expert‐Opinion CDF Example using a fully‐bounded Metalog (support [L,U]).
 *
 * Scenario:
 *   An expert provides three quantile estimates for
 *   completion time (days):
 *     •  5th percentile  (p=0.05):  3.0 days  ← lower bound L
 *     • 50th percentile  (p=0.50):  6.0 days
 *     • 95th percentile  (p=0.95): 10.0 days  ← upper bound U
 *
 * Steps:
 *   1) Define the expert’s (pᵢ, Qᵢ) triplet.
 *   2) Fit a 3-term fully-bounded metalog with support [LS=3.0, US=10.0].
 *   3) Generate evenly-spaced p values in [0.05, 0.95] and emit JSON
 *      of { "quantile": Q(p), "p": p } for Vega-Lite CDF plotting.
 */
public class ExpertOpinionFullyBoundedCdfExample {
    public static void main(String[] args) {
        // 1) Expert quantiles
        double[] pVals      = { 0.05, 0.50, 0.95 };
        double[] xVals      = {  3.0,  6.0, 10.0 };
        // choose true support bounds so  LB < min(xVals) < max(xVals) < UB
        double   lowerBound =  0.0;    // e.g. no negative days
        double   upperBound = 20.0;    // safely above the pessimistic estimate
        int      terms      = pVals.length;

        // 2) Fit fully-bounded metalog
        Metalog metalog = QPFitter.with(pVals, xVals, terms).lower(lowerBound).upper(upperBound).fit();

        double pMin     = pVals[0];
        double pMax     = pVals[pVals.length - 1];
        int    gridSize = 19;

        // 4) Emit JSON for Vega‐Lite
        System.out.println("[");
        for (int i = 0; i < gridSize; i++) {
            double pi = pMin + i*(pMax-pMin)/(gridSize-1);
            double qi = metalog.quantile(pi);
            System.out.printf(
              "  {\"quantile\": %.4f, \"p\": %.4f}%s%n",
              qi, pi, (i<gridSize-1?",":"")
            );
        }
        System.out.println("]");
    }
}
