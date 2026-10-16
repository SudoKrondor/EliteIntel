package elite.intel.ai.ears;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.ai.brain.i18n.de.GermanAiActionAliases;
import elite.intel.ai.brain.i18n.en.EnglishAiActionAliases;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Sleep/Wake gate is the whole feature: while it is closed nothing the commander says should reach the
 * VEGA except the words that reopen it. These cases run against the real {@link WakeBypass} the STT
 * pipeline calls — an earlier version of this suite re-implemented the matching in its own helpers, which
 * would have gone on passing had the pipeline's copy drifted.
 * <p>
 * English and German rather than every language, and deliberately so: German has the two-word "hör mir zu"
 * beside "hör zu", which is where the longest-prefix and prefix-at-the-start rules actually bite.
 */
class WakeBypassTest {

    private static WakeBypass bypass(AiActionAliasProvider provider) {
        return new WakeBypass(provider);
    }

    // --- English -------------------------------------------------------------

    @ParameterizedTest(name = "[EN] \"{0}\" passes the gate")
    @CsvSource({"wake", "wake up", "listen", "listen up", "listen open galaxy map"})
    void englishPassesGate(String transcript) {
        assertTrue(bypass(new EnglishAiActionAliases()).passesGate(transcript));
    }

    @ParameterizedTest(name = "[EN] \"{0}\" is blocked")
    @CsvSource({"open galaxy map", "jump to hyperspace", "do not listen open galaxy map",
            "please listen open galaxy map", "open galaxy map listen", "wake up please"})
    void englishBlockedWhileSleeping(String transcript) {
        assertNull(bypass(new EnglishAiActionAliases()).admit(transcript));
    }

    @Test
    void englishListenPrefixIsStrippedFromTheOrder() {
        WakeBypass en = bypass(new EnglishAiActionAliases());
        assertEquals("open galaxy map", en.admit("listen open galaxy map"));
        assertEquals("open galaxy map", en.admit("listen up open galaxy map"));
    }

    @Test
    void englishPureWakePhraseReachesTheModelWhole() {
        WakeBypass en = bypass(new EnglishAiActionAliases());
        assertEquals("wake up", en.admit("wake up"));
        assertEquals("wake", en.admit("wake"));
        assertEquals("listen", en.admit("listen"));
        assertNull(en.strippedOrder("wake up"));
    }

    // --- German --------------------------------------------------------------

    @ParameterizedTest(name = "[DE] \"{0}\" passes the gate")
    @CsvSource({"wach auf", "hör zu", "hör mir zu", "aktiviere dich", "hör zu öffne galaxiekarte"})
    void germanPassesGate(String transcript) {
        assertTrue(bypass(new GermanAiActionAliases()).passesGate(transcript));
    }

    @ParameterizedTest(name = "[DE] \"{0}\" is blocked")
    @CsvSource({"öffne galaxiekarte", "sprung in den hyperraum", "nicht hör zu öffne galaxiekarte",
            "bitte hör zu öffne galaxiekarte", "öffne galaxiekarte hör zu"})
    void germanBlockedWhileSleeping(String transcript) {
        assertNull(bypass(new GermanAiActionAliases()).admit(transcript));
    }

    @Test
    void germanLongerPrefixWinsOverShorter() {
        WakeBypass de = bypass(new GermanAiActionAliases());
        assertEquals("öffne galaxiekarte", de.admit("hör zu öffne galaxiekarte"));
        assertEquals("öffne galaxiekarte", de.admit("hör mir zu öffne galaxiekarte"));
    }

    @Test
    void germanPureWakePhraseReachesTheModelWhole() {
        WakeBypass de = bypass(new GermanAiActionAliases());
        assertNull(de.strippedOrder("wach auf"));
        assertNull(de.strippedOrder("aktiviere dich"));
        assertNull(de.strippedOrder("hör zu"));
    }

    // --- Blank input ---------------------------------------------------------

    @Test
    void blankTranscriptIsNeverAdmitted() {
        WakeBypass en = bypass(new EnglishAiActionAliases());
        assertNull(en.admit(null));
        assertNull(en.admit(""));
        assertNull(en.admit("   "));
    }
}
