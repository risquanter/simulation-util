package com.risquanter.metalog.fitter;

import org.apache.commons.math3.util.FastMath;

import com.risquanter.metalog.Metalog;

/**
 * Fits an upper-bounded metalog: Q(p) ≤ upperBound.
 *
 * We transform observed xᵢ → yᵢ = ln( upperBound – xᵢ ), then fit
 *   y(pᵢ) = Σ aⱼ · Tⱼ(pᵢ)
 * via ordinary least squares.
 */
public class UpperBoundedMetalogFitter {
    private final double[] pValues;
    private final double[] xValues;
    private final int     terms;
    private final double  upperBound;

    /**
     * @param pValues    probabilities pᵢ ∈ (0,1)
     * @param xValues    observed quantiles Q(pᵢ) ≤ upperBound
     * @param terms      number of basis terms (2…MAX_TERMS)
     * @param upperBound known maximum support U
     */
    public UpperBoundedMetalogFitter(
        double[] pValues,
        double[] xValues,
        int     terms,
        double  upperBound
    ) {
        if (pValues == null || xValues == null) {
            throw new IllegalArgumentException("pValues and xValues must not be null");
        }
        if (pValues.length != xValues.length) {
            throw new IllegalArgumentException("Lengths of pValues and xValues must match");
        }
        // Validate term range (uses same logic as Metalog)
        Metalog.validateInputs(0.5, terms);

        this.pValues    = pValues.clone();
        this.xValues    = xValues.clone();
        this.terms      = terms;
        this.upperBound = upperBound;
    }

    /**
     * Perform OLS on
     *   yᵢ = ln( U – xᵢ ) = Σ aⱼ Tⱼ(pᵢ).
     *
     * @return fitted coefficient array a₀…a_{terms-1}
     */
    public double[] fit() {
        int K = pValues.length;
        double[] yValues = new double[K];

        for (int i = 0; i < K; i++) {
            double p = pValues[i];
            Metalog.validateInputs(p, terms);

            double shifted = upperBound - xValues[i];
            if (shifted <= 0.0) {
                throw new IllegalArgumentException(
                    "All xValues[i] must be ≤ upperBound; "
                  + "but xValues[" + i + "]=" + xValues[i]
                  + " ≥ upperBound=" + upperBound
                );
            }
            yValues[i] = FastMath.log(shifted);
        }

        // Delegate to unbounded fitter on (pValues, yValues)
        SVDMetalogFitter baseFitter = new SVDMetalogFitter(pValues, yValues, terms);
        return baseFitter.fit();
    }
}
