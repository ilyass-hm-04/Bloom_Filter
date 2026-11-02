package com.example.mlops;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ExperimentConfig(
        String datasetVersion,
        int unique,
        int duplicates,
        long seed,
        int bits,
        int hashes,
        long lookupDelayMicros,
        String outputDir
) {
    public static ExperimentConfig load(Path path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
        }
        return new ExperimentConfig(
                p.getProperty("dataset.version", "v1"),
                Integer.parseInt(p.getProperty("stream.unique")),
                Integer.parseInt(p.getProperty("stream.duplicates")),
                Long.parseLong(p.getProperty("stream.seed")),
                Integer.parseInt(p.getProperty("filter.bits")),
                Integer.parseInt(p.getProperty("filter.hashes")),
                Long.parseLong(p.getProperty("store.lookupDelayMicros")),
                p.getProperty("output.dir", "reports")
        );
    }
}