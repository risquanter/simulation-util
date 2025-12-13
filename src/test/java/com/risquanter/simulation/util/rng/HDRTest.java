/*
 * Copyright (C) 2025 Daniel Agota <danago@risquanter.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.risquanter.simulation.util.rng;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for HDR (Hash-based Discrete Random) number generator.
 * Validates implementation against reference Excel calculations.
 */
class HDRTest {

    private static final String SEPARATOR = ";";
    private static final String TEST_FILE = "testfile.txt";
    private static final double ERROR_MARGIN_EQ = 0.000000000000001;
    private static final double ERROR_MARGIN_LOGNORMAL = 0.00000001;

    /**
     * Data structure to hold test case values from the file
     */
    record TestData(int counter, double expectedHdr, double expectedLognormal) {}

    /**
     * Checks if two double values are equal within specified margin
     */
    private boolean eqWithinMargin(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    /**
     * Reads and parses test data from the resource file
     */
    private List<TestData> readTestData() throws IOException {
        List<TestData> testData = new ArrayList<>();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TEST_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            if (inputStream == null) {
                throw new IOException("Test file not found: " + TEST_FILE);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(SEPARATOR);
                if (parts.length == 3) {
                    int counter = Integer.parseInt(parts[0]);
                    double expectedHdr = Double.parseDouble(parts[1]);
                    double expectedLognormal = Double.parseDouble(parts[2]);
                    testData.add(new TestData(counter, expectedHdr, expectedLognormal));
                }
            }
        }

        return testData;
    }

    @Test
    void testHdrImplementationAgainstExcelImpl() throws IOException {
        HDR hdr = new HDR(1, 2, 3, 4);

        // Read test data
        List<TestData> hdrTestValues = readTestData();

        // Test HDR generation against expected values
        boolean allHdrTestsPass = true;
        for (TestData testCase : hdrTestValues) {
            double actualValue = hdr.trial(testCase.counter);
            if (!eqWithinMargin(actualValue, testCase.expectedHdr, ERROR_MARGIN_EQ)) {
                allHdrTestsPass = false;
                System.err.printf("HDR mismatch at counter %d: expected %.15f, got %.15f%n",
                    testCase.counter, testCase.expectedHdr, actualValue);
            }
        }

        System.out.printf("Read %d lines of HDR test data.%n", hdrTestValues.size());

        // Assert all conditions from original Scala test
        assertTrue(allHdrTestsPass, "HDR implementation should match Excel reference values");
        assertEquals(-1, (-3) % 2, "Java modulo behavior check: (-3) % 2 should equal -1");
        assertEquals(1, HDR.MOD(-3, 2), "Custom HDR.MOD(-3, 2) should equal 1");
    }

    @Test
    void testLognormalGenerationAgainstExcelImpl() throws IOException {
        // Create HDR instance and LogNormal distribution with same parameters as Scala test
        HDR hdr = new HDR(1, 2, 3, 4);
        LogNormalDistribution lognormal = new LogNormalDistribution(1, 1);

        // Read test data
        List<TestData> lognormalTestValues = readTestData();

        // Test lognormal generation against expected values
        boolean allLognormalTestsPass = true;
        for (TestData testCase : lognormalTestValues) {
            double hdrValue = hdr.trial(testCase.counter);
            double actualValue = lognormal.inverseCumulativeProbability(hdrValue);
            if (!eqWithinMargin(actualValue, testCase.expectedLognormal, ERROR_MARGIN_LOGNORMAL)) {
                allLognormalTestsPass = false;
                System.err.printf("Lognormal mismatch at counter %d: expected %.8f, got %.8f%n",
                    testCase.counter, testCase.expectedLognormal, actualValue);
            }
        }

        System.out.printf("Read %d lines of Lognormal test data.%n", lognormalTestValues.size());

        assertTrue(allLognormalTestsPass, "Lognormal generation should match Excel reference values");
    }

    @Test
    void testModFunctionBehavior() {
        // Test the MOD function behavior that's critical for Excel compatibility
        assertEquals(1, HDR.MOD(-3, 2), "MOD(-3, 2) should return 1");
        assertEquals(0, HDR.MOD(4, 2), "MOD(4, 2) should return 0");
        assertEquals(1, HDR.MOD(5, 2), "MOD(5, 2) should return 1");
        assertEquals(1, HDR.MOD(-1, 2), "MOD(-1, 2) should return 1");

        // Verify the MOD function handles negative numbers correctly
        // This is crucial for Excel compatibility
        assertTrue(HDR.MOD(-3, 2) > 0, "MOD with negative dividend should return positive result");
    }

    @Test
    void testHdrBasicFunctionality() {
        HDR hdr = new HDR(1, 2, 3, 4);

        // Test that the same inputs produce the same outputs (deterministic)
        double value1 = hdr.trial(100);
        double value2 = hdr.trial(100);
        assertEquals(value1, value2, "HDR should be deterministic");

        // Test different counters produce different values
        double valueA = hdr.trial(1);
        double valueB = hdr.trial(2);
        assertNotEquals(valueA, valueB, "Different counters should produce different values");

        // Test that output is in valid range [0, 1)
        for (int i = 1; i <= 1000; i++) {
            double value = hdr.trial(i);
            assertTrue(value >= 0.0 && value < 1.0,
                String.format("HDR output should be in [0,1), got %.15f", value));
        }
    }
}
