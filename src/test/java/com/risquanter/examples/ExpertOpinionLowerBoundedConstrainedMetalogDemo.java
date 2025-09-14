package com.risquanter.examples;

import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.QPFitter;

import static com.risquanter.examples.ExampleUtil.loadResourceAsString;
import static com.risquanter.examples.ExampleUtil.buildObsJson;
import static com.risquanter.examples.ExampleUtil.buildFitJson;
import static com.risquanter.examples.ExampleUtil.writeToTestResource;

public class ExpertOpinionLowerBoundedConstrainedMetalogDemo {
    public static void main(String[] args) {

        String outputFilename = "vega-lite-expert-opinion.json";

        // 1) Expert’s quantiles
        double[] pVals = { 0.10, 0.50, 0.90 };
        double[] xVals = { 17.0, 24.0, 35.0 };
        int terms = pVals.length + 1; // = 4 for smoother fitting

        Double lowerBound = 16.0;
        Double upperBound = 40.0;

        Metalog metalog = QPFitter.with(pVals, xVals, terms).fit();

        Metalog boundedMetalog = QPFitter.with(pVals, xVals, 9).lower(lowerBound).upper(upperBound).fit();

        // 4) Build Vega-Lite JSON representation of the original data and for the
        // fitted CDF for visual comparison
        // with the original observations highlighted.
        String obsJson = buildObsJson(xVals, pVals);
        String fitJson = buildFitJson(metalog);
        String exactFitJson = buildFitJson(boundedMetalog);

        // 5) Load the stub template from test‐resources
        String template = loadResourceAsString("/vega-lite-expert-opinion-stub-twolines.json");

        // 6) Replace the two placeholders
        String spec = template
                .replace("\"PLACEHOLDER_OBS\"", obsJson)
                .replace("\"PLACEHOLDER_FIT_1\"", fitJson)
                .replace("\"PLACEHOLDER_FIT_2\"", exactFitJson);

        // 7) Print out the filled spec
        writeToTestResource(outputFilename, spec);

        System.out.println("Results written to: " + outputFilename);
    }
}
