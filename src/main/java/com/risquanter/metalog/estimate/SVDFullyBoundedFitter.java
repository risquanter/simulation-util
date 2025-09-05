package com.risquanter.metalog.estimate;

import org.apache.commons.math3.util.FastMath;

import com.risquanter.metalog.Metalog;

/**
 * Fits a fully‐bounded metalog: support [L, U].
 *
 * We transform each observed xᵢ → yᵢ via
 *   yᵢ = ln[ (xᵢ–L)/(U–xᵢ) ],
 * then fit
 *   y(pᵢ) = Σ aⱼ · Tⱼ(pᵢ)
 * by ordinary least squares.
 */
public class SVDFullyBoundedFitter {
    private final double[] pValues;
    private final double[] xValues;
    private final int     terms;
    private final double  lowerBound;
    private final double  upperBound;

    /**
     * @param pValues     probabilities pᵢ ∈ (0,1)
     * @param xValues     observed quantiles xᵢ ∈ (lowerBound, upperBound)
     * @param terms       number of basis terms (2…MAX_TERMS)
     * @param lowerBound  support lower bound L
     * @param upperBound  support upper bound U
     */
    public SVDFullyBoundedFitter(
            double[] pValues,
            double[] xValues,
            int     terms,
            double  lowerBound,
            double  upperBound
    ) {
        if (pValues == null || xValues == null) {
            throw new IllegalArgumentException("pValues and xValues must not be null");
        }
        if (pValues.length != xValues.length) {
            throw new IllegalArgumentException("Lengths of pValues and xValues must match");
        }
        if (upperBound <= lowerBound) {
            throw new IllegalArgumentException(
                "upperBound must exceed lowerBound; got L=" 
              + lowerBound + ", U=" + upperBound
            );
        }

        // Validate term count (uses Metalog.validateInputs)
        Metalog.validateInputs(0.5, terms);

        this.pValues    = pValues.clone();
        this.xValues    = xValues.clone();
        this.terms      = terms;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Fit the fully‐bounded metalog by OLS:
     *   yᵢ = ln[(xᵢ–L)/(U–xᵢ)] = Σ aⱼ Tⱼ(pᵢ).
     *
     * @return coefficients a₀…a_{terms-1}
     */
    public double[] fit() {
        int K = pValues.length;
        double[] yValues = new double[K];

        for (int i = 0; i < K; i++) {
            double p = pValues[i];
            Metalog.validateInputs(p, terms);

            double x = xValues[i];
            if (x <= lowerBound || x >= upperBound) {
                throw new IllegalArgumentException(
                  "All xValues[i] must lie strictly in (L,U); "
                + "but xValues[" + i + "]=" + x
                + " not in (" + lowerBound + "," + upperBound + ")"
                );
            }

            double numerator   = x - lowerBound;
            double denominator = upperBound - x;
            yValues[i] = FastMath.log(numerator / denominator);
        }

        // Delegate to the unbounded MetalogFitter on (pValues, yValues)
        SVDFitter baseFitter = new SVDFitter(pValues, yValues, terms);
        return baseFitter.fit();
    }
}
