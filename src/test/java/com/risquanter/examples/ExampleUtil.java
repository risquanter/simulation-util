package com.risquanter.examples;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.StringJoiner;

import com.risquanter.metalog.Metalog;

public class ExampleUtil {

    static String loadResourceAsString(String resourcePath) {
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

    static String buildObsJson(double[] observations, double[] pValues) {
        StringJoiner obsSj = new StringJoiner(",\n  ", "[\n  ", "\n]");
        for (int i = 0; i < observations.length; i++) {
            obsSj.add(String.format(
                    "{\"quantile\": %.1f, \"p\": %.3f}",
                    observations[i], pValues[i]));
        }
        String obsJson = obsSj.toString();
        return obsJson;
    }

    static String buildFitJson(Metalog metalog) {
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

    public static void writeToTestResource(String filename, String content) {
        File file = Paths.get("src", "test", "resources", filename).toFile();

        try {
            // Ensure parent directories exist
            file.getParentFile().mkdirs();

            // Write content to file (overwrite if exists)
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write(content);
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to write Vega-Lite spec to file: " + file.getAbsolutePath());
            System.err.println("Reason: " + e.getMessage());
            // Optionally rethrow or log more details
        }
    }

    static String buildRulesJson(double lower, double upper) {

        StringJoiner rulesSj = new StringJoiner(",\n  ", "[\n  ", "\n]");

        rulesSj.add(String.format(
                    "{\"x\": %.3f}",
                    lower));
        rulesSj.add(String.format(
                    "{\"x\": %.3f}",
                    upper));
        
        String rulesJson = rulesSj.toString();
        return rulesJson;
    }

    static String buildLabelJson(Metalog metalog, Metalog boundedMetalog) {

        StringJoiner labelSj = new StringJoiner(",\n  ", "[\n  ", "\n]");

        var pBounded = 0.95;
        var qBounded = boundedMetalog.quantile(pBounded);
        var pUnbounded = 0.05;
        var qUnbounded = metalog.quantile(pUnbounded);
        labelSj.add(String.format(
                    "{\"quantile\": %.3f, \"p\": %.2f, \"label\": \"bounded\", \"color\": \"midnightblue\"}",
                    qBounded, pBounded));
        labelSj.add(String.format(
                    "{\"quantile\": %.3f, \"p\": %.2f, \"label\": \"unbounded\", \"color\": \"darkred\"}",
                    qUnbounded, pUnbounded));
        
        String labelJson = labelSj.toString();
        return labelJson;
    }

}
