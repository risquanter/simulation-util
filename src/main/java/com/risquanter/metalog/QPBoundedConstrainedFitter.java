package com.risquanter.metalog;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.Variable;

/**
 * QP-Bounded Metalog Fitter
 *
 * Metalog Basis Functions
 * ------------------------
 * We represent the quantile function Q(p) as a linear combination of n basis
 * functions T_j(p):
 *
 *     Q(p) = Σ_{j=0..n-1} a_j · T_j(p)
 *
 * where the T_j are the “metalog” basis polynomials in the probability p.
 * Common choices (n≥3) include
 *   T₀(p) = 1
 *   T₁(p) = logit(p)           = ln(p/(1−p))
 *   T₂(p) = p − 0.5
 *   T₃(p) = (p − 0.5)·logit(p)
 *   T₄(p) = logit(p)² − π²/3
 *   …and higher-order combinations for added flexibility.
 *
 *
 * Monotonicity Constraint (dQ/dp ≥ ε)
 * -----------------------------------
 * We enforce that Q(p) be strictly increasing by requiring its derivative
 * on a grid of G points pₖ satisfy:
 *
 *     dQ/dp (pₖ) = Σ_j a_j · Tʹ_j(pₖ)  ≥ ε
 *
 * where ε>0 is a tiny regularization constant (e.g. 1e−6). This turns the
 * monotonicity condition into G linear constraints in the QP:
 *
 *     D[k]·a ≥ ε,   D[k,j] = Tʹ_j(pₖ)
 *
 *
 * Lower Bound Constraint (optional)
 * ---------------------------------
 * If a lower bound L is supplied, we add for each grid point pₖ:
 *
 *     Q(pₖ) = Σ_j a_j · T_j(pₖ)  ≥ L
 *
 * guaranteeing the fitted quantile never falls below L on [0,1].
 *
 *
 * Upper Bound Constraint (optional)
 * ---------------------------------
 * If an upper bound U is supplied, we similarly add:
 *
 *     Q(pₖ) = Σ_j a_j · T_j(pₖ)  ≤ U
 *
 * guaranteeing the fitted quantile never exceeds U on [0,1].
 *
 *
 * Putting It All Together
 * -----------------------
 * The QP we solve is:
 *
 *   minimize    ½‖Y·a − xData‖²      (least-squares fit at data points)
 *   subject to  D·a  ≥ ε             (monotonicity)
 *               T·a  ≥ L  (if L≠null) (lower bound)
 *               T·a  ≤ U  (if U≠null) (upper bound)
 *
 * where:
 *   • Y[i,j] = T_j(pData[i])         (basis at your observed quantiles)
 *   • D[k,j] = Tʹ_j(gridP[k])        (derivatives on the enforce grid)
 *   • T[k,j] = T_j(gridP[k])         (basis on the enforce grid)
 */
class QPBoundedConstrainedFitter {

    private final double[] pData;    // length K
    private final double[] xData;    // length K
    private final int    terms;      // n = # of metalog terms
    private final double epsilon;    // monotonicity floor
    private final double[] gridP;    // length G
    private final Double lowerBound; // optional
    private final Double upperBound; // optional

    QPBoundedConstrainedFitter(
            double[] pData,
            double[] xData,
            int terms,
            double epsilon,
            double[] gridP,
            Double lowerBound,
            Double upperBound) {

        if (pData.length != xData.length) {
            throw new IllegalArgumentException("pData and xData must have same length");
        }
        Metalog.validateInputs(0.5, terms);

        this.pData     = pData.clone();
        this.xData     = xData.clone();
        this.terms     = terms;
        this.epsilon   = epsilon;
        this.gridP     = gridP.clone();
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    double[] fit() {

        final int K = pData.length;
        final int n = terms;
        final int G = gridP.length;

        // 1) Build Y (K×n)
        double[][] Y = new double[K][n];
        for (int i = 0; i < K; i++) {
            double[] T = Metalog.basisFunctions(pData[i], n);
            System.arraycopy(T, 0, Y[i], 0, n);
        }

        // 2) Build Q and c for ½ aᵀQa + cᵀa
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

        // 3) Build D (G×n) for dM/dp ≥ epsilon
        double[][] D = new double[G][n];
        for (int k = 0; k < G; k++) {
            double[] dT = Metalog.basisDerivatives(gridP[k], n);
            System.arraycopy(dT, 0, D[k], 0, n);
        }

        // 4) Setup ojAlgo model
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable[] vars = new Variable[n];
        for (int j = 0; j < n; j++) {
            vars[j] = model.addVariable("a" + j);
        }

        // 5) Inject LS objective
        Expression obj = model.addExpression("LS_OBJ").weight(1.0);
        for (int j = 0; j < n; j++) {
            obj.set(vars[j], c[j]);
            for (int k = 0; k < n; k++) {
                obj.set(vars[j], vars[k], 0.5 * Q[j][k]);
            }
        }

        // 6) Monotonicity: D·a ≥ epsilon
        for (int k = 0; k < G; k++) {
            Expression con = model.addExpression("mono_" + k).lower(epsilon);
            for (int j = 0; j < n; j++) {
                con.set(vars[j], D[k][j]);
            }
        }

        // 7) Lower‐bound: M(p) ≥ lowerBound
        if (lowerBound != null) {
            for (int k = 0; k < G; k++) {
                double[] T = Metalog.basisFunctions(gridP[k], n);
                Expression lb = model.addExpression("lb_" + k).lower(lowerBound);
                for (int j = 0; j < n; j++) {
                    lb.set(vars[j], T[j]);
                }
            }
        }

        // 8) Upper‐bound: M(p) ≤ upperBound
        if (upperBound != null) {
            for (int k = 0; k < G; k++) {
                double[] T = Metalog.basisFunctions(gridP[k], n);
                Expression ub = model.addExpression("ub_" + k).upper(upperBound);
                for (int j = 0; j < n; j++) {
                    ub.set(vars[j], T[j]);
                }
            }
        }

        // 9) Solve
        model.minimise();

        // 10) Read back coefficients
        double[] a = new double[n];
        for (int j = 0; j < n; j++) {
            a[j] = vars[j].getValue().doubleValue();
        }
        return a;
    }
}
