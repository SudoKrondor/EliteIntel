package elite.intel.companion.input.en;

import elite.intel.companion.CompanionConfig;
import elite.intel.companion.input.CompanionEvalHarness;
import elite.intel.companion.memory.CompanionMemoryLimits;
import elite.intel.companion.memory.HeuristicTokenEstimator;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.gameapi.SensorDataEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme (English): drive a {@code NarrationThought} with payloads shaped like the real subscribers emit
 * ({@code SensorDataEvent}: route arrival, trade reminder, SAA signals with an exobiology genus list, a full
 * system body dump, an organic-sample report) and assess two things end-to-end:
 * <ol>
 *   <li><b>Volume in vs out.</b> The incoming data (which can be large) must be phrased into a single short
 *       spoken line - narration compresses, it does not echo the payload.</li>
 *   <li><b>No prompt bloat.</b> Only that short line is persisted (the raw data is not), and short-term memory
 *       stays within its entry cap and token budget, so it never bloats the commander prompt later.</li>
 * </ol>
 * It also re-checks the {@code NarrationThought} contract per turn: one LLM round, only the narration system
 * tools ({@code speak}/{@code nothing_to_do}). Opt-in via the local-integration tag; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NarrationEvalTest {

    /** A narration payload shaped like its real subscriber: pre-digested {@code data} + a narration {@code instruction}. */
    private record Item(String name, String data, String instruction, String topic) {}

    /** Same character heuristic the memory gateway uses, so the footprint numbers match the real eviction. */
    private static final HeuristicTokenEstimator EST = new HeuristicTokenEstimator();

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-narration-eval-trace.txt");

    private final List<Item> items = List.of(
            new Item("route-arrival",
                    "Arrived in Shinrarta Dezhra. Next waypoint Sol, star class G, scoopable. 6 jumps remaining.",
                    "Announce this route information.", SensorDataEvent.TOPIC_NAVIGATION),
            new Item("trade-reminder",
                    "Head to Jameson Memorial buy Tritium",
                    "Remind the commander of their active trade route: state the station name and the commodity to buy.",
                    SensorDataEvent.TOPIC_TRADE),
            new Item("saa-signals",
                    "Signals found. Signal type: Geological. Signal type: Biological. Exobiology: three forms. "
                            + "Bacterium Aurasus, Stratum Tectonicas, Tussock Pennata, average projected payment "
                            + "18,500,000 credits. First discovery bonus 92,000,000 credits.",
                    """
                            Report the signals detected on this body. List each signal type briefly.
                            If biological signals are present, name each genus and state the average projected payout.
                            If this is our first discovery, include the first-discovery bonus.
                            """, SensorDataEvent.TOPIC_SYSTEM),
            new Item("system-scan-dump",
                    // A deliberately large, realistic body dump - the kind of full-volume data an arrival can carry.
                    "System scan complete, eight bodies. "
                            + "Body A 1: high metal content world, landable, gravity 0.31g, 1280 kelvin, terraformable, "
                            + "materials iron, nickel, sulphur, carbon, phosphorus, manganese. "
                            + "Body A 2: water world, not landable, gravity 1.12g, 290 kelvin, atmosphere nitrogen oxygen. "
                            + "Body A 3: gas giant with water-based life, two rings, reserves pristine. "
                            + "Body A 4: rocky body, landable, gravity 0.08g, 180 kelvin, materials antimony, polonium. "
                            + "Body A 5: ammonia world, not landable, gravity 1.4g, 240 kelvin. "
                            + "Body B 1: metal-rich body, landable, gravity 0.22g, 720 kelvin, materials ruthenium, tellurium. "
                            + "Body B 2: icy body with biological and geological signals, landable, gravity 0.05g. "
                            + "Two bodies remain unscanned.",
                    "Summarize the system scan for the commander in one short line; do not list every body.",
                    SensorDataEvent.TOPIC_SYSTEM),
            new Item("organic-sample",
                    "Sample taken. Stratum Tectonicas, sample two of three. Move at least 500 metres for the next sample.",
                    "Report the sample progress to the commander.", SensorDataEvent.TOPIC_SYSTEM),
            new Item("route-arrival-2",
                    "Arrived in LHS 20. Next waypoint Wolf 359, star class M, scoopable. 3 jumps remaining.",
                    "Announce this route information.", SensorDataEvent.TOPIC_NAVIGATION));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void narratesRealEventsCompactlyWithoutBloatingShortTermMemory() throws Exception {
        StringBuilder block = new StringBuilder("\n======== NARRATION volume / bloat (real events) ========\n");
        block.append(String.format("%-18s | in(ch/tok) | out(ch/tok) | round | tools | spoken%n", "event"));
        block.append("-".repeat(130)).append("\n");

        int spokeCount = 0;
        int contractOk = 0;
        for (Item it : items) {
            long roundsBefore = h.roundCount();
            h.narrate(it.data(), it.instruction(), it.topic());
            long rounds = h.roundCount() - roundsBefore; // narration is a single short round

            int inChars = it.data().length() + it.instruction().length();
            String spoken = String.join(" ", h.spokenTexts());
            boolean spoke = !spoken.isBlank();
            boolean oneRound = rounds == 1;
            boolean onlyNarrationTools = h.turnToolNames().stream()
                    .allMatch(t -> t.equals("speak") || t.equals("nothing_to_do"));
            if (spoke) {
                spokeCount++;
            }
            if (spoke && oneRound && onlyNarrationTools) {
                contractOk++;
            }
            block.append(String.format("%-18s | %4d/%-4d | %4d/%-4d | %-5s | %-5s | %s%n",
                    it.name(),
                    inChars, EST.estimate(it.data()) + EST.estimate(it.instruction()),
                    spoken.length(), EST.estimate(spoken),
                    oneRound ? "1" : String.valueOf(rounds),
                    onlyNarrationTools ? "ok" : "BAD",
                    spoken));
            block.append(h.memoryDeltaBlock()); // narration records only the spoken [companion] line
        }

        // Final short-term footprint: what narration actually leaves in the prompt's timeline block.
        List<MemoryEntry> timeline = h.memory().readShortTermTimeline();
        int entries = timeline.size();
        int footprintTokens = timeline.stream()
                .mapToInt(e -> EST.estimate(e.content()) + CompanionMemoryLimits.SHORT_TERM_ENTRY_FRAMING_OVERHEAD_TOKENS)
                .sum();
        int maxEntryTokens = timeline.stream().mapToInt(e -> EST.estimate(e.content())).max().orElse(0);

        block.append(String.format("%ncontract ok (spoke, 1 round, narration tools): %d / %d%n", contractOk, items.size()));
        block.append(String.format("short-term: %d / %d entries, footprint ~%d tok (budget %d), largest entry ~%d tok%n",
                entries, CompanionConfig.shortTermMemorySize(), footprintTokens,
                CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET, maxEntryTokens));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        assertTrue(spokeCount > 0, "narration never produced a spoken line - see the trace");
        // No prompt bloat: short-term honors its entry cap, and its token footprint stays within budget
        // (the sole-newest-entry exception aside) - a large incoming payload does not bloat the timeline.
        assertTrue(entries <= CompanionConfig.shortTermMemorySize(),
                "short-term exceeded its entry cap: " + entries + " > " + CompanionConfig.shortTermMemorySize());
        assertTrue(footprintTokens <= CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET || entries <= 1,
                "short-term footprint ~" + footprintTokens + " tok exceeded budget "
                        + CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET + " with " + entries + " entries");
    }
}
