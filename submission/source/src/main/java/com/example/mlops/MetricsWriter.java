package com.example.mlops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;

public class MetricsWriter {

    private static final String HEADER =
            "timestamp,label,dataset_version,dataset_checksum,seed,unique,duplicates,"
                    + "bits,hashes,lookup_delay_us,total_records,possible_duplicates,"
                    + "confirmed_duplicates,false_positives,accepted,exact_lookups,"
                    + "lookups_avoided,elapsed_ms";

    private static String row(ExperimentConfig c, String label, String checksum,
                              PipelineMetrics m, double elapsedMs) {
        return String.join(",",
                Instant.now().toString(), label, c.datasetVersion(), checksum,
                String.valueOf(c.seed()), String.valueOf(c.unique()),
                String.valueOf(c.duplicates()), String.valueOf(c.bits()),
                String.valueOf(c.hashes()), String.valueOf(c.lookupDelayMicros()),
                String.valueOf(m.totalRecords), String.valueOf(m.possibleDuplicates),
                String.valueOf(m.confirmedDuplicates), String.valueOf(m.falsePositives),
                String.valueOf(m.acceptedRecords), String.valueOf(m.exactChecks),
                String.valueOf(m.exactChecksAvoided),
                String.format(Locale.ROOT, "%.1f", elapsedMs));
    }

    /** One line per run, appended so runs can be compared. */
    public static void appendCsv(Path file, ExperimentConfig c, String label,
                                 String checksum, PipelineMetrics m, double elapsedMs)
            throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        boolean isNew = Files.notExists(file);
        StringBuilder sb = new StringBuilder();
        if (isNew) sb.append(HEADER).append('\n');
        sb.append(row(c, label, checksum, m, elapsedMs)).append('\n');
        Files.writeString(file, sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** One JSON file per run. */
    public static void writeJson(Path file, ExperimentConfig c, String label,
                                 String checksum, PipelineMetrics m, double elapsedMs)
            throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        String json = String.format(Locale.ROOT, """
                {
                  "label": "%s",
                  "config": {
                    "dataset_version": "%s",
                    "dataset_checksum": "%s",
                    "seed": %d,
                    "unique": %d,
                    "duplicates": %d,
                    "bits": %d,
                    "hashes": %d,
                    "lookup_delay_us": %d
                  },
                  "metrics": {
                    "total_records": %d,
                    "possible_duplicates": %d,
                    "confirmed_duplicates": %d,
                    "false_positives": %d,
                    "accepted": %d,
                    "exact_lookups": %d,
                    "lookups_avoided": %d,
                    "elapsed_ms": %.1f
                  }
                }
                """,
                label, c.datasetVersion(), checksum, c.seed(), c.unique(),
                c.duplicates(), c.bits(), c.hashes(), c.lookupDelayMicros(),
                m.totalRecords, m.possibleDuplicates, m.confirmedDuplicates,
                m.falsePositives, m.acceptedRecords, m.exactChecks,
                m.exactChecksAvoided, elapsedMs);
        Files.writeString(file, json);
    }
}