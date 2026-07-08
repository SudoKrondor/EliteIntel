package elite.intel.ai.brain.actions.handlers.query;

import elite.intel.companion.prompt.Fact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemorySearchQueryTest {

    @Test
    void appendsSourceFactsAfterRecalledEntries() {
        List<String> result = MemorySearchQuery.merge(
                List.of("docked at jameson", "sold gold"),
                List.of(new Fact("in sol", "system"), new Fact("hull at 80%", "ship")));

        assertEquals(List.of("docked at jameson", "sold gold", "in sol", "hull at 80%"), result);
    }

    @Test
    void dropsASourceFactAlreadyPresentInRecalledEntriesCaseInsensitive() {
        List<String> result = MemorySearchQuery.merge(
                List.of("In Sol"),
                List.of(new Fact("in sol", "system"), new Fact("hull at 80%", "ship")));

        assertEquals(List.of("In Sol", "hull at 80%"), result);
    }

    @Test
    void dropsDuplicateFactsAcrossSources() {
        List<String> result = MemorySearchQuery.merge(
                List.of(),
                List.of(new Fact("in sol", "system"), new Fact("In Sol", "nav")));

        assertEquals(List.of("in sol"), result);
    }

    @Test
    void returnsRecalledEntriesWhenNoSourceFacts() {
        assertEquals(List.of("docked at jameson"),
                MemorySearchQuery.merge(List.of("docked at jameson"), List.of()));
    }

    @Test
    void emptyWhenNothingRecalledAndNoSourceFacts() {
        assertTrue(MemorySearchQuery.merge(List.of(), List.of()).isEmpty());
    }
}
