package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Lower‐bounded Metalog distribution (support [L,∞)).
 *
 * y(p)   = Σ aⱼ Tⱼ(p)
 * Q(p)   = L + exp( y(p) )
 * f(p)   = 1 / [ (Q(p) – L) * y′(p) ]
 */
public class LowerBoundedMetalog {
    private final double[] a;
    private final int     terms;
    private final double  lowerBound;

    /**
     * @param coefficients metalog coefficients (length 2…MAX_TERMS)
     * @param lowerBound   known minimum support L
     */
    public LowerBoundedMetalog(double[] coefficients, double lowerBound) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients must not be null");
        }
        // Validate term count using same logic as Metalog
        Metalog.validateInputs(0.5, coefficients.length);

        this.a          = coefficients.clone();
        this.terms      = a.length;
        this.lowerBound = lowerBound;
    }

    /**
     * Quantile function: Q(p) = L + exp( Σ aⱼ Tⱼ(p) ).
     *
     * @param p probability in (0,1)
     * @return  the quantile Q(p)
     */
    public double quantile(double p) {
        Metalog.validateInputs(p, terms);

        double[] T = Metalog.basisFunctions(p, terms);
        double y   = 0.0;
        for (int j = 0; j < terms; j++) {
            y += a[j] * T[j];
        }
        return lowerBound + FastMath.exp(y);
    }

    /**
     * PDF: f(p) = 1 / [ (Q(p)-L) * (dy/dp) ].
     *
     * @param p probability in (0,1)
     * @return  the density f(p)
     */
    public double pdf(double p) {
        Metalog.validateInputs(p, terms);

        // 1) compute basis and its derivative vectors
        double[] T  = Metalog.basisFunctions(p, terms);
        double[] dT = Metalog.basisDerivatives(p, terms);

        // 2) accumulate y(p) and its slope dy/dp
        double y    = 0.0;
        double dydp = 0.0;
        for (int j = 0; j < terms; j++) {
            y    += a[j] * T[j];
            dydp += a[j] * dT[j];
        }

        // exp(y) = Q(p) - L
        double xMinusL = FastMath.exp(y);

        // f(p) = 1 / [ xMinusL * dydp ]
        return 1.0 / (xMinusL * dydp);
    }
}
