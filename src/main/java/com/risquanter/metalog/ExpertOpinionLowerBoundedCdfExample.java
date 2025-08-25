package com.risquanter.metalog;

/**
 * Expert‐Opinion CDF Example using a lower‐bounded Metalog (support [L,∞)).
 *
 * Expert triplet (symmetric‐percentile):
 *   • 10th percentile (p = 0.10): Q(0.10) = 17 days
 *   • 50th percentile (p = 0.50): Q(0.50) = 24 days
 *   • 90th percentile (p = 0.90): Q(0.90) = 35 days
 *
 * We assume a known lower bound L = 10 days (no finish earlier than day 10),
 * fit a 3-term lower-bounded metalog to those points, then emit a JSON
 * array of { "quantile": Q(p), "p": p } for p = 0.01, 0.02, …, 0.99.
 * Paste the output into Vega-Lite’s `data.values` for an ECDF plot.
 */
public class ExpertOpinionLowerBoundedCdfExample {

    public static void main(String[] args) {
        // 1) Expert’s quantiles
        double[] pVals     = { 0.10, 0.50, 0.90 };
        double[] xVals     = { 17.0, 24.0, 35.0 };
        double   lowerBound = 10.0;
        int      terms      = pVals.length;  // = 3

        // 2) Fit the lower-bounded metalog (support [L, ∞))
        LowerBoundedMetalogFitter fitter =
            new LowerBoundedMetalogFitter(pVals, xVals, terms, lowerBound);
        double[] coeffs = fitter.fit();

        // 3) Wrap the fitted coefficients in the distribution
        LowerBoundedMetalog metalog =
            new LowerBoundedMetalog(coeffs, lowerBound);

        // 4) Generate ECDF points for p = 0.01 … 0.99
        System.out.println("[");
        for (int i = 1; i < 100; i++) {
            double p = i / 100.0;
            double q = metalog.quantile(p);
            System.out.printf(
                "  {\"quantile\": %.4f, \"p\": %.2f}%s%n",
                q,
                p,
                (i < 99 ? "," : "")
            );
        }
        System.out.println("]");
    }
}
