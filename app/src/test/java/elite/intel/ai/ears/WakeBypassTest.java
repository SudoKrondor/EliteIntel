package elite.intel.ai.ears;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.ai.brain.i18n.de.GermanAiActionAliases;
import elite.intel.ai.brain.i18n.en.EnglishAiActionAliases;
import elite.intel.ai.brain.i18n.ru.RussianAiActionAliases;
import elite.intel.ai.brain.i18n.uk.UkrainianAiActionAliases;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Sleep/Wake gate is the whole feature: while it is closed nothing the commander says should reach the
 * companion except the words that reopen it. These cases run against the real {@link WakeBypass} the STT
 * pipeline calls — an earlier version of this suite re-implemented the matching in its own helpers, which
 * would have gone on passing had the pipeline's copy drifted.
 * <p>
 * Four languages rather than nine, and deliberately so: they are the ones whose wake phrases overlap
 * (Russian and Ukrainian share "слушай"/"слухай" shapes, German has the two-word "hör mir zu"), which is
 * where the longest-prefix and prefix-at-the-start rules actually bite.
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

    // --- Russian -------------------------------------------------------------

    @ParameterizedTest(name = "[RU] \"{0}\" passes the gate")
    @CsvSource({"проснись", "слушай", "слушай меня", "активируйся", "слушай открой карту"})
    void russianPassesGate(String transcript) {
        assertTrue(bypass(new RussianAiActionAliases()).passesGate(transcript));
    }

    @ParameterizedTest(name = "[RU] \"{0}\" is blocked")
    @CsvSource({"открой карту галактики", "прыжок в гиперпространство", "не слушай открой карту",
            "пожалуйста слушай открой карту", "открой карту слушай"})
    void russianBlockedWhileSleeping(String transcript) {
        assertNull(bypass(new RussianAiActionAliases()).admit(transcript));
    }

    @Test
    void russianLongerPrefixWinsOverShorter() {
        WakeBypass ru = bypass(new RussianAiActionAliases());
        assertEquals("открой карту", ru.admit("слушай открой карту"));
        // "слушай меня открой карту" must not be reduced by the shorter "слушай" and left with a stray "меня"
        assertEquals("открой карту", ru.admit("слушай меня открой карту"));
    }

    @Test
    void russianPureWakePhraseReachesTheModelWhole() {
        WakeBypass ru = bypass(new RussianAiActionAliases());
        assertNull(ru.strippedOrder("проснись"));
        assertNull(ru.strippedOrder("активируйся"));
        assertNull(ru.strippedOrder("слушай"));
    }

    // --- Ukrainian -----------------------------------------------------------

    @ParameterizedTest(name = "[UK] \"{0}\" passes the gate")
    @CsvSource({"прокинься", "слухай", "слухай мене", "активуйся", "слухай відкрий карту"})
    void ukrainianPassesGate(String transcript) {
        assertTrue(bypass(new UkrainianAiActionAliases()).passesGate(transcript));
    }

    @ParameterizedTest(name = "[UK] \"{0}\" is blocked")
    @CsvSource({"відкрий карту галактики", "стрибок у гіперпростір", "не слухай відкрий карту",
            "будь ласка слухай відкрий карту", "відкрий карту слухай"})
    void ukrainianBlockedWhileSleeping(String transcript) {
        assertNull(bypass(new UkrainianAiActionAliases()).admit(transcript));
    }

    @Test
    void ukrainianLongerPrefixWinsOverShorter() {
        WakeBypass uk = bypass(new UkrainianAiActionAliases());
        assertEquals("відкрий карту", uk.admit("слухай відкрий карту"));
        assertEquals("відкрий карту", uk.admit("слухай мене відкрий карту"));
    }

    @Test
    void ukrainianPureWakePhraseReachesTheModelWhole() {
        WakeBypass uk = bypass(new UkrainianAiActionAliases());
        assertNull(uk.strippedOrder("прокинься"));
        assertNull(uk.strippedOrder("активуйся"));
        assertNull(uk.strippedOrder("слухай"));
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
