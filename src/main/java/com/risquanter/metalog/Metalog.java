package com.risquanter.metalog;

import org.apache.commons.math3.util.FastMath;

/**
 * Metalog distribution: basis functions, quantile & PDF evaluation.
 */
public class Metalog {
    private final double[] a;   // coefficients
    private final int terms;

    public Metalog(double[] coefficients) {
        this.a = coefficients.clone();
        this.terms = a.length;
    }

    /** Basis functions T_j(p) for j=0..terms-1 */
    public static double[] basisFunctions(double p, int terms) {
        double[] T = new double[terms];
        T[0] = 1.0;
        if (terms > 1) T[1] = FastMath.log(p / (1.0 - p));
        if (terms > 2) T[2] = p - 0.5;
        if (terms > 3) T[3] = T[2] * T[1];
        if (terms > 4) T[4] = FastMath.pow(T[2], 2);
        if (terms > 5) T[5] = T[4] * T[1];
        if (terms > 6) T[6] = FastMath.pow(T[2], 3);
        if (terms > 7) T[7] = T[6] * T[1];
        if (terms > 8) T[8] = FastMath.pow(T[2], 4);
        return T;
    }

    /** Quantile function M(p) = Σ a_j * T_j(p) */
    public double quantile(double p) {
        double[] T = basisFunctions(p, terms);
        double q = 0.0;
        for (int j = 0; j < terms; j++) {
            q += a[j] * T[j];
        }
        return q;
    }

    /** Approximate PDF f(x) = 1 / (dM/dp) at p */
    public double pdf(double p) {
        // derivative dM/dp = Σ a_j * dT_j/dp
        double dMdp = 0.0;
        // dT0/dp = 0
        if (terms > 1) {
            double inv = 1.0 / (p * (1.0 - p));
            dMdp += a[1] * inv;
        }
        if (terms > 2) {
            dMdp += a[2];
        }
        if (terms > 3) {
            double ln = FastMath.log(p / (1.0 - p));
            dMdp += a[3] * (ln + (p - 0.5) * (1.0 / (p * (1.0 - p))));
        }
        if (terms > 4) {
            dMdp += a[4] * 2.0 * (p - 0.5);
        }
        // Additional derivative terms for higher orders can be added similarly.
        return 1.0 / dMdp;
    }

    /**
     * Derivatives dT_j(p)/dp for j=0..terms-1.
     * Must mirror basisFunctions() structure.
     */
    public static double[] basisDerivatives(double p, int terms) {
        double[] d = new double[terms];
        // T0 = 1 → dT0=0
        if (terms > 1) {
            // d/dp [ln(p/(1-p))] = 1/[p(1-p)]
            d[1] = 1.0 / (p * (1.0 - p));
        }
        if (terms > 2) {
            // T2 = p-0.5 → dT2=1
            d[2] = 1.0;
        }
        if (terms > 3) {
            // T3 = (p-0.5)*ln(p/(1-p))
            double ln = FastMath.log(p/(1.0-p));
            // product rule: d[(p-0.5)]*ln + (p-0.5)*d[ln]
            d[3] = ln + (p - 0.5)*(1.0/(p*(1.0-p)));
        }
        if (terms > 4) {
            // T4 = (p-0.5)^2 → d = 2*(p-0.5)
            d[4] = 2.0*(p - 0.5);
        }
        if (terms > 5) {
            // T5 = T4 * ln(p/(1-p))
            double ln = FastMath.log(p/(1.0-p));
            // d[T4]*ln + T4*d[ln]
            double t4 = FastMath.pow(p-0.5,2);
            d[5] = 2.0*(p-0.5)*ln + t4*(1.0/(p*(1.0-p)));
        }
        // Additional derivatives (6,7,8…) follow same pattern
        return d;
    }
}
