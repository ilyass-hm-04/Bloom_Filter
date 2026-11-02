package com.example.mlops;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Usage: CsvPipeline <incoming.csv> <accepted.csv> [expected-results.csv] */
public class CsvPipeline {
    public static void main(String[] args) throws Exception {
        Path in = Path.of(args.length > 0 ? args[0] : "data/incoming.csv");
        Path out = Path.of(args.length > 1 ? args[1] : "reports/accepted.csv");

        List<DataRecord> incoming = DataLoader.readCsv(in);
        int m = BloomSizing.optimalBits(incoming.size(), 0.01);
        int k = BloomSizing.optimalHashes(incoming.size(), m);

        ExactIdStore store = new ExactIdStore(0);
        DeduplicationPipeline pipeline = new DeduplicationPipeline(m, k, store);

        List<DataRecord> accepted = new ArrayList<>();
        List<String> decisions = new ArrayList<>();
        for (DataRecord r : incoming) {
            boolean ok = pipeline.process(r);
            decisions.add(ok ? "accept" : "duplicate");
            if (ok) accepted.add(r);
        }
        DataLoader.writeCsv(out, accepted);

        System.out.println("Read " + incoming.size() + " records, wrote " + accepted.size() + " to " + out);
        System.out.println("m=" + m + " k=" + k);
        pipeline.metrics().printSummary();

        if (args.length > 2) { // compare with the known labels
            List<String> lines = Files.readAllLines(Path.of(args[2]));
            int mismatches = 0;
            for (int i = 0; i < decisions.size(); i++) {
                String expected = lines.get(i + 1).split(",")[1].trim();
                if (!expected.equals(decisions.get(i))) mismatches++;
            }
            System.out.println("Mismatches vs expected labels: " + mismatches);
        }
    }
}
