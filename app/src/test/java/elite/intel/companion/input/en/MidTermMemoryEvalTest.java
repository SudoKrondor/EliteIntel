package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
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

/**
 * Theme 4 (English): filling MID-TERM topic memory across many topics, the quality of automatic topic
 * detection, and the use of {@code search_in_memory}. Facts are stated as a natural mission briefing with no
 * topic cues - the model decides each fact's topic on its own ({@code change_global_topic}); they are then
 * pushed out of short-term by a stretch of topic-spread ops chatter, so by probe time the fact is no longer
 * in the hot timeline and a correct answer needs a real recall. For each of the 20 probes it scores: which
 * topic actually holds the fact (topic-detection quality), whether {@code search_in_memory} was called, and
 * whether the spoken answer carries the planted detail. A final phase probes live-state questions (cargo,
 * fuel, location, ...) to check the model routes those to a query function rather than digging in memory -
 * the regression risk of pushing recall harder. The trace ends with a full dump of the companion's memory
 * (mid-term by topic, llm_memory, long-term summary, short-term). Opt-in; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MidTermMemoryEvalTest {

    /** A fact stated with no topic cue, probed after eviction; {@code expectedTopic} is the natural topic we
     *  expect the model to file it under, {@code keyword} the planted detail expected back in the answer (it
     *  also appears in {@code plant}, so it locates the fact across the memory tiers). */
    private record Probe(String plant, String question, String expectedTopic, String keyword) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-midterm-eval-trace.txt");

    private final List<Probe> probes = List.of(
            new Probe("the plan is to rendezvous at the third moon of Maia, that's where we meet the others",
                    "where were we supposed to meet up again?", "navigation", "maia"),
            new Probe("tag the jump-in beacon for that system as Kestrel so we don't lose it",
                    "what did we tag the jump-in beacon as?", "navigation", "kestrel"),
            new Probe("we're not taking the main lanes, we're running the quiet route we call the Backroad",
                    "what did we nickname the route we're taking?", "navigation", "backroad"),
            new Probe("the buyer picking up the cargo goes by Halloran, he's our contact at the station",
                    "who's the buyer we're selling to?", "trade", "halloran"),
            new Probe("for the record, we stashed the painite in cargo rack seven",
                    "what did we stash in rack seven?", "trade", "painite"),
            new Probe("the buyer also put in an order for a load of tritium on the side",
                    "what else did the buyer want besides the main cargo?", "trade", "tritium"),
            new Probe("our mining target this run is low temperature diamonds, nothing else is worth the time",
                    "what's our mining target this run?", "mining", "diamonds"),
            new Probe("the hotspot we're working is the one they call Wrecker's Reach",
                    "which hotspot were we headed to for mining?", "mining", "wrecker"),
            new Probe("the asteroid field around it goes by Bedlam on the charts",
                    "what's the name of the asteroid field we're working?", "mining", "bedlam"),
            new Probe("watch for a pirate that runs these lanes, goes by Vargas, nasty piece of work",
                    "what was the name of that pirate to watch for?", "combat", "vargas"),
            new Probe("if it all goes wrong, our codeword to break off the run is Granite",
                    "what's our codeword to break off?", "combat", "granite"),
            new Probe("I've mapped the rail guns to fire group Bravo, remember that in a fight",
                    "which fire group did we map the rail guns to?", "combat", "bravo"),
            new Probe("we logged an ammonia world out here and nicknamed it Lantern",
                    "what did we nickname that ammonia world we found?", "exploration", "lantern"),
            new Probe("we top off the tanks at that neutron star they call Spindle on the way out",
                    "where do we top off the fuel?", "exploration", "spindle"),
            new Probe("the whole job is contracted by the Mokosh Blue Syndicate, that's who pays us",
                    "who's the job for, which faction?", "missions", "mokosh"),
            new Probe("command has this op filed as Operation Lowtide, use that name on the records",
                    "what's this operation filed as?", "missions", "lowtide"),
            new Probe("old Magpie is flying cover for us this run, stick near his wing",
                    "who's flying cover for us?", "crew", "magpie"),
            new Probe("our fighter pilot, the one in the SLF, is named Okonkwo",
                    "what's our fighter pilot's name?", "crew", "okonkwo"),
            new Probe("we're taking the frame shift drive to Felicity Farseer to get it tuned",
                    "which engineer are we taking the FSD to?", "engineering", "farseer"),
            new Probe("and we finally christened the ship the Wandering Albatross, about time",
                    "what did we christen the ship?", "ship_status", "albatross"));

    // Keyword-free, topic-spread ops chatter that pushes the planted facts out of the 20-slot hot timeline, so
    // eviction is not concentrated in one topic (which would overflow that topic past mid-term).
    private static final List<String> FILLER = List.of(
            "what's our current location", "how much fuel do we have left", "any contacts on the scanner right now",
            "what's the security level in this system", "how's the hull holding up", "what's the nearest station",
            "what time is it back home", "are the shields back to full", "what's the market like at the next port",
            "how many limpets are left in the hold", "what's the next scoopable star on the route",
            "is there anything worth scanning here", "how's the power distribution set",
            "any jobs on the board worth a look", "how far to the next jump", "is the wing still formed up");

    /** System-function ids; any other tool executed in a turn is a query/action (a real game tool). */
    private static final Set<String> SYSTEM_TOOLS = Set.of(
            "speak", "nothing_to_do", "change_global_topic", "change_verbosity", "remember", "clarify",
            "search_in_memory", "find_action");

    // Live-state questions that must be answered by a query function, not from memory - the routing the
    // sharpened recall rule must not break (these are the live state, not a remembered session fact).
    private static final List<String> QUERY_PROBES = List.of(
            "what's in our cargo hold right now", "how much fuel do we have left", "what's our current location",
            "what's the security level in this system", "are there any contacts on the scanner",
            "what's the nearest station");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void recallsFactsByTopicAfterEviction() throws Exception {
        // Phase 1: state every fact with no topic cue - the model files each under a topic on its own.
        for (Probe p : probes) {
            h.say(p.plant());
        }
        // Phase 2: ops chatter pushes the facts out of short-term and into mid-term topic memory.
        for (String filler : FILLER) {
            h.say(filler);
        }

        // Phase 3: probe each fact; it is no longer hot, so a correct answer needs a real recall.
        List<String> report = new ArrayList<>();
        report.add(String.format("%-14s | %-26s | %-12s | %-10s | %-6s | %s",
                "expected topic", "located tier", "topic ok", "recalled", "hit", "spoken"));
        report.add("-".repeat(120));
        int hits = 0;
        for (Probe p : probes) {
            String tier = h.locateTier(p.keyword());
            String actualTopic = h.midTermTopic(p.keyword());
            boolean topicOk = p.expectedTopic().equals(actualTopic);

            h.beginTurn();
            h.say(p.question());
            boolean recalled = h.recalled();
            boolean hit = h.spokenContains(p.keyword());
            if (hit) {
                hits++;
            }
            report.add(String.format("%-14s | %-26s | %-12s | %-10s | %-6s | %s",
                    p.expectedTopic(), tier, topicOk ? "yes" : "(" + actualTopic + ")",
                    recalled ? "yes" : "no", hit ? "yes" : "no", h.spokenTexts()));
        }

        // Phase 4: live-state questions must route to a query function, not to memory - verify the sharpened
        // recall rule did not push current ship/galaxy state into search_in_memory.
        List<String> routing = new ArrayList<>();
        routing.add(String.format("%-42s | %-28s | %-8s | %-10s | %s",
                "live-state question", "tools called", "memory?", "routed ok", "spoken"));
        routing.add("-".repeat(120));
        int routedOk = 0;
        for (String q : QUERY_PROBES) {
            h.beginTurn();
            h.say(q);
            List<String> tools = h.turnToolNames();
            boolean usedMemory = h.recalled();
            boolean usedQuery = tools.stream().anyMatch(t -> !SYSTEM_TOOLS.contains(t));
            boolean ok = usedQuery && !usedMemory; // good routing: hit a query/action, did not dig in memory
            if (ok) {
                routedOk++;
            }
            routing.add(String.format("%-42s | %-28s | %-8s | %-10s | %s",
                    q, tools, usedMemory ? "yes" : "no", ok ? "yes" : "no", h.spokenTexts()));
        }

        StringBuilder block = new StringBuilder("\n======== MID-TERM MEMORY BY TOPIC (theme 4) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("memory hits: %d / %d%n", hits, probes.size()));
        block.append("\n---- QUERY ROUTING (live-state must use a query, not memory) ----\n");
        routing.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("query routing ok: %d / %d%n", routedOk, QUERY_PROBES.size()));
        block.append(companionMemoryDump());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }

    /** Full end-of-run snapshot of the companion's memory: every mid-term topic's entries, the cyclic
     *  llm_memory, the long-term summary, and the short-term timeline - so the trace shows where each fact
     *  finally settled across the tiers. */
    private String companionMemoryDump() {
        MemoryGateway m = h.memory();
        StringBuilder b = new StringBuilder("\n---- companion memory at end ----\n");
        b.append("mid-term topic memory:\n");
        List<ConversationTopic> topics = m.indexes().topicsWithMemory();
        if (topics.isEmpty()) {
            b.append("  (none)\n");
        }
        for (ConversationTopic t : topics) {
            b.append("  [").append(t.name().toLowerCase(Locale.ROOT)).append("]\n");
            for (MemoryEntry e : m.recallTopicMemory(t, null, 100)) {
                b.append("    [").append(e.source().name()).append("] ").append(e.content()).append("\n");
            }
        }
        List<String> llm = m.readLlmMemory();
        b.append("llm_memory (").append(llm.size()).append("):\n");
        for (String s : llm) {
            b.append("    ").append(s).append("\n");
        }
        String summary = m.longTermSummary();
        b.append("long-term summary:\n    ").append(summary == null || summary.isBlank() ? "(empty)" : summary).append("\n");
        b.append(h.shortTermDumpBlock());
        return b.toString();
    }
}
