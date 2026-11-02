package com.example.mlops;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BloomFilterTest {

    @Test
    void neverGivesFalseNegatives() {
        BloomFilter f = new BloomFilter(10_000, 4);
        for (int i = 0; i < 1_000; i++) f.add("id-" + i);
        for (int i = 0; i < 1_000; i++) {
            assertTrue(f.mightContain("id-" + i), "added item must be found: id-" + i);
        }
    }

    @Test
    void emptyFilterContainsNothing() {
        BloomFilter f = new BloomFilter(1_000, 3);
        assertFalse(f.mightContain("anything"));
    }

    @Test
    void rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(0, 3));
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(100, 0));
    }
}