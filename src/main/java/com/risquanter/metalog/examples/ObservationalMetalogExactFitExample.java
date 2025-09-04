package com.risquanter.metalog.examples;
import java.util.Arrays;
import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.MetalogFitter;

public class ObservationalMetalogExactFitExample {
    public static void main(String[] args) {
        // 1) Raw observations
        double[] observations = {
            78, 65, 82, 90, 120, 150, 160, 140, 130, 110, 95, 85
        };
        int n = observations.length;
        
        // 2) Sort and compute p_i = (i - 0.5)/n
        Arrays.sort(observations);
        double[] pValues = new double[n];
        for (int i = 0; i < n; i++) {
            pValues[i] = (i + 0.5) / n;
        }
        double[] xValues = observations.clone();

        // 3) Fit an n‐term metalog (exact through each point)
        MetalogFitter fitter = new MetalogFitter(pValues, xValues, n);
        double[] coeffs = fitter.fit();

        // 4) Instantiate the fitted metalog
        Metalog metalog = new Metalog(coeffs);

        // 5) Recompute key quantiles
        System.out.println("Fitted quantiles from metalog:");
        for (double q : new double[]{0.10, 0.50, 0.90}) {
            double fitted = metalog.quantile(q);
            System.out.printf(" Q(%.2f) = %.1f mm%n", q, fitted);
        }

        // 6) Inspect the PDF at key p's
        System.out.println("\nPDF at selected probabilities:");
        for (double q : new double[]{0.01, 0.25, 0.50, 0.75, 0.99}) {
            System.out.printf(" f(%.2f) = %.4f%n", q, metalog.pdf(q));
        }

        // 7) Draw random samples for Monte Carlo
        java.util.Random rnd = new java.util.Random(12345);
        System.out.println("\n Five random draws from fitted metalog:");
        for (int i = 0; i < 5; i++) {
            double u = rnd.nextDouble();
            double draw = metalog.quantile(u);
            System.out.printf("  Sample %d: %.1f mm%n", i + 1, draw);
        }
    }
}
