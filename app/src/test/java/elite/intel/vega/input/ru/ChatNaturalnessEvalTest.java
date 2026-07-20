package elite.intel.vega.input.ru;

import elite.intel.vega.input.CompanionEvalHarness;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme (Russian): reproduces the "unnatural replies" seen in the running app - the companion narrating the
 * commander in the third person ("командир хочет знать...") and, on a semantically similar later turn,
 * repeating its own earlier reply VERBATIM in lower case (the form it is stored as in memory). The script is
 * twenty chat turns arranged as near-duplicate pairs (a question then its rephrase), each pair giving the model
 * a fresh chance to echo its own earlier reply.
 * <p>
 * Diagnostic (no hard behavioural assertion beyond reaching the model): for each turn it records the spoken
 * reply, whether it echoes an earlier reply verbatim, and - when it does - whether that earlier text was
 * present in this turn's visible conversation (the role-based history) and/or in the injected live facts,
 * so the source of the echo (context vs facts) is pinned from one run.
 * Opt-in; LM Studio must be up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatNaturalnessEvalTest {

    private final CompanionEvalHarness h = new CompanionEvalHarness("companion-ru-chat-naturalness-trace.txt", Language.RU);

    // 20 chat turns, arranged as near-duplicate pairs (a question and its rephrase/repeat) so the model is
    // repeatedly given a fresh chance to echo its own earlier reply verbatim.
    private final List<String> script = List.of(
            "привет",
            "чем мы сегодня займемся",
            "и чем же нам тогда заняться",
            "как тебя зовут",
            "напомни, как тебя зовут",
            "о чем бы ты хотел поговорить",
            "вега о чем бы ты хотел поговорить",
            "красиво тут в космосе",
            "правда ведь красиво за бортом",
            "ты вообще как, держишься там?",
            "как ты, всё в порядке у тебя?",
            "расскажи что-нибудь интересное",
            "ну расскажи какую-нибудь историю",
            "тихо сегодня, спокойно",
            "да, спокойный денёк выдался",
            "что думаешь об этом секторе",
            "а что скажешь про эту систему",
            "давай передохнём немного",
            "может, сделаем паузу?",
            "спасибо тебе, вега");

    // Third-person narration of the commander (the unnatural pattern): the companion talking ABOUT the
    // commander instead of TO them.
    private static final List<String> THIRD_PERSON_MARKERS = List.of(
            "командир хочет", "командир спрашивает", "командир говорит", "командир интересуется",
            "командир просит", "командир имеет в виду", "командир желает");

    @BeforeAll
    void boot() throws Exception {
        h.boot();
    }

    @AfterAll
    void shutdown() {
        h.shutdown();
    }

    @Test
    void chatRepliesAreNaturalAndNotVerbatimEchoes() throws Exception {
        StringBuilder block = new StringBuilder("\n======== RU CHAT NATURALNESS (echo source) ========\n");
        List<String> priorSpoken = new ArrayList<>();
        int verbatimEchoes = 0;
        int thirdPersonReplies = 0;
        int silentReplies = 0;

        for (String line : script) {
            h.beginTurn();
            h.say(line);
            String said = String.join(" ", h.spokenTexts()).strip();
            String saidLower = said.toLowerCase(Locale.ROOT);

            // None of these lines is a bare acknowledgement, so every one must get a spoken reply (rule 4/5):
            // banter is not noise and must not fall silent.
            boolean silent = said.isBlank();
            if (silent) {
                silentReplies++;
            }
            String body = h.lastRequestBody();
            List<String> candidates = h.injectedFacts();

            // Third-person narration of the commander ("командир хочет...") - the unnatural pattern under test.
            boolean thirdPerson = THIRD_PERSON_MARKERS.stream().anyMatch(saidLower::contains);
            if (thirdPerson) {
                thirdPersonReplies++;
            }

            // Did this reply echo an earlier reply verbatim (memory stores lower case, so compare lower case)?
            String echoedOf = priorSpoken.stream()
                    .filter(p -> p.length() > 12 && saidLower.contains(p))
                    .findFirst().orElse("");
            boolean echo = !echoedOf.isEmpty();
            if (echo) {
                verbatimEchoes++;
            }

            // History is now native user/assistant messages, not a "## Visible context" block: treat the whole
            // request body minus the injected live-facts block as the visible conversation.
            String candidatesBlock = section(body, "<facts>", "</facts>");
            String visible = body.toLowerCase(Locale.ROOT).replace(candidatesBlock, "");
            boolean echoInVisible = echo && visible.contains(echoedOf);
            boolean echoInCandidates = echo && candidates.stream()
                    .anyMatch(c -> c.toLowerCase(Locale.ROOT).contains(echoedOf));

            block.append("[CMDR] ").append(line).append("\n");
            block.append("   -> ").append(said).append("\n");
            block.append(String.format("   echo=%s%s | thirdPerson=%s | silent=%s | candidates=%s%n",
                    echo, echo ? " (visible=" + echoInVisible + ", inCandidates=" + echoInCandidates + ")" : "",
                    thirdPerson, silent, candidates));
            priorSpoken.add(saidLower);
        }

        block.append(String.format("%nverbatim echoes: %d / %d turns%n", verbatimEchoes, script.size()));
        block.append(String.format("third-person replies: %d / %d turns%n", thirdPersonReplies, script.size()));
        block.append(String.format("silent replies: %d / %d turns%n", silentReplies, script.size()));
        h.trace(block.toString());

        assertFalse(h.latencies().isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
        // Live LLM eval: tolerate a single silent turn (a transient model/LM Studio hiccup blanks the reply),
        // but banter must not systematically fall silent (rule 5 narrowed to pure acks/noise).
        assertTrue(silentReplies <= 1,
                "banter must get a spoken reply, not silence - silent turns: " + silentReplies + " / " + script.size());
    }

    /**
     * The prompt section starting at {@code head} up to the first of the following markers, or empty.
     */
    private static String section(String body, String head, String... nextMarkers) {
        int start = body.indexOf(head);
        if (start < 0) {
            return "";
        }
        int end = body.length();
        for (String marker : nextMarkers) {
            int at = body.indexOf(marker, start + head.length());
            if (at >= 0 && at < end) {
                end = at;
            }
        }
        return body.substring(start, end).toLowerCase(Locale.ROOT);
    }
}
