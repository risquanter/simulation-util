package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Metalog distribution: basis functions, quantile & PDF evaluation.
 * 
 * Updated basisFunctions and basisDerivatives to match Keelin (2016) Eqn (6),
 * with special handling of term j==2 so that the first “power×logit” term
 * appears at index 2, and pure φ-powers and φ×logit alternate thereafter.
 */
public class Metalog {   
    private static final int MAX_TERMS = 20;
 
    private final double[] a;   // coefficients a₀…a_{terms-1}
    private final int terms;    // number of terms in use

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

    public static double[] basisFunctions(double p, int terms) {
        validateInputs(p, terms);

        double φ     = p - 0.5;
        double logit = FastMath.log(p / (1.0 - p));
        double[] T   = new double[terms];

        // T0 = 1
        T[0] = 1.0;

        // T1 = logit(p)
        if (terms > 1) {
            T[1] = logit;
        }

        // T2 = φ * logit(p)  ← special case for first “power×logit”
        if (terms > 2) {
            T[2] = φ * logit;
        }

        // j ≥ 3: even = φ^(j/2), odd = φ^((j-1)/2) * logit
        for (int j = 3; j < terms; j++) {
            if ((j & 1) == 0) {
                // even j ≥ 4: pure power
                int power = j / 2;
                T[j] = FastMath.pow(φ, power);
            } else {
                // odd j ≥ 3: power × logit
                int power = (j - 1) / 2;
                T[j] = FastMath.pow(φ, power) * logit;
            }
        }
        return T;
    }

    public static double[] basisDerivatives(double p, int terms) {
        validateInputs(p, terms);

        double φ     = p - 0.5;
        double logit = FastMath.log(p / (1.0 - p));
        double inv   = 1.0 / (p * (1.0 - p));
        double[] d   = new double[terms];

        // dT0/dp = 0
        d[0] = 0.0;

        // dT1/dp = 1/[p(1-p)]
        if (terms > 1) {
            d[1] = inv;
        }

        // dT2/dp = derivative of φ*logit = 1*logit + φ*inv
        if (terms > 2) {
            d[2] = logit + φ * inv;
        }

        // j ≥ 3: even = d/dp[φ^(j/2)] ; odd = d/dp[φ^((j-1)/2)*logit]
        for (int j = 3; j < terms; j++) {
            if ((j & 1) == 0) {
                // even j ≥ 4
                int power = j / 2;
                // derivative of φ^power = power * φ^(power-1)
                d[j] = power * FastMath.pow(φ, power - 1);
            } else {
                // odd j ≥ 3
                int power = (j - 1) / 2;
                double φk    = FastMath.pow(φ, power);
                double φkm1  = FastMath.pow(φ, power - 1);
                // d[φ^power*logit] = power·φ^(power-1)·logit + φ^power·inv
                d[j] = power * φkm1 * logit + φk * inv;
            }
        }
        return d;
    }

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
