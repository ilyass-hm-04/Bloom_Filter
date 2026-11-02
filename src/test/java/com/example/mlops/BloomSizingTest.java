package com.example.mlops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BloomSizingTest {

    @Test
    void scenarioA() {
        int m = BloomSizing.optimalBits(100, 0.05);
        assertEquals(624, m);
        assertEquals(4, BloomSizing.optimalHashes(100, m));
    }

    @Test
    void scenarioB() {
        int m = BloomSizing.optimalBits(1_000, 0.01);
        assertEquals(9_586, m);
        assertEquals(7, BloomSizing.optimalHashes(1_000, m));
    }

    @Test
    void scenarioC() {
        int m = BloomSizing.optimalBits(10_000, 0.001);
        assertEquals(143_776, m);
        assertEquals(10, BloomSizing.optimalHashes(10_000, m));
    }
}
