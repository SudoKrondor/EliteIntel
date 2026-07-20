package elite.intel.ai.brain.actions.handlers.queries;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemorySearchQueryTest {

    @Test
    void structuredRecallCarriesExactCountAndBoundedItems() {
        MemorySearchQuery.Remembered remembered = new MemorySearchQuery.Remembered(
                4, 4, true, List.of("[EVENT] docked at Jameson", "[EVENT] docked at Mars High"));

        assertEquals(4, remembered.exactRecordCount());
        assertEquals(4, remembered.matchingUnits());
        assertTrue(remembered.truncated());
        assertEquals(2, remembered.items().size());
        assertTrue(remembered.toYaml().contains("exactRecordCount: 4"));
    }

    @Test
    void emptyRecallIsNotTruncated() {
        MemorySearchQuery.Remembered remembered = new MemorySearchQuery.Remembered(0, 0, false, List.of());

        assertFalse(remembered.truncated());
        assertTrue(remembered.items().isEmpty());
    }
}
