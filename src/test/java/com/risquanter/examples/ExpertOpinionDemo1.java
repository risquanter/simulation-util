package com.risquanter.examples;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import static com.risquanter.examples.ExampleUtil.loadResourceAsString;
import static com.risquanter.examples.ExampleUtil.buildObsJson;
import static com.risquanter.examples.ExampleUtil.buildRulesJson;
import static com.risquanter.examples.ExampleUtil.buildLabelJson;
import static com.risquanter.examples.ExampleUtil.buildFitJson;;

public class ExpertOpinionDemo1 {
    private static final double EPS   = 1e-12;  // must match ε-anchors in the fitter


    public static void main(String[] args) {
        String outputFilename = "vega-lite-expert-opinion-1.json";

        // 1) Expert’s quantiles
        double[] pVals = {0.10, 0.50, 0.90};
        double[] xVals = {17.0, 24.0, 35.0};
        int terms      = pVals.length;  

        Double lowerBound = 16.0;
        Double upperBound = 40.0;

        // 2) Fit two Metalogs: unbounded and bounded
        Metalog metalogUnbounded = QPFitter
            .with(pVals, xVals, terms)
            .fit();

        Metalog metalogBounded = QPFitter
            .with(pVals, xVals, terms)
            .lower(lowerBound)
            .upper(upperBound)
            .fit();

        // 3) Build Vega-Lite JSON snippets
        String obsJson      = buildObsJson(xVals, pVals);
        String fitJsonUnb   = buildFitJson(metalogUnbounded);
        String fitJsonBd    = buildFitJson(metalogBounded);
        String labelJson    = buildLabelJson(metalogUnbounded, metalogBounded);
        String rulesJson    = buildRulesJson(lowerBound, upperBound);

        // 4) Load template and replace placeholders
        String template = loadResourceAsString(
            "/vega-lite-expert-opinion-1-stub-twolines.json"
        );
        String spec = template
            .replace("\"PLACEHOLDER_OBS\"",    obsJson)
            .replace("\"PLACEHOLDER_FIT_1\"",  fitJsonUnb)
            .replace("\"PLACEHOLDER_FIT_2\"",  fitJsonBd)
            .replace("\"PLACEHOLDER_LABELS\"", labelJson)
            .replace("\"PLACEHOLDER_RULES\"",  rulesJson)
            .replace("RULES_COLOR",            "midnightblue");

        // 5) Write the filled spec out
        writeToTestResource(outputFilename, spec);
        System.out.println("Results written to: " + outputFilename);

        // 6) Verify that the bounded fit is pinned at the ends
        System.out.printf("Q(eps)    = %.12f%n", metalogBounded.quantile(EPS));
        System.out.printf("Q(1-eps)  = %.12f%n", metalogBounded.quantile(1 - EPS));
    }

    

    /**
     * Writes `content` to src/test/resources/`filename`, creating
     * directories as needed.
     */
    public static void writeToTestResource(String filename, String content) {
        File file = Paths.get("src", "test", "resources", filename).toFile();
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("❌ Failed to write Vega-Lite spec to file: "
                               + file.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
