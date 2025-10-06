# Metalog Distribution (Java)

This project provides an implementation of the Metalog distribution in Java, including:

- Basis functions for the quantile expansion
- `Metalog` class for quantile & PDF evaluation
- `QPFitter` class to fit a metalog via QP
- `Example` demonstrating fitting & LEC generation
- Unit tests with JUnit 5

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

## Getting Started

1. Clone the repo
2. Build: `mvn clean install`
3. Run example: `mvn exec:java -Dexec.mainClass="com.example.metalog.Example"`
