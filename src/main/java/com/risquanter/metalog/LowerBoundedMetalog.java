package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Lower‐bounded Metalog distribution (support [L,∞)).
 *
 * y(p) = Σ a_j T_j(p)
 * Q(p) = L + exp(y(p))
 * f(p) = 1 / [ (Q(p)-L) * y'(p) ]
 */
public class LowerBoundedMetalog {
    private final double[] a;
    private final int terms;
    private final double lowerBound;

    public LowerBoundedMetalog(double[] coefficients, double lowerBound) {
        this.a          = coefficients.clone();
        this.terms      = a.length;
        this.lowerBound = lowerBound;
    }

    /**
     * Quantile: Q(p) = L + exp( Σ a_j T_j(p) )
     */
    public double quantile(double p) {
        double[] T = Metalog.basisFunctions(p, terms);
        double y   = 0.0;
        for (int j = 0; j < terms; j++) {
            y += a[j] * T[j];
        }
        return lowerBound + FastMath.exp(y);
    }

    /**
     * PDF: f(p) = 1 / [ (Q-L) * (dy/dp) ]
     */
    public double pdf(double p) {
        // 1) compute basis and its derivative
        double[] T   = Metalog.basisFunctions(p, terms);
        double[] dT  = Metalog.basisDerivatives(p, terms);

        // 2) accumulate y and its derivative dy/dp
        double y     = 0.0;
        double dydp  = 0.0;
        for (int j = 0; j < terms; j++) {
            y    += a[j] * T[j];
            dydp += a[j] * dT[j];
        }

        double xMinusL = FastMath.exp(y);
        // Q(p) = L + xMinusL
        // f = 1 / [ xMinusL * dydp ]
        return 1.0 / (xMinusL * dydp);
    }
}
