# Results: baseline vs Bloom-gated

Measured with the project's classes. Elapsed times are single runs after a warm-up (Java 21, cloud container), so only orders of magnitude are meaningful.

## Cost assumptions

- Exact-store lookup (`contains()`) simulated at **50 µs** by busy-waiting; `addIfAbsent()` costs nothing extra.
- Stream: 100,000 records, shuffled with seed 42, repeated IDs drawn from the unique IDs.
- Filter sized for the number of unique IDs with p = 1% (`BloomSizing`), k = 7, unless stated.
- Memory: `BloomFilter` uses `boolean[]` (1 byte per bit). Packed, it would be 8× smaller.

## A. Comparison table (7 configurations)

| # | Stream (unique + duplicates) | m (bits) | k | Delay | Pipeline | Lookups | Avoided | False positives | Accepted | Time |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 90,000 + 10,000 | 862,656 | 7 | 50 µs | Exact store only | 100,000 | 0 | n/a | 90,000 | 5,224 ms |
| 1 | 90,000 + 10,000 | 862,656 | 7 | 50 µs | Bloom-gated | 10,167 | 89,833 | 167 | 90,000 | 650 ms |
| 2 | 100,000 + 0 | 958,506 | 7 | 50 µs | Exact store only | 100,000 | 0 | n/a | 100,000 | 5,260 ms |
| 2 | 100,000 + 0 | 958,506 | 7 | 50 µs | Bloom-gated | 171 | 99,829 | 171 | 100,000 | 51 ms |
| 3 | 50,000 + 50,000 | 479,253 | 7 | 50 µs | Exact store only | 100,000 | 0 | n/a | 50,000 | 5,198 ms |
| 3 | 50,000 + 50,000 | 479,253 | 7 | 50 µs | Bloom-gated | 50,075 | 49,925 | 75 | 50,000 | 2,630 ms |
| 4 | 10,000 + 90,000 | 95,851 | 7 | 50 µs | Exact store only | 100,000 | 0 | n/a | 10,000 | 5,272 ms |
| 4 | 10,000 + 90,000 | 95,851 | 7 | 50 µs | Bloom-gated | 90,013 | 9,987 | 13 | 10,000 | 4,644 ms |
| 5 | 1,000 + 99,000 | 9,586 | 7 | 50 µs | Exact store only | 100,000 | 0 | n/a | 1,000 | 5,212 ms |
| 5 | 1,000 + 99,000 | 9,586 | 7 | 50 µs | Bloom-gated | 99,001 | 999 | 1 | 1,000 | 5,039 ms |
| 6 | 90,000 + 10,000 | 862,656 | 7 | **0** | Exact store only | 100,000 | 0 | n/a | 90,000 | 18 ms |
| 6 | 90,000 + 10,000 | 862,656 | 7 | **0** | Bloom-gated | 10,167 | 89,833 | 167 | 90,000 | 38 ms |
| 7 | 10,000 + 90,000 | 95,851 | 7 | **0** | Exact store only | 100,000 | 0 | n/a | 10,000 | 6 ms |
| 7 | 10,000 + 90,000 | 95,851 | 7 | **0** | Bloom-gated | 90,013 | 9,987 | 13 | 10,000 | 8 ms |

In every row both pipelines make **identical decisions** (same accepted count).

## B. Project dataset (`data/incoming.csv`, 100,000 rows) via `CsvPipeline`

| Metric | Value |
|---|---|
| Records / accepted / confirmed duplicates | 100,000 / 90,000 / 10,000 |
| m, k | 958,506, 7 |
| Possible duplicates (filter says maybe) | 10,086 |
| False positives | 86 |
| Lookups / avoided | 10,086 / 89,914 |
| Mismatches vs `expected-results.csv` | **0** |

## C. Small filters of Step 8 (filter-only false-positive rate, 10,000 unseen IDs)

| Run | m | k | n | Theory | Observed, sequential IDs | Observed, random UUIDs |
|---|---|---|---|---|---|---|
| A | 1,000 | 3 | 100 | 1.74% | 1.64% | 1.92% |
| B | 2,000 | 3 | 100 | 0.27% | 0.29% | 0.19% |
| C | 1,000 | 5 | 100 | 0.94% | 0.95% | 1.16% |
| D | 1,000 | 3 | 300 | 20.90% | 19.78% | 21.31% |

Pipeline run D on a stream of 300 unique IDs: 21 possible duplicates, **21 false positives**, 279 lookups avoided.

**Difference with the Step 8 output you reported.** That output showed 0 false positives and 0.000% for run D. The current code does not reproduce it (21 false positives for run D), and the "False-positive rate" line of `Main.java` reads the metrics of a pipeline that never processed any record, divided by a hard-coded 100, so it always prints 0.

## D. Sizing (Step 9)

| Scenario | n | p | m | k | Predicted FPR |
|---|---|---|---|---|---|
| A | 100 | 5% | 624 | 4 | about 5% |
| B | 1,000 | 1% | 9,586 | 7 | about 1% |
| C | 10,000 | 0.1% | 143,776 | 10 | about 0.1% |
| Step 5 | 90,000 | 1% | 862,656 | 7 | about 1% |

Checked by `BloomSizingTest`.

## E. FPR vs bit-array size

See `answers-section-14.md`, question 10, and `report/fpr-vs-bits.png`.

## Reproduce

```bash
javac -d out src/main/java/com/example/mlops/{BloomFilter,BloomSizing,DataRecord,DeduplicationPipeline,ExactIdStore,PipelineMetrics}.java results/bench/Bench.java
java -cp out com.example.mlops.Bench fpr                       # sections C and E
java -cp out com.example.mlops.Bench cmp 50 90000 10000 862656 7   # one row of section A
java -cp out com.example.mlops.Bench mem                       # exact-store memory
```

`Bench cmp <delay µs> <unique> <duplicates> <m> <k>` prints one baseline vs Bloom-gated row.
