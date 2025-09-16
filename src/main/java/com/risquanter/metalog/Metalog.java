package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Metalog distribution: basis functions, quantile & PDF evaluation.
 */
public class Metalog {
    private static final int MAX_TERMS = 20;

    private final double[] a;   // coefficients a₀…a_{terms-1}
    private final int terms;    // number of terms in use

    /**
     * Construct a Metalog with the given coefficients.
     *
     * @param coefficients array of metalog coefficients; length between 2 and MAX_TERMS
     * @throws IllegalArgumentException if null, length < 2, or length > MAX_TERMS
     */
    public Metalog(double[] coefficients) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficient array must not be null");
        }
        if (coefficients.length < 2 || coefficients.length > MAX_TERMS) {
            throw new IllegalArgumentException(
                "Number of terms must be between 2 and " + MAX_TERMS
                + ", got " + coefficients.length);
        }
        this.a     = coefficients.clone();
        this.terms = a.length;
    }

    /**
     * Compute the basis functions T_j(p) for j=0..terms-1,
     * exactly in the order used by Keelin (2016), eqn (6):
     *
     *   T₀(p) = 1
     *   T₁(p) = ln[p/(1-p)]
     *   T₂(p) = (p-0.5)*ln[p/(1-p)]
     *   T₃(p) = (p-0.5)
     *   T₄(p) = (p-0.5)^2
     *   T₅(p) = (p-0.5)^2*ln[p/(1-p)]
     *   T₆(p) = (p-0.5)^3
     *   T₇(p) = (p-0.5)^3*ln[p/(1-p)]
     *   T₈(p) = (p-0.5)^4
     *   …
     *
     * @param p      probability in (0,1)
     * @param terms  how many terms to compute (2…MAX_TERMS)
     * @return       array of length = terms
     */
    public static double[] basisFunctions(double p, int terms) {
        validateInputs(p, terms);

        double phi   = p - 0.5;
        double logit = FastMath.log(p / (1.0 - p));
        double[] T   = new double[terms];

        T[0] = 1.0;
        if (terms > 1) {
            T[1] = logit;
        }
        // now terms >= 2
        for (int j = 2; j < terms; j++) {
            int power = j / 2;  // integer division floors(j/2)
            if ((j & 1) == 0) {
                // even j: T_j = φ^power
                T[j] = FastMath.pow(phi, power);
            } else {
                // odd j:  T_j = φ^power * ln[p/(1-p)]
                T[j] = FastMath.pow(phi, power) * logit;
            }
        }
        return T;
    }

    /**
     * Inverse CDF (quantile) M(p) = Σ_j a_j * T_j(p).
     *
     * @param p probability in (0,1)
     * @return  quantile Q(p)
     */
    public double quantile(double p) {
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("p must be in (0,1), got " + p);
        }
        double[] T = basisFunctions(p, terms);
        double q = 0.0;
        for (int j = 0; j < terms; j++) {
            q += a[j] * T[j];
        }
        return q;
    }

    /**
     * PDF f(p) = 1 / (dM/dp) = 1 / Σ_j a_j * dT_j/dp.
     *
     * @param p probability in (0,1)
     * @return  density at p
     */
    public double pdf(double p) {
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("p must be in (0,1), got " + p);
        }
        double[] dT  = basisDerivatives(p, terms);
        double dMdp  = 0.0;
        for (int j = 0; j < terms; j++) {
            dMdp += a[j] * dT[j];
        }
        return 1.0 / dMdp;
    }

    /**
     * Derivatives dT_j(p)/dp for j=0..terms-1, mirroring basisFunctions:
     *   - T₀=1   → dT₀=0
     *   - T₁=logit(p) → dT₁=1/[p(1-p)]
     *   - T₂=φ·logit → dT₂ = 1·logit + φ·(1/[p(1-p)])
     *   - T₃=φ      → dT₃ = 1
     *   - T₄=φ²     → dT₄ = 2φ
     *   - T₅=φ²·logit → dT₅ = 2φ·logit + φ²·(1/[p(1-p)])
     *   - etc.
     *
     * @param p     probability in (0,1)
     * @param terms how many terms (2…MAX_TERMS)
     * @return      array of derivatives length = terms
     */
    public static double[] basisDerivatives(double p, int terms) {
        validateInputs(p, terms);

        double phi   = p - 0.5;
        double logit = FastMath.log(p / (1.0 - p));
        double inv   = 1.0 / (p * (1.0 - p));
        double[] d   = new double[terms];

        // dT₀/dp = 0
        if (terms > 1) {
            d[1] = inv;  // derivative of logit
        }
        for (int j = 2; j < terms; j++) {
            int power = j / 2;
            if ((j & 1) == 0) {
                // even j: T_j = φ^power → d = power·φ^(power-1)
                d[j] = power * FastMath.pow(phi, power - 1);
            } else {
                // odd j:  T_j = φ^power·logit
                // d = power·φ^(power-1)·logit + φ^power·(1/[p(1-p)])
                double phiK   = FastMath.pow(phi, power);
                double phiKm1 = FastMath.pow(phi, power - 1);
                d[j] = power * phiKm1 * logit + phiK * inv;
            }
        }
        return d;
    }

    /** Validate p∈(0,1) and 2≤terms≤MAX_TERMS. */
    public static void validateInputs(double p, int terms) {
        if (terms < 2 || terms > MAX_TERMS) {
            throw new IllegalArgumentException(
                "terms must be in [2," + MAX_TERMS + "], got " + terms);
        }
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("p must be in (0,1), got " + p);
        }
    }
}
