package com.example.mlops;

import java.util.List;

import static com.example.mlops.Experiment.buildStream;
import static com.example.mlops.Experiment.run;

public class Main {
    public static void main(String[] args) {

        DeduplicationPipeline pipeline =
                new DeduplicationPipeline(1000, 3, new ExactIdStore(0));
        List<DataRecord> stream = buildStream(300, 0, 42L);
        run("exact store only (baseline)", stream, false);
        run("bloom-gated", stream, true);

        double fpr = 100.0 * pipeline.metrics().falsePositives / 100;
        System.out.printf("False-positive rate: %.3f%%%n", fpr);






    }
}