# Overview

This library's goal is to provide a flexible probability modeling toolkit based on Keelin’s Metalog system [1] and the HDR pseudo-random number generator (PRNG) proposed by Hubbard [2].

# Metalog Distribution (Java)

This library offers the following functionality:

- Basis functions for quantile expansion
- `Metalog` class for quantile and PDF evaluation
- `QPFitter` class to fit a Metalog distribution via quadratic programming (QP) using the Ojalgo library

The implementation follows the referenced paper for quantile and PDF evaluation. Instead of ordinary least squares (OLS), the current version uses a quadratic programming (QP) approach for fitting, with a linear constraint on the probabilities.

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

To generate quantiles for a full range of probabilities $p$ from $0$ to $1$, use evenly spaced $p$ values, but ensures that $p$ never reaches the exact endpoints $0$ or $1$. Instead, $p$ is clamped to the interval $(\text{EPS}, 1-\text{EPS})$, where `EPS` is a very small number (e.g., $10^{-12}$):

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

# HDR (not yet included in the current version)

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

## Getting Started

1. Clone the repository
2. Build: `mvn clean install`
3. Run example: `mvn exec:java -Dexec.mainClass="com.risquanter.examples.Example"`

## References
[1] T. W. Keelin, "The Metalog Distributions," *Decision Analysis*, vol. 13, no. 4, pp. 243–277, Dec. 2016. [Online]. Available: https://doi.org/10.1287/deca.2016.0338
[2] D. W. Hubbard, "A Multi-Dimensional, Counter-Based Pseudo Random Number Generator as a Standard for Monte Carlo Simulations," in *Proc. Winter Simulation Conf.*, N. Mustafee, K.-H. G. Bae, S. Lazarova-Molnar, M. Rabe, C. Szabo, P. Haas, and Y.-J. Son, Eds., IEEE, 2019, pp. 3064–3073. [Online]. Available: https://www.informs-sim.org/wsc19papers/339.pdf
