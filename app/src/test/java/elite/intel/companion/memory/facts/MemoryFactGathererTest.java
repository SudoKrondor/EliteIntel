package elite.intel.companion.memory.facts;

import elite.intel.companion.prompt.Fact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryFactGathererTest {

    @Test
    void gathersFactsTaggedByEachSourceId() {
        List<Fact> facts = MemoryFactGatherer.gather(ctx(), List.of(
                source("ship", "hull at 80%"),
                source("system", "in sol", "no stations scanned")));

        assertEquals(List.of(
                new Fact("hull at 80%", "ship"),
                new Fact("in sol", "system"),
                new Fact("no stations scanned", "system")), facts);
    }

    @Test
    void skipsAThrowingSourceButKeepsTheRest() {
        MemoryFactSource boom = new MemoryFactSource() {
            @Override public String id() { return "boom"; }
            @Override public List<String> factsFor(MemoryFactContext context) { throw new IllegalStateException("boom"); }
        };

        assertEquals(List.of(new Fact("ok", "good")),
                MemoryFactGatherer.gather(ctx(), List.of(boom, source("good", "ok"))));
    }

    @Test
    void treatsANullReturnAsNoFacts() {
        MemoryFactSource nully = new MemoryFactSource() {
            @Override public String id() { return "nully"; }
            @Override public List<String> factsFor(MemoryFactContext context) { return null; }
        };

        assertTrue(MemoryFactGatherer.gather(ctx(), List.of(nully)).isEmpty());
    }

    @Test
    void dropsBlankFactsAndStripsWhitespace() {
        assertEquals(List.of(new Fact("trimmed", "s")),
                MemoryFactGatherer.gather(ctx(), List.of(source("s", "  ", "  trimmed  "))));
    }

    @Test
    void gatherForSearchIsEmptyForAmbientOnlySources() {
        // The ambient/state sources do not override searchFacts, so memory_search gets nothing from them.
        assertTrue(MemoryFactGatherer.gatherForSearch(ctx(), List.of(source("ship", "hull at 80%"))).isEmpty());
    }

    @Test
    void gatherForSearchUsesTheSearchRole() {
        MemoryFactSource searchable = new MemoryFactSource() {
            @Override public String id() { return "q"; }
            @Override public List<String> factsFor(MemoryFactContext context) { return List.of("ambient"); }
            @Override public List<String> searchFacts(MemoryFactContext context) { return List.of("searched"); }
        };

        assertEquals(List.of(new Fact("ambient", "q")), MemoryFactGatherer.gather(ctx(), List.of(searchable)));
        assertEquals(List.of(new Fact("searched", "q")), MemoryFactGatherer.gatherForSearch(ctx(), List.of(searchable)));
    }

    private static MemoryFactContext ctx() {
        return MemoryFactContext.forQuery("q");
    }

    private static MemoryFactSource source(String id, String... facts) {
        return new MemoryFactSource() {
            @Override public String id() { return id; }
            @Override public List<String> factsFor(MemoryFactContext context) { return List.of(facts); }
        };
    }
}
