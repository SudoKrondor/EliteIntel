package elite.intel.companion.memory;

import elite.intel.ai.embed.AngleEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void queryExpiresWhenItLeavesRecentHistory() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        gateway.write(MemoryRecord.query(Instant.EPOCH, "cargo?", "eight tonnes"));
        for (int i = 1; i <= CompanionMemoryPolicy.recentRecordLimit(); i++) {
            gateway.write(dialogue(i, "order " + i, "reply " + i));
        }

        MemorySnapshot snapshot = gateway.snapshot();

        assertTrue(snapshot.recent().stream().noneMatch(record -> record.kind() == MemoryKind.QUERY));
        assertTrue(snapshot.retainedByKind().values().stream().flatMap(List::stream)
                .noneMatch(record -> record.kind() == MemoryKind.QUERY));
    }

    @Test
    void dialogueAndEventMoveToTheirOwnRetainedHistories() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        gateway.write(dialogue(0, "hello", "hello commander"));
        gateway.write(MemoryRecord.event(Instant.ofEpochSecond(1), "jump complete"));
        for (int i = 2; i <= CompanionMemoryPolicy.recentRecordLimit(); i++) {
            gateway.write(dialogue(i, "order " + i, "reply " + i));
        }
        gateway.write(dialogue(100, "final order", "done"));

        MemorySnapshot snapshot = gateway.snapshot();

        assertEquals("hello", snapshot.retainedByKind().get(MemoryKind.DIALOGUE)
                .get(0).entries().get(0).content());
        assertEquals("jump complete", snapshot.retainedByKind().get(MemoryKind.EVENT)
                .get(0).entries().get(0).content());
    }

    @Test
    void recentDialogueDuplicatesCollapseAtTheMidTermHandoff() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0,
                () -> new SemanticPhraseMatcher(new AngleEmbedder(Map.of(
                        "where are we", 0.0,
                        "what system is this", 0.0,
                        "we are in Sol", 90.0,
                        "our system is Sol", 90.0))));
        MemoryRecord first = dialogue(0, "where are we", "we are in Sol");
        MemoryRecord newest = dialogue(1, "what system is this", "our system is Sol");
        gateway.write(first);
        gateway.write(newest);
        for (int i = 2; i < CompanionMemoryPolicy.recentRecordLimit() + 2; i++) {
            gateway.write(dialogue(i, "order " + i, "reply " + i));
        }

        assertEquals(List.of(newest), gateway.snapshot().retainedByKind().get(MemoryKind.DIALOGUE));
    }

    @Test
    void savedTextSkipsRecentHistoryAndRemainsVerbatimWithinItsOwnLimit() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        String phrase = "Remember " + "X".repeat(CompanionMemoryPolicy.entryMaxChars() + 20);

        gateway.write(MemoryRecord.savedText(Instant.EPOCH, phrase));

        assertTrue(gateway.readRecentHistory().isEmpty());
        assertEquals(phrase, gateway.savedTextRecords().get(0).entries().get(0).content());
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
    void savedTextBypassesTheOversizedCompressor() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        List<MemoryRecord> deferred = new ArrayList<>();
        gateway.setOversizedMemoryListener(deferred::add);
        String text = "remember " + "x".repeat(CompanionMemoryPolicy.entryMaxChars() + 10);

        gateway.write(MemoryRecord.savedText(Instant.EPOCH, text));

        assertTrue(deferred.isEmpty());
        assertEquals(text, gateway.savedTextRecords().getFirst().savedText());
    }

    @Test
    void summariesAreIndependentByKindAndSearchable() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        List<MemoryRecord> pending = new ArrayList<>();
        gateway.setPendingConsolidationListener(pending::add);
        int dialogueWrites = CompanionMemoryPolicy.recentRecordLimit()
                + CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE) + 1;
        for (int i = 0; i < dialogueWrites; i++) {
            gateway.write(dialogue(i, "dialogue " + i, "reply " + i));
        }
        MemoryRecord dialogueBatch = pending.stream()
                .filter(record -> record.kind() == MemoryKind.DIALOGUE)
                .findFirst().orElseThrow();
        gateway.commitConsolidation(
                MemoryKind.DIALOGUE, List.of(dialogueBatch), "We discussed exploration in Colonia");

        int eventWrites = CompanionMemoryPolicy.recentRecordLimit()
                + CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.EVENT) + 1;
        for (int i = 0; i < eventWrites; i++) {
            gateway.write(MemoryRecord.event(Instant.ofEpochSecond(1000 + i), "event " + i));
        }
        MemoryRecord eventBatch = pending.stream()
                .filter(record -> record.kind() == MemoryKind.EVENT)
                .findFirst().orElseThrow();
        gateway.commitConsolidation(MemoryKind.EVENT, List.of(eventBatch), "The ship arrived in Sol");

        assertEquals(Map.of(
                MemoryKind.DIALOGUE, "We discussed exploration in Colonia",
                MemoryKind.EVENT, "The ship arrived in Sol"), gateway.longTermSummaries());
        assertFalse(gateway.recallMatching("Colonia", 5).items().isEmpty());
    }

    @Test
    void explicitSearchReturnsOneItemPerCompletedRecord() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        gateway.write(MemoryRecord.query(Instant.EPOCH, "cargo status", "Eight tonnes remain."));

        MemorySearchResult result = gateway.recallMatching("cargo", 10);

        assertEquals(1, result.matchingUnits());
        assertEquals(1, result.exactRecordCount());
        assertEquals(1, result.items().size());
        assertTrue(result.items().get(0).contains("[COMMANDER] cargo status"));
        assertTrue(result.items().get(0).contains("Eight tonnes remain."));
    }

    @Test
    void explicitSearchCountsAllGranularMatchesDespiteTheItemLimit() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        for (int i = 0; i < 30; i++) {
            gateway.write(MemoryRecord.savedText(
                    Instant.ofEpochSecond(i), "station visit " + i));
        }

        MemorySearchResult result = gateway.recallMatching("station", 5);

        assertEquals(30, result.matchingUnits());
        assertEquals(30, result.exactRecordCount());
        assertEquals(5, result.items().size());
        assertTrue(result.truncated());
    }

    @Test
    void explicitSearchKeepsShortExactTermsInsteadOfTreatingThemAsBlank() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        gateway.write(MemoryRecord.savedText(Instant.EPOCH, "AX beacon is active"));
        gateway.write(MemoryRecord.savedText(Instant.ofEpochSecond(1), "unrelated beacon"));

        MemorySearchResult result = gateway.recallMatching("AX", 5);

        assertEquals(1, result.matchingUnits());
        assertTrue(result.items().getFirst().contains("AX beacon"));
    }

    @Test
    void explicitSearchFallsBackToWordsWhenTheSemanticMatcherIsUnavailable() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(() -> {
            throw new IllegalStateException("embedding model unavailable");
        });
        gateway.write(MemoryRecord.savedText(Instant.EPOCH, "Colonia rendezvous"));

        MemorySearchResult result = gateway.recallMatching("Colonia", 5);

        assertEquals(1, result.matchingUnits());
        assertTrue(result.items().getFirst().contains("Colonia rendezvous"));
    }

    @Test
    void explicitSearchBoundsBothIndividualItemsAndCombinedOutput() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        for (int i = 0; i < 10; i++) {
            String text = "marker-" + i + " "
                    + "x".repeat(CompanionMemoryPolicy.savedTextMaxChars() - 11);
            gateway.write(MemoryRecord.savedText(Instant.ofEpochSecond(i), text));
        }

        MemorySearchResult result = gateway.recallMatching("marker", 25);

        assertEquals(10, result.matchingUnits());
        assertTrue(result.items().stream()
                .allMatch(item -> item.length() <= CompanionMemoryPolicy.searchItemMaxChars()));
        assertTrue(result.items().stream().mapToInt(String::length).sum()
                <= CompanionMemoryPolicy.searchResultMaxChars());
        assertTrue(result.truncated());
    }

    @Test
    void savedTextStorageRejectsOversizedOrExcessRecords() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        assertThrows(IllegalArgumentException.class, () -> gateway.write(MemoryRecord.savedText(
                Instant.EPOCH, "x".repeat(CompanionMemoryPolicy.savedTextMaxChars() + 1))));

        for (int i = 0; i < CompanionMemoryPolicy.savedTextRecordLimit(); i++) {
            gateway.write(MemoryRecord.savedText(Instant.ofEpochSecond(i), "saved " + i));
        }
        assertThrows(IllegalStateException.class, () -> gateway.write(MemoryRecord.savedText(
                Instant.MAX, "one record too many")));
    }

    @Test
    void pendingRecordsRemainSearchableUntilSummaryCommit() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        List<MemoryRecord> pending = new ArrayList<>();
        gateway.setPendingConsolidationListener(pending::add);
        int writes = CompanionMemoryPolicy.recentRecordLimit()
                + CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE) + 1;
        for (int i = 0; i < writes; i++) {
            String commander = i == 0 ? "legacy colonia marker" : "dialogue " + i;
            gateway.write(dialogue(i, commander, "reply " + i));
        }

        MemoryRecord batchRecord = pending.get(0);
        assertEquals(1, gateway.recallMatching("marker", 5).matchingUnits());
        assertEquals(List.of(batchRecord), gateway.snapshot().pendingByKind().get(MemoryKind.DIALOGUE));

        gateway.commitConsolidation(MemoryKind.DIALOGUE, List.of(batchRecord), "Earlier dialogue summarized.");

        assertTrue(gateway.snapshot().pendingByKind().isEmpty());
        assertEquals(0, gateway.recallMatching("marker", 5).matchingUnits());
    }

    @Test
    void matchingSummaryMakesHistoricalRecordCountExplicitlyUnavailable() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        List<MemoryRecord> pending = new ArrayList<>();
        gateway.setPendingConsolidationListener(pending::add);
        int writes = CompanionMemoryPolicy.recentRecordLimit()
                + CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.EVENT) + 1;
        for (int i = 0; i < writes; i++) {
            gateway.write(MemoryRecord.event(Instant.ofEpochSecond(i), "event " + i));
        }
        gateway.commitConsolidation(MemoryKind.EVENT, List.of(pending.getFirst()),
                "AX marker appeared in an older event");

        MemorySearchResult result = gateway.recallMatching("AX", 5);

        assertEquals(1, result.matchingUnits());
        assertNull(result.exactRecordCount());
        assertTrue(result.items().getFirst().startsWith("[event_summary]"));
    }

    @Test
    void retainedOverflowIsPublishedAsWholeRecord() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        List<MemoryRecord> evicted = new ArrayList<>();
        gateway.setPendingConsolidationListener(evicted::add);
        int writes = CompanionMemoryPolicy.recentRecordLimit()
                + CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE) + 1;
        for (int i = 0; i < writes; i++) {
            gateway.write(dialogue(i, "order " + i, "reply " + i));
        }

        assertEquals(1, evicted.size());
        assertEquals(Instant.EPOCH, evicted.get(0).timestamp());
        assertEquals(2, evicted.get(0).entryCount());
    }

    @Test
    void snapshotCollectionsAreImmutable() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(text -> 0);
        for (int i = 0; i <= CompanionMemoryPolicy.recentRecordLimit(); i++) {
            gateway.write(dialogue(i, "order " + i, "reply " + i));
        }
        MemorySnapshot snapshot = gateway.snapshot();

        assertThrows(UnsupportedOperationException.class, snapshot.recent()::clear);
        assertThrows(UnsupportedOperationException.class, snapshot.retainedByKind()::clear);
        snapshot.retainedByKind().values().forEach(records ->
                assertThrows(UnsupportedOperationException.class, records::clear));
        assertThrows(UnsupportedOperationException.class, snapshot.savedTexts()::clear);
    }
}
