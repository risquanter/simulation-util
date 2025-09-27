# Metalog Distribution (Java)

This project provides an implementation of the Metalog distribution in Java, including:

- Basis functions for the quantile expansion  
- `Metalog` class for quantile & PDF evaluation  
- `MetalogFitter` class to fit a metalog via OLS using Apache Commons Math  
- `Example` demonstrating fitting & LEC generation  
- Unit tests with JUnit 5  

## Getting Started

1. Clone the repo  
2. Build: `mvn clean install`  
3. Run example: `mvn exec:java -Dexec.mainClass="com.example.metalog.Example"`
