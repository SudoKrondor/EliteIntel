package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.model.memory.MemoryImportance;
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
 * Theme: one comprehensive English memory eval over a simulated live conversation, replacing the separate
 * short-term / mid-term / long-term evals. A believable salvage-run session interleaves commander statements
 * across many topics, explicit "remember" instructions (which the consciousness should rate {@code MAX}),
 * routine chatter, and HIGH game events that land in memory under their static topic. It then assesses, from
 * one run:
 * <ul>
 *   <li><b>filling &amp; recall</b> - facts stated early (pushed out of the hot timeline into mid-term) are
 *       recalled later, including the companion's own lines;</li>
 *   <li><b>importance distribution</b> - the AI assigns LOW/NORMAL/HIGH, and explicit "remember" facts are
 *       MAX;</li>
 *   <li><b>topic distribution</b> - statements are filed across topics, events under their static topic;</li>
 *   <li><b>events in memory</b> - HIGH events are recorded and findable;</li>
 *   <li><b>coherence</b> - a multi-fact question is answered from several remembered facts;</li>
 *   <li><b>routing</b> - live-state questions go to a query function, not memory recall.</li>
 * </ul>
 * Mostly observational (the trace carries the scores and the full memory distribution); the hard assertions
 * are only that the model was reached and that recall works at all. Opt-in; LM Studio must be up.
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

    // A believable session: statements across topics, explicit remembers (-> MAX), chatter (-> LOW), HIGH game
    // events (mapped types, recorded under their static topic), and one in-flight recall while still hot.
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
            "speak", "nothing_to_do", "change_global_topic", "change_verbosity", "clarify",
            "search_in_memory", "set_importance");

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

        // Did the explicit "remember" facts get MAX importance from the AI?
        boolean maxAssigned = h.allEntries().stream().anyMatch(e -> e.importance() == MemoryImportance.MAX
                && (e.content().contains("sierra") || e.content().contains("hutton")));

        block.append("\n---- scores ----\n");
        block.append(String.format("in-conversation recall: %d / %d%n", hotHits, hotAsks));
        block.append(String.format("recall after eviction:  %d / %d (search_in_memory called %d)%n", recallHits, recallProbes.size(), recalledCount));
        block.append(String.format("coherence (2 facts):    %s%n", coherenceOk ? "ok" : "no"));
        block.append(String.format("events recorded:        %d / %d%n", eventsLanded, eventKeywords.size()));
        block.append(String.format("query routing:          %d / %d%n", routedOk, queryProbes.size()));
        block.append(String.format("explicit-remember -> MAX assigned: %s%n", maxAssigned));
        block.append(h.memoryDistributionBlock());
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(recallHits > 0, "no fact was recalled after eviction - see the trace");
    }
}
