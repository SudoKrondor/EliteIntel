package elite.intel.ai.brain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A personality clause lands in the same system prompt as the rules, and the model tends to follow whichever is
 * louder and more specific. These pin the phrasings that have contradicted those rules.
 */
class ShipPersonalityTest {

    /**
     * WHY: {@link VegaIdentity} opens the prompt with "You are Vega ... never pretend otherwise". A clause saying
     * "You are Seven of Nine" names a second speaker in the next breath, so a personality voices a character instead.
     */
    @ParameterizedTest
    @EnumSource(ShipPersonality.class)
    void aPersonalityNeverNamesASecondSpeaker(ShipPersonality personality) {
        assertFalse(clause(personality).contains("you are "),
                () -> personality + " must say \"You speak in the voice of\", not \"You are\"");
    }

    /**
     * WHY: the prompts say every reply is spoken aloud and never mentions the machinery behind it. Breaking the
     * fourth wall invites exactly that, and a text box or chat interface describes an app the commander is not using.
     */
    @ParameterizedTest
    @EnumSource(ShipPersonality.class)
    void aPersonalityNeverPointsAtTheMachineryOrATextInterface(ShipPersonality personality) {
        for (String phrase : List.of("fourth wall", "fourth-wall", "text box", "chat interface", "on read")) {
            assertFalse(clause(personality).contains(phrase),
                    () -> personality + " must not mention \"" + phrase + "\"");
        }
    }

    private static String clause(ShipPersonality personality) {
        return personality.getPersonalityClause().toLowerCase(Locale.ROOT);
    }
}
