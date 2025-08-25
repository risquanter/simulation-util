package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Fully‐bounded Metalog distribution (support [L, U]).
 *
 * We define
 *   y(p) = Σ aⱼ · Tⱼ(p),
 * then invert via
 *   Q(p) = L + (U–L)·σ(y(p)),  where σ(y)=eʸ/(1+eʸ).
 *
 * PDF:
 *   f(p) = (U – L)
 *          ⁄ [ (Q(p)–L) · (U–Q(p)) · y′(p) ].
 *
 * Note: 
 *  • “L→Q” = (Q(p) – L)  
 *  • “Q→U” = (U – Q(p))
 */
public class FullyBoundedMetalog {
    private final double[] a;
    private final int     terms;
    private final double  lowerBound;
    private final double  upperBound;

    /**
     * @param coefficients metalog coefficients (length 2…MAX_TERMS)
     * @param lowerBound   support lower bound L
     * @param upperBound   support upper bound U
     */
    public FullyBoundedMetalog(
            double[] coefficients,
            double   lowerBound,
            double   upperBound
    ) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients must not be null");
        }
        if (upperBound <= lowerBound) {
            throw new IllegalArgumentException(
                "upperBound must exceed lowerBound; got L="
              + lowerBound + ", U=" + upperBound
            );
        }
        // Validate term count
        Metalog.validateInputs(0.5, coefficients.length);

        this.a           = coefficients.clone();
        this.terms       = a.length;
        this.lowerBound  = lowerBound;
        this.upperBound  = upperBound;
    }

    /**
     * Quantile function:
     *   Let y = Σ aⱼ Tⱼ(p).
     *   Then σ = eʸ / (1 + eʸ),
     *   Q(p) = L + (U–L) · σ.
     */
    public double quantile(double p) {
        Metalog.validateInputs(p, terms);

        double[] T = Metalog.basisFunctions(p, terms);
        double y   = 0.0;
        for (int j = 0; j < terms; j++) {
            y += a[j] * T[j];
        }

        double expy = FastMath.exp(y);
        double sigma = expy / (1.0 + expy);
        return lowerBound + (upperBound - lowerBound) * sigma;
    }

    /**
     * PDF:
     *   f(p) = (U–L)
     *          ⁄ [ (Q(p)–L) · (U–Q(p)) · y′(p) ].
     *
     * Here “L→Q” = (Q(p)–L) and “Q→U” = (U–Q(p)).
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

        double expy = FastMath.exp(y);
        // Compute quantile and the two distances
        double Q     = lowerBound + (upperBound - lowerBound) * (expy / (1.0 + expy));
        double LtoQ  = Q - lowerBound;
        double QtoU  = upperBound - Q;

        // f(p) = (U–L) / [ (L→Q) * (Q→U) * y′(p) ]
        // here “Q → U” means “go from the quantile up to the upper bound,” i.e. (U – Q(p)).
        // and  “L → Q” means “go from the lower bound up to the quantile,” i.e. (Q(p) – L).
        return (upperBound - lowerBound) / (LtoQ * QtoU * dydp);
    }
}
