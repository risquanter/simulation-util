// File: QPUnboundedConstrainedFitter.java
package com.risquanter.metalog;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.Variable;


/**
 * Internal QP that only enforces strict monotonicity (dQ/dp ≥ ε)
 * on an unbounded metalog basis.  No bound constraints.
 */
class QPUnboundedConstrainedFitter {
    private final double[] pData, xData;
    private final int terms;
    private final double epsilon;
    private final double[] gridP;

    QPUnboundedConstrainedFitter(
        double[] pData,
        double[] xData,
        int terms,
        double epsilon,
        double[] gridP
    ) {
        if (pData.length != xData.length) {
            throw new IllegalArgumentException("pData and xData must match");
        }
        Metalog.validateInputs(0.5, terms);
        this.pData   = pData.clone();
        this.xData   = xData.clone();
        this.terms   = terms;
        this.epsilon = epsilon;
        this.gridP   = gridP.clone();
    }

    double[] fit() {
        int K = pData.length, n = terms, G = gridP.length;

        // 1) Y matrix (K×n)
        double[][] Y = new double[K][n];
        for (int i = 0; i < K; i++) {
            double[] T = Metalog.basisFunctions(pData[i], n);
            System.arraycopy(T, 0, Y[i], 0, n);
        }

        // 2) Build Q (n×n) and c (n) for ½ aᵀQa + cᵀa
        double[][] Q = new double[n][n];
        double[]   c = new double[n];
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < n; j++) {
                double yij = Y[i][j];
                c[j]    -= yij * xData[i];
                for (int k = 0; k < n; k++) {
                    Q[j][k] += yij * Y[i][k];
                }
            }
        }

        // 3) Derivative matrix D (G×n)
        double[][] D = new double[G][n];
        for (int k = 0; k < G; k++) {
            double[] dT = Metalog.basisDerivatives(gridP[k], n);
            System.arraycopy(dT, 0, D[k], 0, n);
        }

        // 4) Build ojAlgo model
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable[] vars = new Variable[n];
        for (int j = 0; j < n; j++) {
            vars[j] = model.addVariable("a" + j);
        }

        // 5) Least‐squares objective
        Expression obj = model.addExpression("LS").weight(1.0);
        for (int j = 0; j < n; j++) {
            obj.set(vars[j], c[j]);
            for (int k = 0; k < n; k++) {
                obj.set(vars[j], vars[k], 0.5 * Q[j][k]);
            }
        }

        // 6) Monotonicity: D·a ≥ ε
        for (int k = 0; k < G; k++) {
            Expression con = model.addExpression("mono_" + k).lower(epsilon);
            for (int j = 0; j < n; j++) {
                con.set(vars[j], D[k][j]);
            }
        }

        // 7) Solve
        model.minimise();

        // 8) Return coefficients
        double[] a = new double[n];
        for (int j = 0; j < n; j++) {
            a[j] = vars[j].getValue().doubleValue();
        }
        return a;
    }
}
