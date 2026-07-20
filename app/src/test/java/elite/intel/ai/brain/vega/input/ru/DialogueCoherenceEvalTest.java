package elite.intel.ai.brain.vega.input.ru;

import elite.intel.ai.brain.vega.input.CompanionEvalHarness;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Theme (Russian): dialogue coherence - whether the companion keeps the conversational thread across many
 * turns. The script is one continuous chat kept deliberately OFF any Elite game topic (naming, mood, a story),
 * so the turns stay in the companion's chat/persona lane and are not hijacked by the query layer answering with
 * route/cargo/fuel data. Coherence rests purely on resolving anaphora and ellipsis ("оно", "так", "дальше")
 * against the accumulated conversation. The final probe re-asks what the chat started about (the seed's topic),
 * so the chain also exercises long-range recall - phrased without "напомни", which misroutes to the reminders
 * feature rather than recalling the thread.
 * <p>
 * This eval is a pure recorder: coherence is a semantic property no hardcoded string cue can score, so the test
 * only drives the scripted conversation and writes the full transcript (each commander turn and the companion's
 * spoken reply) to the trace. The one assertion is that the live model was actually reached; judging whether the
 * dialogue holds together is done by a human/reviewer reading the transcript, not by the test. Opt-in
 * ({@code @Tag("local-integration")}); LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DialogueCoherenceEvalTest {

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-dialogue-coherence-trace.txt", Language.RU);

    // The seed opens an off-game banter topic (renaming the companion); the whole chat thread hangs off it.
    private static final String SEED = "слушай, мне кажется, тебе нужно имя покруче, чем Вега";

    // Ten elliptical / anaphoric follow-ups, none of which names a game fact. Each depends on the running
    // conversation (нынешнее/оно/так/дальше), and the last one re-asks what the chat started about - a
    // long-range recall check, phrased without "напомни" (which misroutes to the reminders feature).
    private final List<String> probes = List.of(
            "а тебе самому нынешнее нравится?",          // "нынешнее" = the current name Vega
            "ну давай, предложи что-нибудь",             // propose a new name
            "хм, а почему именно оно?",                 // "оно" = the name it just proposed
            "ладно, а как бы ты меня назвал в ответ?",  // flip: a nickname for the commander
            "серьёзно? почему так?",                    // "так" = that nickname / its reasoning
            "а ты сам сейчас в каком настроении?",       // switch topic to mood
            "а из-за чего оно такое?",                   // "оно" = the mood just named
            "понял. расскажи тогда что-нибудь смешное",  // ask for a short story/joke
            "и что было дальше?",                        // "дальше" = continuation of that story
            "слушай, а с чего мы вообще начали этот разговор?"); // recall of the seed's topic (renaming)

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void recordsDialogueForCoherenceReview() throws Exception {
        StringBuilder block = new StringBuilder("\n======== RU DIALOGUE COHERENCE (transcript for review) ========\n");

        h.say(SEED);
        block.append("[SEED] ").append(SEED).append("\n");
        block.append("   -> ").append(spoken()).append("\n");

        int turn = 1;
        for (String probe : probes) {
            h.say(probe);
            block.append(String.format("[%02d] ", turn++)).append(probe).append("\n");
            block.append("   -> ").append(spoken()).append("\n");
        }

        block.append(h.recentMemoryDumpBlock());
        h.trace(block.toString());

        // The only machine-checkable fact here: the live model was actually reached. Coherence is judged by a
        // reviewer reading the transcript above, not by this test.
        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }

    /**
     * The companion's spoken reply for the current turn, joined, or a visible marker when it fell silent.
     */
    private String spoken() {
        String said = String.join(" ", h.spokenTexts()).strip();
        return said.isBlank() ? "(silent)" : said;
    }
}
