package elite.intel.vega.input.ru;

import elite.intel.ai.brain.ShipPersonality;
import elite.intel.vega.input.CompanionEvalHarness;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme (Russian, Mistral cloud): does switching the AI personality actually change the spoken tone? Each
 * personality runs on its OWN fresh boot (a fresh {@code SessionMemoryGateway}, pristine timeline), so no
 * earlier block's reply can echo into a later one - the memory echo previously washed the personality out,
 * especially ROGUE run last. For each personality it (a) asserts the request body Mistral received carries that
 * personality's clause (proves the switch reached the wire), and (b) traces the spoken replies so the tone can
 * be read per personality against a clean context.
 * <p>
 * Opt-in; the Mistral provider (API key) must be configured.
 */
@Tag("local-integration")
class PersonalitySwitchEvalTest {

    private static final List<ShipPersonality> PERSONALITIES =
            List.of(ShipPersonality.PROFESSIONAL, ShipPersonality.FRIENDLY, ShipPersonality.ROGUE);

    private static final List<String> SCRIPT = List.of(
            "привет",
            "как настроение",
            "что будем делать сегодня");

    /**
     * A distinctive fragment of each personality's clause that survives JSON escaping in the request body.
     */
    private static String fragment(ShipPersonality personality) {
        return switch (personality) {
            case PROFESSIONAL -> "military professional";
            case CASUAL -> "like a colleague";
            case FRIENDLY -> "close friend";
            case UNHINGED -> "chaotic energy";
            case ROGUE -> "comedic mayhem";
        };
    }

    @Test
    void switchingPersonalityReachesTheWireAndVariesTheTone() throws Exception {
        int spokenReplies = 0;
        boolean modelReached = false;

        for (ShipPersonality personality : PERSONALITIES) {
            // Fresh harness per personality = fresh memory, so no earlier reply can echo into this block.
            CompanionEvalHarness h = new CompanionEvalHarness(
                    "companion-ru-personality-" + personality.name().toLowerCase(Locale.ROOT) + "-trace.txt",
                    Language.RU, CompanionEvalHarness.Backend.MISTRAL);
            h.boot();
            try {
                h.setPersonality(personality);
                StringBuilder block = new StringBuilder(
                        "\n---- personality = " + personality.name() + " (fresh memory) ----\n");
                for (String line : SCRIPT) {
                    h.beginTurn();
                    h.say(line);
                    String said = String.join(" ", h.spokenTexts()).strip();
                    if (!said.isBlank()) {
                        spokenReplies++;
                    }
                    block.append("[CMDR] ").append(line).append("\n   -> ").append(said).append("\n");
                }

                // The request Mistral got must carry this personality's clause (switch reached the wire).
                String body = h.lastRequestBody();
                String frag = fragment(personality);
                block.append(String.format("   [wire carries \"%s\": %s]%n", frag, body.contains(frag)));
                h.trace(block.toString());
                assertTrue(body.contains(frag),
                        personality + " clause (\"" + frag + "\") must be in the request body sent for this personality");
                if (!h.latencies().isEmpty()) {
                    modelReached = true;
                }
            } finally {
                h.shutdown();
            }
        }

        assertTrue(modelReached, "the Mistral model was never reached - check AI Services config / API key");
        assertTrue(spokenReplies > 0, "no spoken replies were produced across any personality");
    }
}
