package com.example.mlops;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeduplicationPipelineTest {

    private static DataRecord rec(String id) {
        return new DataRecord(id, 0.0, 0);
    }

    @Test
    void sampleDatasetMatchesExpectedDecisions() {
        DeduplicationPipeline p =
                new DeduplicationPipeline(1000, 4, new ExactIdStore(0));
        List<String> ids = List.of("r001", "r002", "r003", "r002", "r004", "r005", "r001", "r006");
        List<Boolean> expected = List.of(true, true, true, false, true, true, false, true);

        for (int i = 0; i < ids.size(); i++) {
            assertEquals(expected.get(i), p.process(rec(ids.get(i))), "row " + i);
        }
        assertEquals(6, p.metrics().acceptedRecords);
        assertEquals(2, p.metrics().confirmedDuplicates);
    }

    @Test
    void definitelyAbsentIdTriggersNoExactLookup() {
        ExactIdStore store = new ExactIdStore(0);
        DeduplicationPipeline p = new DeduplicationPipeline(100_000, 5, store);

        assertTrue(p.process(rec("brand-new")));
        assertEquals(0, store.lookups());
        assertEquals(1, p.metrics().exactChecksAvoided);
    }

    @Test
    void possibleMatchIsNeverDiscardedWithoutExactConfirmation() {
        ExactIdStore store = new ExactIdStore(0);
        // Size 1 and one hash: after one insert, the filter answers "maybe" for every ID.
        DeduplicationPipeline p = new DeduplicationPipeline(1, 1, store);

        assertTrue(p.process(rec("a")));
        // "b" is new, but the filter says "maybe": it must be verified, then accepted.
        assertTrue(p.process(rec("b")));

        assertEquals(1, store.lookups());
        assertEquals(1, p.metrics().falsePositives);
        assertEquals(2, p.metrics().acceptedRecords);
        assertEquals(0, p.metrics().confirmedDuplicates);
    }

    @Test
    void confirmedDuplicateIsDiscardedAfterExactLookup() {
        ExactIdStore store = new ExactIdStore(0);
        DeduplicationPipeline p = new DeduplicationPipeline(10_000, 4, store);

        assertTrue(p.process(rec("x")));
        assertFalse(p.process(rec("x")));
        assertEquals(1, store.lookups());
        assertEquals(1, p.metrics().confirmedDuplicates);
    }

    @Test
    void staleFilterCannotCauseDoubleAcceptance() {
        ExactIdStore store = new ExactIdStore(0);
        store.addIfAbsent("x"); // ID is in the store but the new filter is empty (restart)
        DeduplicationPipeline p = new DeduplicationPipeline(10_000, 4, store);

        // Filter says "absent", but addIfAbsent() is the final arbiter.
        assertFalse(p.process(rec("x")));
        assertEquals(0, store.lookups());
    }

    @Test
    void rebuildFromExactStoreRestoresTheFilter() {
        ExactIdStore store = new ExactIdStore(0);
        store.addIfAbsent("x");
        DeduplicationPipeline p = new DeduplicationPipeline(10_000, 4, store);

        p.rebuildFilterFromExactStore();
        assertTrue(p.filter().mightContain("x"));
        assertFalse(p.process(rec("x")));
        assertEquals(1, store.lookups());
    }

    @Test
    void blankIdIsRejected() {
        DeduplicationPipeline p =
                new DeduplicationPipeline(1000, 4, new ExactIdStore(0));
        assertThrows(IllegalArgumentException.class, () -> p.process(rec(" ")));
    }

    /** Small benchmark: fails CI if the filter stops saving lookups. */
    @Test
    void bloomGatedUsesFarFewerLookupsThanBaseline() {
        List<DataRecord> stream = Experiment.buildStream(9_000, 1_000, 42L);

        ExactIdStore s1 = new ExactIdStore(0);
        DeduplicationPipeline baseline = new DeduplicationPipeline(86_000, 7, s1);
        stream.forEach(baseline::processBaseline);

        ExactIdStore s2 = new ExactIdStore(0);
        DeduplicationPipeline gated = new DeduplicationPipeline(86_000, 7, s2);
        stream.forEach(gated::process);

        assertEquals(baseline.metrics().acceptedRecords, gated.metrics().acceptedRecords);
        assertEquals(10_000, s1.lookups());
        assertTrue(s2.lookups() < s1.lookups() / 2,
                "expected < 50% of baseline lookups, got " + s2.lookups());
    }
}