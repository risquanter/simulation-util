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

import java.util.function.Function;

/**
 * HDR (Hash-based Discrete Random) number generator implementation
 * Based on: "Hash-based Discrete Random Number Generation for Large-Scale Simulations"
 * Proceedings of the 2019 Winter Simulation Conference (WSC 2019).
 * Available at: https://www.informs-sim.org/wsc19papers/339.pdf
 */
public record HDR(long entityId, long varId, long seed3, long seed4) {

    // Constructor with defaults (matching Scala case class defaults)
    public HDR(long entityId, long varId) {
        this(entityId, varId, 0L, 0L);
    }

    public double trial(long counter) {
        return generate(counter, entityId, varId, seed3, seed4);
    }

    public static double generate(long counter, long entityId, long varId, long seed3, long seed4) {
        return (MOD(
            (MOD(
                MOD(
                    999999999999989L,
                    MOD(
                        counter * 2499997L + (varId) * 1800451L + (entityId) * 2000371L + (seed3) * 1796777L + (seed4) * 2299603L,
                        7450589L
                    ) * 4658L + 7450581L
                ) * 383L,
                99991L
            ) * 7440893L + MOD(
                MOD(
                    999999999999989L,
                    MOD(
                        counter * 2246527L + (varId) * 2399993L + (entityId) * 2100869L + (seed3) * 1918303L + (seed4) * 1624729L,
                        7450987L
                    ) * 7580L + 7560584L
                ) * 17669L,
                7440893L
            )) * 1343L,
            4294967296L
        ) + 0.5) / 4294967296L;
    }

    public static long MOD(long dividend, long divisor) {
        return ((dividend % divisor) + divisor) % divisor;
    }

    public static Function<Long, Double> createHdr(long entityId, long varId, long seed3, long seed4) {
        HDR hdr = new HDR(entityId, varId, seed3, seed4);
        return hdr::trial;
    }

    public static Function<Long, Double> createHdr(long entityId, long varId) {
        return createHdr(entityId, varId, 0L, 0L);
    }

    public static TriFunction<Long, Long, Long, Double> createGenerator(long seed3, long seed4) {
        return (counter, entityId, varId) -> generate(counter, entityId, varId, seed3, seed4);
    }

    public static TriFunction<Long, Long, Long, Double> createGenerator() {
        return createGenerator(0L, 0L);
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
