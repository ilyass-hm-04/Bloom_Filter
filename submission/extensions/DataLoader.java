package com.example.mlops;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    /** Reads record_id,feature_a,feature_b (with a header line). */
    public static List<DataRecord> readCsv(Path file) throws IOException {
        List<DataRecord> records = new ArrayList<>();
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = in.readLine(); // header
            int lineNo = 1;
            while ((line = in.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                String[] c = line.split(",", -1);
                if (c.length != 3) {
                    throw new IOException("Line " + lineNo + ": expected 3 columns, got " + c.length);
                }
                records.add(new DataRecord(c[0].trim(), Double.parseDouble(c[1]), Integer.parseInt(c[2].trim())));
            }
        }
        return records;
    }

    public static void writeCsv(Path file, List<DataRecord> records) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("record_id,feature_a,feature_b\n");
            for (DataRecord r : records) {
                out.write(r.recordId() + "," + r.featureA() + "," + r.featureB() + "\n");
            }
        }
    }
}
