package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Upper‐bounded Metalog distribution (support (−∞, U]).
 *
 * y(p)   = Σ aⱼ Tⱼ(p)
 * Q(p)   = U – exp( y(p) )
 * f(p)   = 1 / [ (U – Q(p)) * y′(p) ]
 *
 * Note on arrow notation:
 *   “Q → U” denotes (U – Q(p)), i.e. distance from the quantile up to the upper bound.
 */
public class UpperBoundedMetalog {
    private final double[] a;
    private final int     terms;
    private final double  upperBound;

    public UpperBoundedMetalog(double[] coefficients, double upperBound) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients must not be null");
        }
        Metalog.validateInputs(0.5, coefficients.length);
        this.a          = coefficients.clone();
        this.terms      = a.length;
        this.upperBound = upperBound;
    }

    public double quantile(double p) {
        Metalog.validateInputs(p, terms);

        double[] T = Metalog.basisFunctions(p, terms);
        double y   = 0.0;
        for (int j = 0; j < terms; j++) {
            y += a[j] * T[j];
        }
        return upperBound - FastMath.exp(y);
    }

    /**
     * PDF: f(p) = 1 / [ (U – Q(p)) * y′(p) ]
     *
     * Here “Q → U” means “go from the quantile up to the upper bound,” i.e. (U – Q(p)).
     */
    public double pdf(double p) {
        Metalog.validateInputs(p, terms);

        double[] T  = Metalog.basisFunctions(p, terms);
        double[] dT = Metalog.basisDerivatives(p, terms);

        double y    = 0.0;
        double dydp = 0.0;
        for (int j = 0; j < terms; j++) {
            y    += a[j] * T[j];
            dydp += a[j] * dT[j];
        }

        // exp(y) == U – Q(p)
        double diff = FastMath.exp(y);

        // f = 1 / [ (Q → U) * y′(p) ]
        return 1.0 / (diff * dydp);
    }
}
