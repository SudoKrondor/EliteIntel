package elite.intel.ai.brain.vega.memory.facts;

import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactGatherer;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSource;
import elite.intel.ai.brain.vega.prompt.Fact;
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
            @Override
            public String id() {
                return "boom";
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                throw new IllegalStateException("boom");
            }
        };

        assertEquals(List.of(new Fact("ok", "good")),
                MemoryFactGatherer.gather(ctx(), List.of(boom, source("good", "ok"))));
    }

    @Test
    void treatsANullReturnAsNoFacts() {
        MemoryFactSource nully = new MemoryFactSource() {
            @Override
            public String id() {
                return "nully";
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                return null;
            }
        };

        assertTrue(MemoryFactGatherer.gather(ctx(), List.of(nully)).isEmpty());
    }

    @Test
    void dropsBlankFactsAndStripsWhitespace() {
        assertEquals(List.of(new Fact("trimmed", "s")),
                MemoryFactGatherer.gather(ctx(), List.of(source("s", "  ", "  trimmed  "))));
    }

    @Test
    void gatherRelevantDelegatesTheDecisionToEachSource() {
        MemoryFactSource relevant = new MemoryFactSource() {
            @Override
            public String id() {
                return "relevant";
            }

            @Override
            public boolean isRelevant(MemoryFactContext context) {
                return true;
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                return List.of("included");
            }
        };
        MemoryFactSource irrelevant = new MemoryFactSource() {
            @Override
            public String id() {
                return "irrelevant";
            }

            @Override
            public boolean isRelevant(MemoryFactContext context) {
                return false;
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                throw new AssertionError("factsFor must not run for an irrelevant source");
            }
        };

        assertEquals(List.of(new Fact("included", "relevant")),
                MemoryFactGatherer.gatherRelevant(ctx(), List.of(relevant, irrelevant)));
    }

    /**
     * The block's total cap is small, so the order the sources are asked in decides who loses a slot. A fact that
     * answers the commander's actual subject must outrank one that speaks on every turn.
     */
    @Test
    void gatherRelevantPutsSubjectSourcesBeforeStandingContext() {
        MemoryFactSource ambient = new MemoryFactSource() {
            @Override
            public String id() {
                return "ambient";
            }

            @Override
            public boolean isRelevant(MemoryFactContext context) {
                return true;
            }

            @Override
            public boolean isAmbient() {
                return true;
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                return List.of("standing context");
            }
        };
        MemoryFactSource subject = new MemoryFactSource() {
            @Override
            public String id() {
                return "subject";
            }

            @Override
            public boolean isRelevant(MemoryFactContext context) {
                return true;
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                return List.of("what was asked about");
            }
        };

        // Registered ambient-first, so only the ordering rule can put the subject source in front.
        assertEquals(List.of(
                        new Fact("what was asked about", "subject"),
                        new Fact("standing context", "ambient")),
                MemoryFactGatherer.gatherRelevant(ctx(), List.of(ambient, subject)));
    }

    @Test
    void gatherRelevantIsolatesAThrowingRelevanceCheck() {
        MemoryFactSource broken = new MemoryFactSource() {
            @Override
            public String id() {
                return "broken";
            }

            @Override
            public boolean isRelevant(MemoryFactContext context) {
                throw new IllegalStateException("boom");
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                return List.of("must not appear");
            }
        };
        MemoryFactSource healthy = new MemoryFactSource() {
            @Override
            public String id() {
                return "healthy";
            }

            @Override
            public boolean isRelevant(MemoryFactContext context) {
                return true;
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                return List.of("ok");
            }
        };

        assertEquals(List.of(new Fact("ok", "healthy")),
                MemoryFactGatherer.gatherRelevant(ctx(), List.of(broken, healthy)));
    }

    private static MemoryFactContext ctx() {
        return MemoryFactContext.forCommanderInput("q");
    }

    private static MemoryFactSource source(String id, String... facts) {
        return new MemoryFactSource() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public List<String> factsFor(MemoryFactContext context) {
                return List.of(facts);
            }
        };
    }
}
