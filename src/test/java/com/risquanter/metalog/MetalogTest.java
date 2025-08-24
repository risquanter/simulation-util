package com.risquanter.metalog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MetalogTest {

    @Test
    void testQuantileAndPDF() {
        // Simple 3-term metalog: a0=0, a1=1, a2=0
        double[] a = {0.0, 1.0, 0.0};
        Metalog m = new Metalog(a);

        // For this setup, M(p) = ln(p/(1-p))
        double p = 0.3;
        double expected = Math.log(p/(1-p));
        assertEquals(expected, m.quantile(p), 1e-12);

        // PDF = 1 / dM/dp, and dM/dp = 1/[p(1-p)], so PDF = p(1-p)
        double pdf = m.pdf(p);
        assertEquals(p*(1-p), pdf, 1e-12);
    }
}
