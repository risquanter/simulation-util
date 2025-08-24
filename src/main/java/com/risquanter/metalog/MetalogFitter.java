package com.risquanter.metalog;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

/**
 * Fits a metalog distribution to (p_i, x_i) quantile points.
 */
public class MetalogFitter {
    private final double[] pValues;
    private final double[] xValues;
    private final int terms;

    /**
     * @param pValues cumulative probabilities (length K)
     * @param xValues quantile values at pValues (length K)
     * @param terms   number of metalog terms (n)
     */
    public MetalogFitter(double[] pValues, double[] xValues, int terms) {
        if (pValues.length != xValues.length) {
            throw new IllegalArgumentException("Lengths of p and x must match");
        }
        this.pValues = pValues.clone();
        this.xValues = xValues.clone();
        this.terms = terms;
    }

    /** Fit via ordinary least squares: returns coefficients a_j */
    public double[] fit() {
        int K = pValues.length;
        RealMatrix T = new Array2DRowRealMatrix(K, terms);
        for (int i = 0; i < K; i++) {
            double[] row = Metalog.basisFunctions(pValues[i], terms);
            T.setRow(i, row);
        }
        RealVector x = new ArrayRealVector(xValues);
        RealVector a = new QRDecomposition(T).getSolver().solve(x);
        return a.toArray();
    }
}
