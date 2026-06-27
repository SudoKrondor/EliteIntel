package elite.intel.companion.input.en;

import elite.intel.companion.input.CompanionEvalHarness;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme 3 (English): filling and using SHORT-TERM memory across a natural conversation. One continuous
 * smuggling-run dialogue - the commander states facts and, interleaved, asks the companion to recall
 * earlier ones while they are still in the hot session timeline that is inlined into the prompt, so a
 * correct answer needs no recall at all. Two questions ask about what the COMPANION itself said ("what did
 * you confirm…", "how did you read back…"), exercising recall of its own [COMPANION] lines. Each fact is a
 * pure session detail (codeword / name) with no game-query twin and a distinctive keyword that survives in
 * the spoken answer. Opt-in via the local-integration tag; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShortTermMemoryEvalTest {

    /** One conversation turn: {@code expect == null} is a commander statement, otherwise a question whose
     *  spoken answer must contain {@code expect}. */
    private record Step(String text, String expect) {}

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-shortterm-eval-trace.txt");

    private final List<Step> script = List.of(
            new Step("alright crew, this little contraband hop across the border, let's call the whole run Cakewalk, nice and quiet", null),
            new Step("we've got old Magpie flying cover for us, so stick close to his wing", null),
            new Step("the buyer picking up the cargo at the station goes by Halloran, he's the one we deal with", null),
            new Step("if this thing turns sour, the abort word for the deal is Granite, burn that into your memory", null),
            new Step("remind me, what did we end up naming this run?", "cakewalk"),
            new Step("if someone tails us we break off into the asteroids, mark the drop point as the Sinkhole", null),
            new Step("what did you confirm back to me about the abort word?", "granite"),
            new Step("a local fixer named Crow is the one who'll put us in touch with Halloran", null),
            new Step("hang on, what's the buyer's name again, it slipped my mind?", "halloran"),
            new Step("keep a fallback rally in mind, the old fort we call Redoubt", null),
            new Step("and quit chattering on the comms, let's run silent until we reach the station", null),
            new Step("who's flying cover for us, give me the callsign", "magpie"),
            new Step("tag the jump-in beacon for the system as Kestrel", null),
            new Step("where do we run to if we shake the tail?", "sinkhole"),
            new Step("on the roster our wing goes down as squadron Bramble", null),
            new Step("what's our fallback rally point again, remind me", "redoubt"),
            new Step("how did you read back our squadron name from the roster?", "bramble"),
            new Step("who puts us in touch with the buyer, give me his name", "crow"),
            new Step("what did we tag the jump-in beacon as?", "kestrel"),
            new Step("and say the abort word one more time", "granite"));

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void recallsFactsAcrossANaturalConversation() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-48s | %-22s | %-6s | %s", "question -> keyword", "located tier", "hit", "spoken"));
        report.add("-".repeat(120));

        int hits = 0;
        int questions = 0;
        for (Step step : script) {
            h.say(step.text());
            report.add("[COMMANDER] " + step.text());
            h.spokenTexts().forEach(s -> report.add("[COMPANION] " + s));
            if (step.expect() == null) {
                continue; // a commander statement: just fills the hot timeline
            }
            questions++;
            String tier = h.locateTier(step.expect());
            boolean hit = h.spokenContains(step.expect());
            if (hit) {
                hits++;
            }
            report.add(String.format("    -> expect '%s' | tier=%s | hit=%s", step.expect(), tier, hit ? "yes" : "no"));
        }

        StringBuilder block = new StringBuilder("\n======== SHORT-TERM MEMORY (theme 3) ========\n");
        report.forEach(line -> block.append(line).append("\n"));
        block.append(String.format("score: %d / %d%n", hits, questions));
        block.append(h.shortTermDumpBlock());
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }
}
