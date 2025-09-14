package com.risquanter.examples;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

import static com.risquanter.examples.ExampleUtil.loadResourceAsString;
import static com.risquanter.examples.ExampleUtil.buildObsJson;
import static com.risquanter.examples.ExampleUtil.buildFitJson;
import static com.risquanter.examples.ExampleUtil.writeToTestResource;
import static com.risquanter.examples.ExampleUtil.buildRulesJson;
import static com.risquanter.examples.ExampleUtil.buildLabelJson;

public class ExpertOpinionDemo1 {
    public static void main(String[] args) {

        String outputFilename = "vega-lite-expert-opinion-1.json";

        // 1) Expert’s quantiles
        double[] pVals = { 0.10, 0.50, 0.90 };
        double[] xVals = { 17.0, 24.0, 35.0 };
        int terms = pVals.length + 1; // = 4 for smoother fitting

        Double lowerBound = 16.0;
        Double upperBound = 40.0;

        Metalog metalog = QPFitter.with(pVals, xVals, terms).fit();

        Metalog boundedMetalog = QPFitter.with(pVals, xVals, terms).lower(lowerBound).upper(upperBound).fit();

        // 4) Build Vega-Lite JSON representation of the original data and for the
        // fitted CDF for visual comparison
        // with the original observations highlighted.
        String obsJson = buildObsJson(xVals, pVals);
        String fitJson = buildFitJson(metalog);
        String exactFitJson = buildFitJson(boundedMetalog);
        String labelJson = buildLabelJson(metalog, boundedMetalog);
        String rulesJson = buildRulesJson(lowerBound, upperBound);

        // 5) Load the stub template from test‐resources
        String template = loadResourceAsString("/vega-lite-expert-opinion-1-stub-twolines.json");

        // 6) Replace the two placeholders
        String spec = template
                .replace("\"PLACEHOLDER_OBS\"", obsJson)
                .replace("\"PLACEHOLDER_FIT_1\"", fitJson)
                .replace("\"PLACEHOLDER_FIT_2\"", exactFitJson)
                .replace("\"PLACEHOLDER_LABELS\"", labelJson)
                .replace("\"PLACEHOLDER_RULES\"", rulesJson)
                .replace("RULES_COLOR", "midnightblue");

        // 7) Print out the filled spec
        writeToTestResource(outputFilename, spec);

        System.out.println("Results written to: " + outputFilename);
    }

    
}
