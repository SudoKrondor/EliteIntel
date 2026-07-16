package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.gameapi.journal.events.BaseEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One comprehensive English memory evaluation over a simulated salvage-run conversation. It covers recent and
 * retained dialogue, trusted event facts, exact SAVED_TEXT phrases, multi-fact coherence, and the boundary between
 * memory answers and live-state queries. The trace carries the record-kind distribution and recall results;
 * hard assertions require the live model and successful recall. Opt-in; LM Studio must be available.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemoryEvalTest {

    private enum Kind { SAY, EVENT, ASK }

    /** SAY: {@code a}=commander line. EVENT: {@code a}=journal type, {@code b}=summary. ASK: {@code a}=question, {@code b}=expected keyword. */
    private record Turn(Kind kind, String a, String b) {}

    private static Turn say(String line) { return new Turn(Kind.SAY, line, null); }
    private static Turn event(String type, String summary) { return new Turn(Kind.EVENT, type, summary); }
    private static Turn ask(String question, String expect) { return new Turn(Kind.ASK, question, expect); }

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-memory-eval-trace.txt");

    // A believable session with dialogue, explicit SAVED_TEXT phrases, trusted events, and recent recall.
    private final List<Turn> script = List.of(
            say("alright, the plan this run is a quiet salvage job out past Deciat, keep it off the books"),
            say("remember our docking authorization code is Sierra Nine Four, we'll need it at the station"),
            event("FSDJump", "arrived in the Wolf 359 system"),
            say("the buyer for the salvage goes by Halloran, he's our contact at the market"),
            say("nice and quiet out here, just the way I like it"),
            say("if a pirate jumps us, the codeword to break off the run is Granite, burn that in"),
            event("ShipTargeted", "scanned a wanted pirate named Vargas"),
            say("while we're out here our mining target is low temperature diamonds, nothing else"),
            say("old Magpie is flying cover for us, stick near his wing"),
            say("note down that our emergency rendezvous is Hutton Orbital if we get separated"),
            event("MissionAccepted", "accepted a massacre mission against the Code faction"),
            ask("what's the codeword to break off the run?", "granite"), // still hot: no recall needed
            say("we logged an ammonia world out here and nicknamed it Lantern"),
            event("MarketSell", "sold forty tons of osmium at the market"),
            say("how are you holding up over there"),
            say("we top off the tanks at the neutron star they call Spindle on the way home"),
            say("our fighter pilot in the SLF is named Okonkwo"),
            say("and we finally christened the ship the Wandering Albatross"));

    // After the run the early facts have been pushed into mid-term, so these need a real recall.
    private final List<Turn> recallProbes = List.of(
            ask("what's our docking authorization code?", "sierra"),     // MAX
            ask("where's our emergency rendezvous?", "hutton"),          // MAX
            ask("who's the buyer for the salvage?", "halloran"),
            ask("what's our mining target this run?", "diamonds"),
            ask("who's flying cover for us?", "magpie"),
            ask("what did we christen the ship?", "albatross"));

    // One coherence probe: the answer should weave together two separately-stated facts.
    private final Turn coherenceProbe = ask("remind me the abort codeword and where we rendezvous if separated", "granite");

    // Live-state questions: must route to a query function, not a memory recall.
    private final List<String> queryProbes = List.of(
            "what's our current fuel level", "what's in the cargo hold right now", "what's our current location");

    // Keywords planted only by events, to check each HIGH event landed in some memory tier.
    private final List<String> eventKeywords = List.of("wolf", "vargas", "massacre", "osmium");

    /** System-function ids; any other executed tool is a real game query/action. */
    private static final Set<String> SYSTEM_TOOLS = Set.of(
            "speak", "request_input", "memory_search");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void remembersFillsDistributesAndRecallsAcrossAConversation() throws Exception {
        StringBuilder block = new StringBuilder("\n======== COMPREHENSIVE MEMORY (live conversation) ========\n");

        // Phase 1: play the session. ASK turns here are scored as in-conversation recall.
        int hotHits = 0;
        int hotAsks = 0;
        for (Turn turn : script) {
            switch (turn.kind()) {
                case SAY -> {
                    h.say(turn.a());
                    block.append("[COMMANDER] ").append(turn.a()).append("\n");
                    h.spokenTexts().forEach(s -> block.append("[VEGA] ").append(s).append("\n"));
                }
                case EVENT -> {
                    h.gameEvent(turn.a(), turn.b(), BaseEvent.Importance.HIGH);
                    block.append("[EVENT ").append(turn.a()).append("] ").append(turn.b()).append("\n");
                }
                case ASK -> {
                    hotAsks++;
                    h.say(turn.a());
                    boolean hit = h.spokenContains(turn.b());
                    if (hit) {
                        hotHits++;
                    }
                    block.append("[COMMANDER] ").append(turn.a()).append("\n");
                    block.append("    -> expect '").append(turn.b()).append("' hot-hit=").append(hit).append(" | ").append(h.spokenTexts()).append("\n");
                }
            }
        }

        // Phase 2: recall probes - the facts are no longer hot, so a correct answer needs a real recall.
        block.append("\n---- recall after eviction ----\n");
        int recallHits = 0;
        int recalledCount = 0;
        for (Turn probe : recallProbes) {
            String tier = h.locateTier(probe.b());
            h.beginTurn();
            h.say(probe.a());
            boolean hit = h.spokenContains(probe.b());
            boolean recalled = h.recalled();
            if (hit) {
                recallHits++;
            }
            if (recalled) {
                recalledCount++;
            }
            block.append(String.format("expect '%s' | tier=%s | recalled=%s | hit=%s | %s%n",
                    probe.b(), tier, recalled, hit, h.spokenTexts()));
        }

        // Phase 3: coherence - one answer drawing on two separately-stated facts.
        h.beginTurn();
        h.say(coherenceProbe.a());
        String coherent = String.join(" ", h.spokenTexts()).toLowerCase(Locale.ROOT);
        boolean coherenceOk = coherent.contains("granite") && coherent.contains("hutton");
        block.append("\n---- coherence ----\n");
        block.append("expect both 'granite' + 'hutton' | ok=").append(coherenceOk).append(" | ").append(h.spokenTexts()).append("\n");

        // Phase 4: events landed in memory.
        block.append("\n---- events recorded ----\n");
        int eventsLanded = 0;
        for (String kw : eventKeywords) {
            String tier = h.locateTier(kw);
            boolean landed = !"LOST".equals(tier);
            if (landed) {
                eventsLanded++;
            }
            block.append(String.format("event keyword '%s' | tier=%s%n", kw, tier));
        }

        // Phase 5: live-state routing - must use a query, not memory.
        block.append("\n---- query routing (live state must not use memory) ----\n");
        int routedOk = 0;
        for (String q : queryProbes) {
            h.beginTurn();
            h.say(q);
            List<String> tools = h.turnToolNames();
            boolean usedQuery = tools.stream().anyMatch(t -> !SYSTEM_TOOLS.contains(t));
            boolean ok = usedQuery && !h.recalled();
            if (ok) {
                routedOk++;
            }
            block.append(String.format("%-38s | tools=%s | routed-ok=%s%n", q, tools, ok));
        }

        boolean explicitFactsPinned = h.memory().savedTextRecords().stream()
                .flatMap(record -> record.entries().stream())
                .anyMatch(entry -> entry.content().toLowerCase(Locale.ROOT).contains("sierra"));

        block.append("\n---- scores ----\n");
        block.append(String.format("in-conversation recall: %d / %d%n", hotHits, hotAsks));
        block.append(String.format("recall after eviction:  %d / %d (candidates injected %d)%n", recallHits, recallProbes.size(), recalledCount));
        block.append(String.format("coherence (2 facts):    %s%n", coherenceOk ? "ok" : "no"));
        block.append(String.format("events recorded:        %d / %d%n", eventsLanded, eventKeywords.size()));
        block.append(String.format("query routing:          %d / %d%n", routedOk, queryProbes.size()));
        block.append(String.format("explicit remember -> SAVED_TEXT: %s%n", explicitFactsPinned));
        block.append(h.memoryDistributionBlock());
        block.append(h.recentMemoryDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(recallHits > 0, "no fact was recalled after eviction - see the trace");
    }
}
