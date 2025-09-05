package com.risquanter.metalog.estimate;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.Variable;

import com.risquanter.metalog.Metalog;

/**
 * Fits a Metalog quantile function to (pData[i], xData[i]) by
 * solving the QP
 *
 * minimize ∥Y·a – x∥² (Keelin’s original LS objective)
 * subject to dM/dp(gridP[k]) ≥ epsilon on a fine grid
 *
 * where M(p)=Σ_j a_j T_j(p), Y[i,j]=T_j(pData[i]), D[k,j]=dT_j/dp(gridP[k]).
 */
public class QPConstrainedFitter {

    private final double[] pData; // length K
    private final double[] xData; // length K
    private final int terms; // n = # of metalog terms
    private final double epsilon; // small positivity floor
    private final double[] gridP; // length G

    public QPConstrainedFitter(
            double[] pData,
            double[] xData,
            int terms,
            double epsilon,
            double[] gridP) {

        if (pData.length != xData.length) {
            throw new IllegalArgumentException("pData and xData must have same length");
        }
        Metalog.validateInputs(0.5, terms);

        this.pData = pData.clone();
        this.xData = xData.clone();
        this.terms = terms;
        this.epsilon = epsilon;
        this.gridP = gridP.clone();
    }

    public double[] fit() {

        // --- 1) Dimensions ---
        int K = pData.length; // # of data points
        int n = terms; // # of coefficients
        int G = gridP.length; // # of grid points for monotonicity

        // --- 2) Build basis‐matrix Y (K×n) ---
        double[][] Y = new double[K][n];
        for (int i = 0; i < K; i++) {
            double[] T = Metalog.basisFunctions(pData[i], n);
            for (int j = 0; j < n; j++) {
                Y[i][j] = T[j];
            }
        }

        // --- 3) Build quadratic objective: Q = YᵀY, c = –Yᵀx ---
        double[][] Q = new double[n][n];
        double[] c = new double[n];
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < n; j++) {
                double yij = Y[i][j];
                c[j] -= yij * xData[i];
                for (int k = 0; k < n; k++) {
                    Q[j][k] += yij * Y[i][k];
                }
            }
        }

        // --- 4) Build monotonicity‐matrix D (G×n) ---
        double[][] D = new double[G][n];
        for (int k = 0; k < G; k++) {
            double[] dT = Metalog.basisDerivatives(gridP[k], n);
            for (int j = 0; j < n; j++) {
                D[k][j] = dT[j];
            }
        }

        // --- 5) Set up ojAlgo model ---
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // 5a) Create the n free variables a0…a{n-1} (unbounded by default)
        Variable[] vars = new Variable[n];
        for (int j = 0; j < n; j++) {
            vars[j] = model.addVariable("a" + j);
        }

        // 5b) Inject the least‐squares objective: ½·aᵀQa + cᵀa
        Expression obj = model.addExpression("LS_OBJ")
                .weight(1.0); // include in objective

        // Linear part
        for (int j = 0; j < n; j++) {
            obj.set(vars[j], c[j]);
        }
        // new (correct) quadratic injection — restores the ½ in ½aᵀQ a
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                obj.set(vars[i], vars[j], 0.5 * Q[i][j]);
            }
        }

        // 5c) Add monotonicity constraints D[k]·a ≥ epsilon
        for (int k = 0; k < G; k++) {
            Expression cons = model.addExpression("mono_" + k)
                    .lower(epsilon);
            for (int j = 0; j < n; j++) {
                cons.set(vars[j], D[k][j]);
            }
        }

        // --- 6) Solve the QP (objective + constraints) ---
        model.minimise();

        // --- 7) Read back the fitted coefficients a_j ---
        double[] a = new double[n];
        for (int j = 0; j < n; j++) {
            a[j] = vars[j].getValue().doubleValue();
        }
        return a;
    }
}
