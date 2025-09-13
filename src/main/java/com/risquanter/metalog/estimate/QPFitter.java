package com.risquanter.metalog.estimate;

import java.util.stream.IntStream;

import com.risquanter.metalog.Metalog;

/**
 * Fluent builder for all QP‐based Metalog fitters:
 * – Unbounded (default)
 * – Lower‐bounded
 * – Upper‐bounded
 * – Fully‐bounded
 */
public class QPFitter {
  public static Options with(double[] p, double[] x, int terms) {
    return new Builder(p, x, terms);
  }

  /**
   * Stage that collects all optional parameters.
   * You can call these in any order, any number of times.
   */
  public interface Options {
    /** regularization epsilon (default 1e-6) */
    Options epsilon(double eps);

    /**
     * grid of p's at which to enforce bounds
     * (default = 1/100…99/100)
     */
    Options grid(double[] gridP);

    /** enforce Q(p) ≥ L */
    Options lower(double L);

    /** enforce Q(p) ≤ U */
    Options upper(double U);

    /** finalize and fit */
    Metalog fit();
  }

  private static class Builder implements Options {
    private final double[] p, x;
    private final int terms;

    // defaults
    private double epsilon = 1e-6;
    //default value set in fit() if null
    private double[] gridP = null;
    //optional, the Fitter constructor accepts nulls
    private Double lower = null;
    private Double upper = null;

    //mandatory params in constructor
    Builder(double[] p, double[] x, int terms) {
      this.p = p;
      this.x = x;
      this.terms = terms;
    }

    @Override
    public Options epsilon(double eps) {
      this.epsilon = eps;
      return this;
    }

    @Override
    public Options grid(double[] gridP) {
      this.gridP = gridP;
      return this;
    }

    @Override
    public Options lower(double L) {
      this.lower = L;
      return this;
    }

    @Override
    public Options upper(double U) {
      this.upper = U;
      return this;
    }

    @Override
    public Metalog fit() {
      if (this.gridP == null) {
        this.gridP = IntStream.rangeClosed(1, 99)
            .mapToDouble(i -> i / 100.0)
            .toArray();
      }

      QPBoundedConstrainedFitter fitter = new QPBoundedConstrainedFitter(
          p, x, terms, epsilon, gridP, lower, upper);

      double[] coeffs = fitter.fit();

      Metalog metalog = new Metalog(coeffs);
      return metalog;
    }
  }
}
