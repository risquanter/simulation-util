package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Fits a lower‐bounded metalog: Q(p) ≥ lowerBound.
 *
 * We transform xᵢ → yᵢ = ln(xᵢ – L), then fit
 *   y(pᵢ) = Σ aⱼ · Tⱼ(pᵢ)
 * by ordinary least squares.
 */
public class LowerBoundedMetalogFitter {
    private final double[] pValues;
    private final double[] xValues;
    private final int terms;
    private final double lowerBound;

    /**
     * @param pValues     probabilities pᵢ ∈ (0,1)
     * @param xValues     observed quantiles Q(pᵢ) ≥ lowerBound
     * @param terms       # basis terms (2…MAX_TERMS)
     * @param lowerBound  known minimum support L
     */
    public LowerBoundedMetalogFitter(
        double[] pValues,
        double[] xValues,
        int     terms,
        double  lowerBound
    ) {
        if (pValues == null || xValues == null) {
            throw new IllegalArgumentException("pValues and xValues must not be null");
        }
        if (pValues.length != xValues.length) {
            throw new IllegalArgumentException("Lengths of pValues and xValues must match");
        }
        // Validate term count (uses same checks as Metalog)
        Metalog.validateInputs(0.5, terms);

        this.pValues    = pValues.clone();
        this.xValues    = xValues.clone();
        this.terms      = terms;
        this.lowerBound = lowerBound;
    }

    /**
     * Perform the OLS fit on
     *   yᵢ = ln(xᵢ – L) = Σ aⱼ Tⱼ(pᵢ).
     *
     * @return the fitted coefficient array a₀…a_{terms-1}
     */
    public double[] fit() {
        int K = pValues.length;
        double[] yValues = new double[K];

        for (int i = 0; i < K; i++) {
            double p = pValues[i];
            Metalog.validateInputs(p, terms);

            double shifted = xValues[i] - lowerBound;
            if (shifted <= 0.0) {
                throw new IllegalArgumentException(
                    "All xValues[i] must exceed lowerBound; "
                  + "xValues[" + i + "]=" + xValues[i]
                  + " ≤ lowerBound=" + lowerBound
                );
            }
            yValues[i] = FastMath.log(shifted);
        }

        // Delegate to the unbounded fitter on (pValues, yValues)
        MetalogFitter baseFitter = new MetalogFitter(pValues, yValues, terms);
        return baseFitter.fit();
    }
}
