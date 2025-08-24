package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Fits a lower-bounded metalog: Q(p) ≥ lowerBound.
 *
 * We define y(p) = ln[Q(p) - L] = Σ a_j T_j(p).
 */
public class LowerBoundedMetalogFitter {
    private final double[] pValues;
    private final double[] xValues;
    private final int terms;
    private final double lowerBound;

    /**
     * @param pValues     probabilities p₁…p_K
     * @param xValues     observed quantiles Q(pᵢ) ≥ lowerBound
     * @param terms       # basis terms
     * @param lowerBound  known minimum support L
     */
    public LowerBoundedMetalogFitter(
        double[] pValues, double[] xValues,
        int terms, double lowerBound
    ) {
        if (pValues.length != xValues.length) {
            throw new IllegalArgumentException("p and x lengths must match");
        }
        this.pValues    = pValues.clone();
        this.xValues    = xValues.clone();
        this.terms      = terms;
        this.lowerBound = lowerBound;
    }

    /**
     * @return coefficients a_j fitting
     *         y_i = ln(x_i - L) = Σ a_j T_j(p_i)
     */
    public double[] fit() {
        int K = pValues.length;
        double[] yValues = new double[K];

        for (int i = 0; i < K; i++) {
            double shifted = xValues[i] - lowerBound;
            if (shifted <= 0) {
                throw new IllegalArgumentException(
                  "All x_i must exceed lowerBound"
                );
            }
            yValues[i] = FastMath.log(shifted);
        }

        // Delegate to unbounded OLS fitter on (pValues, yValues)
        MetalogFitter base = new MetalogFitter(pValues, yValues, terms);
        return base.fit();
    }
}
