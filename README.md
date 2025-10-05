# Metalog Distribution (Java)

This project provides an implementation of the Metalog distribution in Java, including:

- Basis functions for the quantile expansion
- `Metalog` class for quantile & PDF evaluation
- `MetalogFitter` class to fit a metalog via OLS using Apache Commons Math
- `Example` demonstrating fitting & LEC generation
- Unit tests with JUnit 5

## Installation

Add this dependency to your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.risquanter</groupId>
    <artifactId>java-metalog-distribution</artifactId>
    <version>0.6.0</version>
</dependency>
```

## Quick Start

```java
import com.risquanter.metalog.Metalog;
import com.risquanter.metalog.MetalogFitter;

// Create sample data
double[] y = {1.0, 2.0, 3.0, 4.0, 5.0};

// Fit the metalog distribution
MetalogFitter fitter = new MetalogFitter(y);
Metalog metalog = fitter.fit(4); // 4 terms

// Calculate quantiles
double q = metalog.quantile(0.5); // median
double pdf = metalog.pdf(q); // PDF at median
```

## Contributing

We welcome contributions! Please note that all contributors must sign our Contributor License Agreement (CLA) before we can accept any contributions. The CLA signing process is automated and will be initiated when you submit a pull request.

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

## Getting Started

1. Clone the repo
2. Build: `mvn clean install`
3. Run example: `mvn exec:java -Dexec.mainClass="com.example.metalog.Example"`
