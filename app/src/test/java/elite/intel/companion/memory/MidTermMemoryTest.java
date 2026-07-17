package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MidTermMemoryTest {

    @Test
    void dialogueAndEventHaveIndependentLimits() {
        MidTermMemory memory = new MidTermMemory();
        for (int i = 0; i <= CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE); i++) {
            memory.add(MemoryRecord.dialogue(Instant.ofEpochSecond(i), "order " + i, "reply " + i));
        }
        MemoryRecord event = MemoryRecord.event(Instant.ofEpochSecond(1000), "arrived");
        memory.add(event);

        List<MemoryRecord> evicted = memory.stageOverflow();

        assertEquals(1, evicted.size());
        assertEquals(Instant.EPOCH, evicted.get(0).timestamp());
        assertEquals(CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE),
                memory.retainedSnapshot().get(MemoryKind.DIALOGUE).size());
        assertEquals(List.of(evicted.get(0)), memory.pendingSnapshot().get(MemoryKind.DIALOGUE));
        assertEquals(CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.DIALOGUE) + 1,
                memory.records(MemoryKind.DIALOGUE).size());
        assertEquals(List.of(event), memory.records(MemoryKind.EVENT));
    }

    @Test
    void rejectsKindsWithoutRetainedStorage() {
        MidTermMemory memory = new MidTermMemory();

        assertThrows(IllegalArgumentException.class,
                () -> memory.add(MemoryRecord.savedText(Instant.EPOCH, "remember this")));
    }

    @Test
    void delayedCompressedRecordKeepsRetainedHistoryChronological() {
        MidTermMemory memory = new MidTermMemory();
        MemoryRecord later = MemoryRecord.event(Instant.ofEpochSecond(2), "later");
        MemoryRecord delayed = MemoryRecord.event(Instant.ofEpochSecond(1), "compressed later");

        memory.add(later);
        memory.add(delayed);

        assertEquals(List.of(delayed, later), memory.records(MemoryKind.EVENT));
    }

    @Test
    void semanticDialogueDuplicateKeepsOnlyTheNewestWholeRecord() {
        MidTermMemory memory = new MidTermMemory();
        MemoryRecord first = embeddedDialogue(1, "where are we", "we are in Sol",
                new float[]{1, 0}, new float[]{0, 1});
        MemoryRecord newest = embeddedDialogue(2, "what system is this", "our system is Sol",
                new float[]{1, 0}, new float[]{0, 1});

        memory.add(first);
        memory.add(newest);

        assertEquals(List.of(newest), memory.records(MemoryKind.DIALOGUE));
    }

    @Test
    void dialogueWithChangedReplyIsNotADuplicate() {
        MidTermMemory memory = new MidTermMemory();
        MemoryRecord first = embeddedDialogue(1, "where are we", "we are in Sol",
                new float[]{1, 0}, new float[]{0, 1});
        MemoryRecord changed = embeddedDialogue(2, "what system is this", "we are in Achenar",
                new float[]{1, 0}, new float[]{1, 0});

        memory.add(first);
        memory.add(changed);

        assertEquals(List.of(first, changed), memory.records(MemoryKind.DIALOGUE));
    }

    @Test
    void eventsCollapseOnlyOnExactTextAndKeepTheNewestOccurrence() {
        MidTermMemory memory = new MidTermMemory();
        MemoryRecord first = MemoryRecord.event(Instant.ofEpochSecond(1), "docked at Sol");
        MemoryRecord similar = MemoryRecord.event(Instant.ofEpochSecond(2), "docked at Achenar");
        MemoryRecord repeated = MemoryRecord.event(Instant.ofEpochSecond(3), "docked at Sol");

        memory.add(first);
        memory.add(similar);
        memory.add(repeated);

        assertEquals(List.of(similar, repeated), memory.records(MemoryKind.EVENT));
    }

    @Test
    void pendingDuplicateIsNotAddedAgain() {
        MidTermMemory memory = new MidTermMemory();
        MemoryRecord oldest = MemoryRecord.event(Instant.EPOCH, "jump complete");
        memory.add(oldest);
        for (int i = 1; i <= CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.EVENT); i++) {
            memory.add(MemoryRecord.event(Instant.ofEpochSecond(i), "event " + i));
        }
        memory.stageOverflow();

        memory.add(MemoryRecord.event(Instant.ofEpochSecond(1000), "jump complete"));

        assertEquals(List.of(oldest), memory.pendingSnapshot().get(MemoryKind.EVENT));
        assertEquals(CompanionMemoryPolicy.midTermRecordLimit(MemoryKind.EVENT),
                memory.retainedSnapshot().get(MemoryKind.EVENT).size());
    }

    private static MemoryRecord embeddedDialogue(
            long second,
            String commander,
            String companion,
            float[] commanderEmbedding,
            float[] companionEmbedding
    ) {
        MemoryRecord record = MemoryRecord.dialogue(Instant.ofEpochSecond(second), commander, companion);
        return record.withEntries(List.of(
                record.entries().get(0).withEmbedding(commanderEmbedding),
                record.entries().get(1).withEmbedding(companionEmbedding)));
    }
}
