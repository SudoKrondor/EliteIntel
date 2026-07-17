package elite.intel.companion.model.memory;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryRecordTest {

    @Test
    void factoriesCreateProtocolValidShapes() {
        assertEquals(List.of(MemorySource.COMMANDER, MemorySource.COMPANION),
                MemoryRecord.dialogue(Instant.EPOCH, "hello", "hello").entries().stream()
                        .map(MemoryEntry::source).toList());
        assertEquals(List.of(MemorySource.EVENT),
                MemoryRecord.event(Instant.EPOCH, "arrived").entries().stream()
                        .map(MemoryEntry::source).toList());
        assertEquals(2, MemoryRecord.query(Instant.EPOCH, "cargo?", "empty").entryCount());
        assertEquals("remember this", MemoryRecord.savedText(Instant.EPOCH, "remember this")
                .entries().get(0).content());
    }

    @Test
    void rejectsPartialDialogueAndQuery() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryRecord(
                Instant.EPOCH, MemoryKind.DIALOGUE,
                List.of(new MemoryEntry(MemorySource.COMMANDER, "hello"))));
        assertThrows(IllegalArgumentException.class, () -> new MemoryRecord(
                Instant.EPOCH, MemoryKind.QUERY, List.of(
                        new MemoryEntry(MemorySource.COMMANDER, "cargo?"),
                        new MemoryEntry(MemorySource.SYSTEM, "empty"))));
    }

    @Test
    void derivedEmbeddingDoesNotChangeEntryIdentity() {
        MemoryEntry plain = new MemoryEntry(MemorySource.EVENT, "arrived at Sol");

        assertEquals(plain, plain.withEmbedding(new float[]{1.0f, 0.5f}));
    }

    @Test
    void embeddingCannotBeMutatedThroughEitherArrayReference() {
        float[] source = {1.0f, 0.5f};
        MemoryEntry entry = new MemoryEntry(MemorySource.EVENT, "arrived at Sol", source);

        source[0] = 0.0f;
        float[] exposed = entry.embedding();
        exposed[1] = 0.0f;

        assertEquals(1.0f, entry.embedding()[0]);
        assertEquals(0.5f, entry.embedding()[1]);
    }
}
