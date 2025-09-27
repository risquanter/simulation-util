package com.risquanter.examples;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.util.Arrays;
import java.util.Random;


public class MixtureSamplingComparison {
    // --- helper to capture quantiles at fixed p's ---
    static class QuantileResult {
        final double[] p, x;

        QuantileResult(double[] p, double[] x) {
            this.p = p;
            this.x = x;
        }
    }

    // 1) fixed probabilities at which we compare quantiles
    private static final double[] FIXED_P = {
            0.001, 0.02, 0.1, 0.25, 0.5, 0.75, 0.9, 0.98, 0.999
    };

    // 2) extract empirical quantiles from a sample array
    private static QuantileResult extractFixedQuantiles(double[] losses) {
        int sims = losses.length;
        Arrays.sort(losses);
        int K = FIXED_P.length;

        double[] p = new double[K], x = new double[K];
        for (int i = 0; i < K; i++) {
            p[i] = FIXED_P[i];
            // floor(p * sims) gives us the “p‐th” order statistic
            int idx = (int) Math.floor(p[i] * sims);
            x[i] = losses[Math.min(idx, sims - 1)];
        }
        return new QuantileResult(p, x);
    }

    // 3) zero‐inflated lognormal mixture
    static class ZeroInflatedLognormal {
        private final double pLoss, pNoLoss;
        private final LogNormalDistribution lnDist;

        ZeroInflatedLognormal(double mu, double sigma, double pLoss) {
            this.pLoss = pLoss;
            this.pNoLoss = 1.0 - pLoss;
            this.lnDist = new LogNormalDistribution(mu, sigma);
        }

        /**
         * Inverse CDF of the mixture:
         * returns 0 if p ≤ 1−pLoss,
         * otherwise rescales into the lognormal inverse‐CDF.
         */
        public double inverseCumulativeProbability(double p) {
            if (p <= pNoLoss) {
                return 0.0;
            }
            double scaledP = (p - pNoLoss) / pLoss;
            return lnDist.inverseCumulativeProbability(scaledP);
        }
    }

    public static void main(String[] args) {
        // —————————————————————————————————————————————————
        // A) convert [100,200] @80% bounds into a lognormal(mu,sigma)
        double lossLB = 100;
        double lossUB = 200;
        double meanLog = (Math.log(lossLB) + Math.log(lossUB)) / 2.0;
        double varLog = Math.pow((Math.log(lossUB) - Math.log(lossLB)) / 3.29, 2);
        double sigma = Math.sqrt(varLog);
        double mu = meanLog;

        // 10% chance of a loss each year
        double pLoss = 0.1;

        ZeroInflatedLognormal mix = new ZeroInflatedLognormal(mu, sigma, pLoss);

        // —————————————————————————————————————————————————
        // B) Monte‐Carlo sample the annual loss via direct Bernoulli+lnDist.sample()
        int sims = 10_000;
        // 1) Seed a RandomGenerator once
        long seed = 42L;
        RandomGenerator rng = new Well19937c(seed);

        // 2) Pass it into the distribution
        LogNormalDistribution lnDist = new LogNormalDistribution(rng, mu, sigma);
        Random rnd = new Random(42);

        double[] lossesMC = new double[sims];
        for (int i = 0; i < sims; i++) {
            if (rnd.nextDouble() < pLoss) {
                lossesMC[i] = lnDist.sample();
            } else {
                lossesMC[i] = 0.0;
            }
        }

        // C) compute empirical quantiles from the MC run
        QuantileResult qrMC = extractFixedQuantiles(lossesMC);
        double[] pEmpirical = qrMC.p;
        double[] xEmpirical = qrMC.x;

        // D) compute analytic quantiles from the mixture inverse‐CDF
        int K = pEmpirical.length;
        double[] xAnalytic = new double[K];
        for (int i = 0; i < K; i++) {
            xAnalytic[i] = mix.inverseCumulativeProbability(pEmpirical[i]);
        }

        // —————————————————————————————————————————————————
        // E) print side-by-side comparison
        System.out.println("[");
        for (int i = 0; i < K; i++) {
            System.out.printf(
                    "  {\"p\": %.3f, \"analytic\": %.4f, \"empirical\": %.4f}%s%n",
                    pEmpirical[i],
                    xAnalytic[i],
                    xEmpirical[i],
                    (i < K - 1 ? "," : ""));
        }
        System.out.println("]");

      
    }
}
