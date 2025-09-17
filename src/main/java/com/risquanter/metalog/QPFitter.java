// File: QPFitter.java
package com.risquanter.metalog;


import java.util.stream.IntStream;

/**
 * Fluent Metalog QP-fitter that uses Keelin’s transform approach for bounds:
 * • unbounded: raw x
 * • lower-bounded: z = ln(x - L)
 * • upper-bounded: z = -ln(U - x)
 * • fully-bounded: z = logit((x - L)/(U - L))
 *
 * Internally always solves a pure monotonicity-only QP on z, then wraps
 * the raw unbounded Metalog to invert the transform in quantile(p).
 */
public class QPFitter {
  public static Options with(double[] p, double[] x, int terms) {
    return new Builder(p, x, terms);
  }

  public interface Options {
    Options epsilon(double eps);

    Options grid(double[] gridP);

    Options lower(double L);

    Options upper(double U);

    Metalog fit();
  }

  private static class Builder implements Options {
    private final double[] p, x;
    private final int terms;
    private double epsilon = 1e-6;
    private double[] gridP = null;
    private Double lower = null, upper = null;

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
      // 1) build default gridP on [ε … 1−ε]
      if (this.gridP == null) {
        int G = 100;
        double eps = 1e-12;
        this.gridP = IntStream.rangeClosed(0, G)
            .mapToDouble(i -> eps + (1 - 2 * eps) * i / (double) G)
            .toArray();
      }

      // 2) transform x→z
      double[] z = new double[x.length];
      if (lower != null && upper != null) {
        double L = lower, U = upper, span = U - L;
        for (int i = 0; i < x.length; i++) {
          double y = (x[i] - L) / span;
          z[i] = Math.log(y / (1 - y)); // logit
        }
      } else if (lower != null) {
        double L = lower;
        for (int i = 0; i < x.length; i++) {
          z[i] = Math.log(x[i] - L);
        }
      } else if (upper != null) {
        double U = upper;
        for (int i = 0; i < x.length; i++) {
          z[i] = -Math.log(U - x[i]);
        }
      } else {
        System.arraycopy(x, 0, z, 0, x.length);
      }

      // 3) fit an unbounded metalog on (p, z)
      QPUnboundedConstrainedFitter qp = new QPUnboundedConstrainedFitter(
          p, z, terms, epsilon, gridP);
      double[] a = qp.fit();
      Metalog mZ = new Metalog(a);

      // 4) wrap mZ to invert the transform
      return new Metalog(a) {
        @Override
        public double quantile(double p) {
          double zz = mZ.quantile(p);
          if (lower != null && upper != null) {
            double L = lower, U = upper;
            double frac = 1.0 / (1.0 + Math.exp(-zz)); // logistic
            return L + frac * (U - L);
          } else if (lower != null) {
            return lower + Math.exp(zz);
          } else if (upper != null) {
            return upper - Math.exp(-zz);
          } else {
            return zz;
          }
        }
      };
    }
  }
}
