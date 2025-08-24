package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Metalog distribution: basis functions, quantile & PDF evaluation.
 */
public class Metalog {
    /** Maximum number of terms currently supported (T₀…T₈). */
    private static final int MAX_TERMS = 9;

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
        this.a = coefficients.clone();
        this.terms = a.length;
    }

    /**
     * Compute the basis functions T_j(p) for j=0..terms-1 as in Keelin (2016).
     * 
     * T₀(p)=1  
     * T₁(p)=logit(p)=ln[p/(1–p)]  
     * T₂(p)=p–0.5  
     * T₃(p)=(p–0.5)*logit(p)  
     * T₄(p)=(p–0.5)²  
     * T₅(p)=(p–0.5)²*logit(p)  
     * T₆(p)=(p–0.5)³  
     * T₇(p)=(p–0.5)³*logit(p)  
     * T₈(p)=(p–0.5)⁴  
     * 
     * @param p        probability in (0,1)
     * @param terms    how many terms to compute (2…MAX_TERMS)
     * @return         array of length = terms
     * @throws IllegalArgumentException if p≤0 or ≥1, or terms out of [2,MAX_TERMS]
     */
    public static double[] basisFunctions(double p, int terms) {
        validateInputs(p, terms);
        double phi   = p - 0.5;                      // φ(p)
        double logit = FastMath.log(p / (1.0 - p));  // ℓ(p)
        double[] T   = new double[terms];

        T[0] = 1.0;
        if (terms > 1) T[1] = logit;
        if (terms > 2) T[2] = phi;
        if (terms > 3) T[3] = phi * logit;
        if (terms > 4) T[4] = phi * phi;
        if (terms > 5) T[5] = T[4] * logit;
        if (terms > 6) T[6] = phi * phi * phi;
        if (terms > 7) T[7] = T[6] * logit;
        if (terms > 8) T[8] = phi * phi * phi * phi;

        return T;
    }

    /**
     * Inverse CDF (quantile) M(p) = Σ_j a_j * T_j(p).
     * 
     * @param p probability in (0,1)
     * @return  Q(p)
     * @throws  IllegalArgumentException if p≤0 or ≥1
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
     * Derivatives are supplied by basisDerivatives().
     * 
     * @param p probability in (0,1)
     * @return  density at p
     * @throws  IllegalArgumentException if p≤0 or ≥1
     */
    public double pdf(double p) {
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("p must be in (0,1), got " + p);
        }
        double[] dT = basisDerivatives(p, terms);
        double dMdp = 0.0;
        for (int j = 0; j < terms; j++) {
            dMdp += a[j] * dT[j];
        }
        return 1.0 / dMdp;
    }

    /**
     * Derivatives dT_j(p)/dp for j=0..terms-1.
     * Must mirror basisFunctions() exactly.
     * 
     * Even-indexed terms (T_{2k}=φ^k) use the power rule:
     *    d[φ^k]/dp = k * φ^(k-1)
     * Odd-indexed terms (T_{2k+1}=φ^k * logit) use the product rule:
     *    d[φ^kℓ]/dp = k φ^(k-1)ℓ + φ^k·[1/(p(1−p))]
     * 
     * @param p     probability in (0,1)
     * @param terms how many terms (2…MAX_TERMS)
     * @return      array of derivatives length = terms
     * @throws      IllegalArgumentException if p≤0 or ≥1, or terms out of [2,MAX_TERMS]
     */
    public static double[] basisDerivatives(double p, int terms) {
        validateInputs(p, terms);
        double phi   = p - 0.5;
        double logit = FastMath.log(p / (1.0 - p));
        double inv   = 1.0 / (p * (1.0 - p));
        double[] d   = new double[terms];

        // T₀=1 → dT₀=0 (already default)
        if (terms > 1) {
            d[1] = inv;                                   // d[logit]/dp
        }
        if (terms > 2) {
            d[2] = 1.0;                                   // d[φ]/dp
        }
        if (terms > 3) {
            // d[φ·ℓ]/dp = 1·ℓ + φ·(1/[p(1-p)])
            d[3] = logit + phi * inv;
        }
        if (terms > 4) {
            d[4] = 2.0 * phi;                             // d[φ^2]/dp
        }
        if (terms > 5) {
            // d[φ^2·ℓ]/dp = 2φ·ℓ + φ^2·(1/(p(1-p)))
            d[5] = 2.0 * phi * logit + phi * phi * inv;
        }
        if (terms > 6) {
            d[6] = 3.0 * phi * phi;                       // d[φ^3]/dp
        }
        if (terms > 7) {
            // d[φ^3·ℓ]/dp = 3φ^2·ℓ + φ^3·(1/(p(1-p)))
            d[7] = 3.0 * phi * phi * logit + phi * phi * phi * inv;
        }
        if (terms > 8) {
            d[8] = 4.0 * phi * phi * phi;                 // d[φ^4]/dp
        }

        return d;
    }

    /** Validate p∈(0,1) and 2≤terms≤MAX_TERMS. */
    protected static void validateInputs(double p, int terms) {
        if (terms < 2 || terms > MAX_TERMS) {
            throw new IllegalArgumentException(
                "terms must be in [2," + MAX_TERMS + "], got " + terms);
        }
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("p must be in (0,1), got " + p);
        }
    }
}
