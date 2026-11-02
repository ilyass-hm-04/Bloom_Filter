package com.example.mlops;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

public class Experiment {

    public static void main(String[] args) throws Exception {
        Path configPath = Path.of(args.length > 0
                ? args[0] : "experiments/experiment-config.properties");
        ExperimentConfig cfg = ExperimentConfig.load(configPath);

        List<DataRecord> stream = buildStream(cfg.unique(), cfg.duplicates(), cfg.seed());
        String checksum = checksum(stream);
        System.out.println("Dataset " + cfg.datasetVersion() + " sha256=" + checksum);

        run("baseline", stream, false, cfg, checksum);
        run("bloom", stream, true, cfg, checksum);
    }

    static List<DataRecord> buildStream(int unique, int duplicates, long seed) {
        List<DataRecord> list = new ArrayList<>();
        for (int i = 0; i < unique; i++) {
            list.add(new DataRecord("id-" + i, i * 0.001, i % 100));
        }
        Random rnd = new Random(seed);
        for (int d = 0; d < duplicates; d++) {
            list.add(list.get(rnd.nextInt(unique)));
        }
        Collections.shuffle(list, rnd);
        return list;
    }

    /** SHA-256 over the ordered record IDs: proves two runs used the same stream. */
    static String checksum(List<DataRecord> stream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (DataRecord r : stream) {
            md.update((r.recordId() + "\n").getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(md.digest());
    }

    static void run(String label, List<DataRecord> stream, boolean useFilter,
                    ExperimentConfig cfg, String checksum) throws Exception {
        ExactIdStore store = new ExactIdStore(cfg.lookupDelayMicros());
        DeduplicationPipeline pipeline =
                new DeduplicationPipeline(cfg.bits(), cfg.hashes(), store);

        long start = System.nanoTime();
        for (DataRecord record : stream) {
            if (useFilter) pipeline.process(record);
            else pipeline.processBaseline(record);
        }
        double ms = (System.nanoTime() - start) / 1e6;

        System.out.println("== " + label + " ==");
        pipeline.metrics().printSummary();
        System.out.printf("Elapsed: %.0f ms%n%n", ms);

        Path dir = Path.of(cfg.outputDir());
        MetricsWriter.appendCsv(dir.resolve("metrics.csv"), cfg, label, checksum,
                pipeline.metrics(), ms);
        MetricsWriter.writeJson(dir.resolve("run-" + label + ".json"), cfg, label,
                checksum, pipeline.metrics(), ms);
    }
}