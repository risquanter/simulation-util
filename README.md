# Overview
This library provides a low-level yet flexible probability modeling toolkit built on Keelin’s Metalog distribution system [1] and the HDR pseudo-random number generator (PRNG) proposed by Hubbard [2].

It is designed for cybersecurity practitioners, tool providers, and decision scientists who want to accurately represent risks and translate expert judgment into quantitative models. By using structured confidence levels—often expressed as probability triplets (e.g., with 10% certainty losses will be below 2 million, with 50% certainty below 16 million, and with 90% certainty below 20 million)—experts can articulate their assesments in a way that is both intuitive and mathematically rigorous.

The key advantage of this toolkit is that it enables precise, simulation-based quantitative risk assessment and aggregation on the JVM without requiring experts to master complex mathematics. This makes it possible to incorporate expert elicitation directly into robust probabilistic models, supporting better-informed decisions in high-stakes domains such as cybersecurity and enterprise risk management.

# Metalog Distribution (Java)

This library offers the following functionality:

The basic metalog functionality (basis functions for quantile expansion) as defined in the original papers and a QP fitter.

The library also provides a flexible yet simple builder API consisting of:    
- `Metalog.java` class for quantile and PDF evaluation
- `QPFitter.java` class to fit a Metalog distribution via quadratic programming (QP) using the Ojalgo library

The implementation follows the referenced paper for quantile and PDF evaluation. However, instead of ordinary least squares (OLS), the current version uses a quadratic programming (QP) approach for fitting, with convex feasibility constraints (such as monotonicity and bounds) on the coefficients. This is motivated by Keelin’s proof that the set of feasible Metalog coefficients is convex, making convex optimization (QP) both robust and theoretically justified for Metalog fitting.

# Usage

## Metalog Fitting

```java
// 1) Expert quantiles
double[] pVals = {0.10, 0.50, 0.90};
double[] xVals = {17.0, 24.0, 35.0};
int terms = pVals.length;

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
```

## Generating a Quantile Trace

To generate quantiles for a full range of probabilities \(p\) from \(0\) to \(1\), use evenly spaced \(p\) values.  
Make sure \(p\) never reaches the exact endpoints \(0\) or \(1\).  
Instead, clamp \(p\) to the interval $(\text{EPS}, 1-\text{EPS})$, where `EPS` is a very small number (e.g., $10^{-12}$):

```java
double pForQ = Math.min(Math.max(p, EPS), 1.0 - EPS);

int steps = 100; // or use ExampleUtil.STEPS
double eps = 1e-12; // or use ExampleUtil.EPS

for (int i = 0; i <= steps; i++) {
    double p = i / (double) steps;
    double pClamped = Math.min(Math.max(p, eps), 1.0 - eps);
    // Use pClamped for quantile calculation
    System.out.printf("p: %.5f, pClamped: %.12f%n", p, pClamped);
}
```

> **Note:**
> Clamping $p$ to $(\text{EPS}, 1-\text{EPS})$ is necessary because the Metalog implementation requires $p$ to be strictly within $(0,1)$.
> This avoids undefined behavior in the quantile and PDF calculations, such as division by zero or logarithm of zero, which can occur at the endpoints.
> Always clamp $p$ before calling `quantile(p)` or `pdf(p)` to ensure numerical safety.

Various examples under `src/test/java/com/risquanter/examples` demonstrating fitting and practical use-cases, such as working with risk hierarchies expressed as loss exceedance curves (LEC). The above snippets are from

# HDR Pseudo-Random Number Generator (Java)
This library now includes an implementation of the HDR pseudo-random number generator (PRNG) proposed by Douglas Hubbard [2]. The HDR PRNG designed by Hubbard [2] is a multi-dimensional, counter-based generator that produces reproducible random sequences across platforms and programming languages. Unlike traditional serial PRNGs (e.g., Mersenne Twister), which rely on iterative state updates, the HDR approach uses explicit counters for each dimension of a simulation, enabling direct access to any trial without computing all preceding values.

## Key Features
Counter-Based Design Each random number is generated directly from a counter (trial ID, variable ID, entity ID, time ID, agent ID). This allows “random access” to any scenario without sequential iteration, making it highly efficient for large-scale simulations.

Multi-Dimensional Support The generator supports up to five independent dimensions:

- Trial ID (unique scenario identifier)
- Variable ID (unique identifier for each random variable)
- Entity ID (organization or model context)
- Time ID (time series index for temporal simulations)
- Agent ID (optional dimension for agent-based models)

These dimensions ensure that random sequences remain independent and reproducible across different modeling contexts.

## Cross-Platform Consistency 
The HDR PRNG is algebraically defined to produce identical results in Java, Python, R, Excel, and other environments, regardless of differences in precision or implementation details. This makes it suitable for collaborative modeling across diverse toolchains.

## Statistical Quality 
The HDR PRNG has been validated against the Dieharder test suite (114 tests for randomness) and performs comparably to industry-standard generators such as Mersenne Twister. It achieves strong coverage of the 32-bit integer space and passes demanding randomness tests, ensuring reliability for Monte Carlo simulations.

## Scalability 
Capable of generating millions of scenarios across millions of variables and entities, while maintaining reproducibility and independence of random streams. This makes it practical for interactive risk modeling and large-scale enterprise simulations.

## Implementation Notes
- The Java implementation follows the structure defined in Hubbard’s paper, using nested modulus operations with carefully chosen prime coefficients to ensure randomness quality and compliance with Excel’s precision limits.
- The Java implementation's correctness is tested against the Excel implementaiton.
- Each dimension is mapped to a prime multiplier, ensuring unique sequences for different combinations of trial, variable, entity, time, and agent identifiers.
- The output is normalized to the interval [0,1], making it directly usable for probability-based sampling and integration with distributions such as Metalog.
- Because the generator is counter-based, parallelization is straightforward: different processors can generate disjoint ranges of scenarios without overlap or correlation.

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

## Getting Started

1. Clone the repository
2. Build: `mvn clean install`
3. Run the example from the command line:
   ```bash
   mvn exec:java -Dexec.mainClass="com.risquanter.examples.ExpertOpinionDemo1"
   ```


> **Note:**
> To keep the compiled JAR lean, all example classes are located under `src/test/java` and are not included in the main build artifact.
> If you want to run the examples without performing a full `mvn clean install`, you can compile the test sources with:
> ```bash
> mvn clean test
> ```
> This will compile the test classes, allowing you to run the examples with `mvn exec:java -Dexec.mainClass="..."` as shown above.

## References
[1] T. W. Keelin, "The Metalog Distributions," *Decision Analysis*, vol. 13, no. 4, pp. 243–277, Dec. 2016. [Online]. Available: https://doi.org/10.1287/deca.2016.0338

[2] D. W. Hubbard, "A Multi-Dimensional, Counter-Based Pseudo Random Number Generator as a Standard for Monte Carlo Simulations," in *Proc. Winter Simulation Conf.*, N. Mustafee, K.-H. G. Bae, S. Lazarova-Molnar, M. Rabe, C. Szabo, P. Haas, and Y.-J. Son, Eds., IEEE, 2019, pp. 3064–3073. [Online]. Available: https://www.informs-sim.org/wsc19papers/339.pdf
