# Bloom Filter MLOps Lab: Submission

## Contents

| Path | What it is |
|---|---|
| `answers-section-14.md` | Answers to the 10 questions of section 14 |
| `source/` | Snapshot of the project: `src/main`, `src/test`, `pom.xml`, `experiments/`, `scripts/`, `.github/`, `Dockerfile` |
| `data/` | Known dataset `incoming.csv` (100,000 rows) and exact labels `expected-results.csv` (90,000 accept, 10,000 duplicate) |
| `results/results-table.md` | Results table (7 configurations), baseline vs Bloom-gated comparison and cost assumptions |
| `results/bench/Bench.java` | Benchmark harness that produced the numbers |
| `report/fpr-vs-bits.png` | Graph: false-positive rate vs bit-array size |
| `extensions/` | CSV loader and CSV pipeline (question 9), tested on `data/` |

## Reproduce

Requirements: JDK 17 or later, Maven.

```bash
cd source
mvn test                                   # unit tests
mvn -q compile
java -cp target/classes com.example.mlops.Experiment experiments/experiment-config.properties
java -cp target/classes com.example.mlops.BloomSizing
```

CSV pipeline (question 9): copy `extensions/DataLoader.java` over `source/src/main/java/com/example/mlops/DataLoader.java` and add `CsvPipeline.java` next to it, then:

```bash
java -cp target/classes com.example.mlops.CsvPipeline ../data/incoming.csv reports/accepted.csv ../data/expected-results.csv
```

## Section 15 checklist

| Requirement | Where |
|---|---|
| Source code and instructions to reproduce | `source/`, this file |
| Known dataset and exact expected duplicate labels | `data/incoming.csv`, `data/expected-results.csv` (0 mismatches with the pipeline) |
| Unit tests for the Bloom filter | `BloomFilterTest`, `BloomSizingTest` |
| Test: a possible match is never discarded without exact confirmation | `DeduplicationPipelineTest.possibleMatchIsNeverDiscardedWithoutExactConfirmation` |
| Test: a definitely-absent ID triggers no exact lookup | `DeduplicationPipelineTest.definitelyAbsentIdTriggersNoExactLookup` |
| Results table with at least four configurations | `results/results-table.md` |
| Baseline vs Bloom-gated comparison with lookups avoided and cost assumptions | `results/results-table.md` (sections A and "Cost assumptions") |
| Short report on correctness, performance and limitations | Not written as a separate file yet; correctness, performance and limitations are covered in `answers-section-14.md` (questions 1 to 8) |

## Known issues in the snapshot

- `Main.java` calls `Experiment.run(label, stream, boolean)`, a signature that no longer exists, so the project does not compile until `Main.java` is fixed or removed. Its "False-positive rate" line also reads an unused pipeline and divides by 100.
- The `Dockerfile` is still the IntelliJ default (`ubuntu` + `top`), not the reproducible-run Dockerfile of Step 10.
- `DataLoader.java` in `src/main` is an empty stub; the working version is in `extensions/`.
