package com.risquanter.metalog;

/**
 * Expert‐Opinion CDF Example for the Metalog API.
 *
 * Scenario:
 *   An expert provides three quantile estimates for
 *   completion time (days):
 *     • 5th percentile  (p=0.05):  3.0 days
 *     • 50th percentile (p=0.50):  6.0 days
 *     • 95th percentile (p=0.95): 10.0 days
 *
 * This example fits a 3‐term unbounded metalog to those points
 * and then emits a JSON array of { "quantile": Q(p), "p": p }
 * for p ∈ [0.05,0.95], equally spaced in 19 steps.
 * You can paste the printed JSON directly into the Vega‐Lite
 * “values” section to plot the CDF curve.
 */
public class ExpertOpinionCdfExample {

    public static void main(String[] args) {
        // 1) Expert‐provided quantiles
        double[] pVals = { 0.05, 0.50, 0.95 };
        double[] xVals = { 3.0,  6.0,  10.0 };

        // 2) Fit a 3‐term metalog (one term per quantile)
        MetalogFitter fitter = new MetalogFitter(pVals, xVals, pVals.length);
        double[] coeffs = fitter.fit();

        // Wrap in Metalog for quantile/CDF evaluation
        Metalog metalog = new Metalog(coeffs);

        // 3) Define the domain of interest [0.05 .. 0.95]
        double pMin = pVals[0];
        double pMax = pVals[pVals.length - 1];

        // Number of points (including endpoints) to sample
        int gridSize = 19;

        // 4) Emit JSON array for Vega-Lite CDF plot
        System.out.println("[");
        for (int i = 0; i < gridSize; i++) {
            // equally spaced p between pMin and pMax
            double pi = pMin + i * (pMax - pMin) / (gridSize - 1);
            double qi = metalog.quantile(pi);

            // JSON object with quantile (x‐axis) and p (y‐axis)
            System.out.printf(
                "  {\"quantile\": %.4f, \"p\": %.4f}%s%n",
                qi,
                pi,
                (i < gridSize - 1 ? "," : "")
            );
        }
        System.out.println("]");
    }
}
