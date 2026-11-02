package com.example.mlops;

import java.util.*;

public class Bench {
    static List<DataRecord> stream(int unique, int dups, long seed) {
        List<DataRecord> l = new ArrayList<>();
        for (int i = 0; i < unique; i++) l.add(new DataRecord("id-" + i, i * 0.001, i % 100));
        Random r = new Random(seed);
        for (int d = 0; d < dups; d++) l.add(l.get(r.nextInt(unique)));
        Collections.shuffle(l, r);
        return l;
    }

    record Res(double ms, long lookups, PipelineMetrics m) {}

    static Res run(List<DataRecord> s, boolean filter, int m, int k, long delay) {
        ExactIdStore st = new ExactIdStore(delay);
        DeduplicationPipeline p = new DeduplicationPipeline(m, k, st);
        long t = System.nanoTime();
        for (DataRecord r : s) { if (filter) p.process(r); else p.processBaseline(r); }
        double ms = (System.nanoTime() - t) / 1e6;
        return new Res(ms, st.lookups(), p.metrics());
    }

    static double observedFpr(int m, int k, int n, boolean random, int tests) {
        BloomFilter f = new BloomFilter(m, k);
        for (int i = 0; i < n; i++) f.add(random ? UUID.randomUUID().toString() : "id-" + i);
        int fp = 0;
        for (int i = 0; i < tests; i++)
            if (f.mightContain(random ? UUID.randomUUID().toString() : "unseen-" + i)) fp++;
        return 100.0 * fp / tests;
    }

    public static void main(String[] a) {
        switch (a[0]) {
            case "fpr" -> {
                System.out.println("## FPR sweep n=1000, k=7 (20000 unseen IDs)");
                System.out.println("m,theory%,seq%,uuid%");
                for (int m : new int[]{2000, 4000, 6000, 8000, 9586, 12000, 16000, 24000})
                    System.out.printf(Locale.ROOT, "%d,%.3f,%.3f,%.3f%n", m, 100 * BloomSizing.expectedFpr(m, 7, 1000),
                            observedFpr(m, 7, 1000, false, 20000), observedFpr(m, 7, 1000, true, 20000));
                System.out.println("## Step 8 configs (10000 unseen IDs)");
                System.out.println("run,m,k,n,theory%,seq%,uuid%");
                int[][] cfg = {{1000, 3, 100}, {2000, 3, 100}, {1000, 5, 100}, {1000, 3, 300}};
                String[] nm = {"A", "B", "C", "D"};
                for (int i = 0; i < 4; i++) {
                    int m = cfg[i][0], k = cfg[i][1], n = cfg[i][2];
                    System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%.3f,%.3f,%.3f%n", nm[i], m, k, n,
                            100 * BloomSizing.expectedFpr(m, k, n), observedFpr(m, k, n, false, 10000), observedFpr(m, k, n, true, 10000));
                }
                System.out.println("## Pipeline on pure-unique stream, run D (m=1000,k=3,n=300)");
                Res r = run(stream(300, 0, 42L), true, 1000, 3, 0);
                System.out.println("possible=" + r.m.possibleDuplicates + " falsePositives=" + r.m.falsePositives + " avoided=" + r.m.exactChecksAvoided);
            }
            case "cmp" -> {
                long delay = Long.parseLong(a[1]);
                int u = Integer.parseInt(a[2]), d = Integer.parseInt(a[3]);
                int m = Integer.parseInt(a[4]), k = Integer.parseInt(a[5]);
                List<DataRecord> s = stream(u, d, 42L);
                for (int i = 0; i < 3; i++) { run(s, true, m, k, 0); run(s, false, m, k, 0); } // warm-up
                Res b = run(s, false, m, k, delay);
                Res g = run(s, true, m, k, delay);
                System.out.printf(Locale.ROOT,
                        "RESULT delay=%d unique=%d dups=%d m=%d k=%d | base: lookups=%d ms=%.0f accepted=%d | bloom: lookups=%d avoided=%d fp=%d ms=%.0f accepted=%d%n",
                        delay, u, d, m, k, b.lookups, b.ms, b.m.acceptedRecords, g.lookups, g.m.exactChecksAvoided, g.m.falsePositives, g.ms, g.m.acceptedRecords);
            }
            case "mem" -> {
                Runtime rt = Runtime.getRuntime();
                System.gc(); long before = rt.totalMemory() - rt.freeMemory();
                ExactIdStore st = new ExactIdStore(0);
                for (int i = 0; i < 90000; i++) st.addIfAbsent("id-" + i);
                System.gc(); long after = rt.totalMemory() - rt.freeMemory();
                System.out.println("exact store, 90000 ids: approx bytes=" + (after - before) + " size=" + st.snapshot().size());
            }
        }
    }
}
