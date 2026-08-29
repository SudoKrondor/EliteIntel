package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryKind;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionMemoryGatewayTest {

    private static MemoryRecord dialogue(long second, String commander, String companion) {
        return MemoryRecord.dialogue(Instant.ofEpochSecond(second), commander, companion);
    }

    @Test
    void writesCompletedRecordAtomically() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        MemoryRecord record = dialogue(1, "hello", "hello commander");

        gateway.write(record);

        assertEquals(List.of(record), gateway.readRecentHistory());
    }

    /**
     * The window is the whole store: what leaves it is gone, and no tier below it keeps a copy that nothing
     * would ever read again.
     */
    @Test
    void anEvictedRecordIsNotRetainedAnywhereElse() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        MemoryRecord oldest = MemoryRecord.query(Instant.EPOCH, "cargo?", "eight tonnes");
        gateway.write(oldest);
        for (int i = 1; i <= CompanionMemoryPolicy.recentRecordLimit(); i++) {
            gateway.write(dialogue(i, "order " + i, "reply " + i));
        }

        MemorySnapshot snapshot = gateway.snapshot();

        assertEquals(CompanionMemoryPolicy.recentRecordLimit(), snapshot.recent().size());
        assertFalse(snapshot.recent().contains(oldest));
        assertTrue(snapshot.recent().stream().noneMatch(record -> record.kind() == MemoryKind.QUERY));
    }

    @Test
    void ordinaryEntriesAreBoundedBeforeStorage() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        gateway.write(dialogue(0, "x".repeat(CompanionMemoryPolicy.entryMaxChars() + 20), "done"));

        String stored = gateway.readRecentHistory().get(0).entries().get(0).content();

        assertEquals(CompanionMemoryPolicy.entryMaxChars(), stored.length());
        assertTrue(stored.endsWith("..."));
    }

    @Test
    void oversizedRecordIsHandedOffWholeBeforeAnyStorageMutation() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        List<MemoryRecord> deferred = new ArrayList<>();
        gateway.setOversizedMemoryListener(deferred::add);
        MemoryRecord query = MemoryRecord.query(
                Instant.EPOCH, "route?", "leg ".repeat(CompanionMemoryPolicy.entryMaxChars()));

        gateway.write(query);

        assertTrue(gateway.readRecentHistory().isEmpty());
        assertEquals(List.of(query), deferred);
        assertEquals(2, deferred.get(0).entryCount());
    }

    @Test
    void declinedOversizedHandoffStoresOneBoundedCompleteRecord() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        gateway.setOversizedMemoryListener(record -> false);
        gateway.write(MemoryRecord.query(
                Instant.EPOCH, "route?", "first leg then second leg ".repeat(20)));

        MemoryRecord stored = gateway.readRecentHistory().getFirst();

        assertEquals(MemoryKind.QUERY, stored.kind());
        assertEquals(2, stored.entryCount());
        assertTrue(stored.companionText().length() <= CompanionMemoryPolicy.entryMaxChars());
        assertTrue(stored.companionText().endsWith("..."));
        assertFalse(stored.companionText().endsWith("le..."),
                "fallback should prefer a complete word over a mid-word cut");
    }

    @Test
    void snapshotCollectionsAreImmutable() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        for (int i = 0; i <= CompanionMemoryPolicy.recentRecordLimit(); i++) {
            gateway.write(dialogue(i, "order " + i, "reply " + i));
        }

        assertThrows(UnsupportedOperationException.class, gateway.snapshot().recent()::clear);
    }
}
