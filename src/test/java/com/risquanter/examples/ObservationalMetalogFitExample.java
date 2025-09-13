package com.risquanter.examples;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringJoiner;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

public class ObservationalMetalogFitExample {
    public static void main(String[] args) {

        // 1) Raw observations (n = 9)
        //double[] observations = { 78, 65, 82, 90, 120 /* ! */, 160, 143 /* ! */, /* 127 ,*/ 110, 85 };
        //double[] observations = { 78, 65, 82, 85,  160, 143 ,  127 , 110, 85 };
        double[] observations = { 78, 65, 82, 90, 160, 143 /* ! */, 127 , 110, 85 };
        int n = observations.length;
        Arrays.sort(observations);

        // 2) Compute plotting positions p_i = (i + 0.5) / n
        // Hazen plotting positions estimate the empirical CDF by assigning each sorted
        // observation a probability p in (0,1), using the formula p = (i + 0.5) / n.
        // We use them to generate valid p-values when they are not directly available
        // from expert estimates or simulation-based approximations.
        double[] pValues = new double[n];
        for (int i = 0; i < n; i++) {
            pValues[i] = (i + 0.5) / n; // Hazen plotting positions
        }

        // 3) Fit a metalog to 9 data points with 5 terms and 9 terms (exact fit)  
        // (more terms = more flexible, but also more complexity / chance for overfitting)
        int terms = 4;

        Metalog metalog = QPFitter.with(pValues, observations, terms).lower(0.0).upper(170.0).fit();

        Metalog exactMetalog = QPFitter.with(pValues, observations, 9).lower(0.0).upper(170.0).fit();

        // 4) Build Vega-Lite JSON representation of the original data and for the
        // fitted CDF for visual comparison
        // with the original observations highlighted.
        String obsJson = buildObsJson(observations, pValues);
        String fitJson = buildFitJson(metalog);
        String exactFitJson = buildFitJson(exactMetalog);

        // 5) Load the stub template from test‐resources
        String template = loadResourceAsString("/vega-lite-observational-stub-twolines.json");

        // 6) Replace the two placeholders
        String spec = template
                .replace("\"PLACEHOLDER_OBS\"", obsJson)
                .replace("\"PLACEHOLDER_FIT_1\"", fitJson)
                .replace("\"PLACEHOLDER_FIT_2\"", exactFitJson);

        // 7) Print out the filled spec
        System.out.println(spec);

    }

    private static String loadResourceAsString(String resourcePath) {
        InputStream in = ObservationalMetalogFitExample.class
                .getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }
        // Use Scanner trick to read the entire stream
        try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name())) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private static String buildObsJson(double[] observations, double[] pValues) {
        StringJoiner obsSj = new StringJoiner(",\n  ", "[\n  ", "\n]");
        for (int i = 0; i < observations.length; i++) {
            obsSj.add(String.format(
                    "{\"quantile\": %.1f, \"p\": %.3f}",
                    observations[i], pValues[i]));
        }
        String obsJson = obsSj.toString();
        return obsJson;
    }

    private static String buildFitJson(Metalog metalog) {
        int grid = 99;
        StringJoiner fitSj = new StringJoiner(",\n  ", "[\n  ", "\n]");
        for (int i = 1; i <= grid; i++) {
            double p = i / 100.0;
            double q = metalog.quantile(p);
            fitSj.add(String.format(
                    "{\"quantile\": %.3f, \"p\": %.2f}",
                    q, p));
        }
        String fitJson = fitSj.toString();
        return fitJson;
    }
}
