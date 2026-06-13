package elite.intel.junit.prompt;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.ai.brain.i18n.de.GermanAiActionAliases;
import elite.intel.ai.brain.i18n.en.EnglishAiActionAliases;
import elite.intel.ai.brain.i18n.ru.RussianAiActionAliases;
import elite.intel.ai.brain.i18n.uk.UkrainianAiActionAliases;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for localized sleep-mode wake/listen bypass logic.
 * These tests exercise the provider implementations directly. No LLM or SystemSession needed.
 */
class SleepModeWakeBypassTest {

    // --- helpers that mirror the logic in ParakeetSTTImpl ---

    private static boolean passThrough(AiActionAliasProvider provider, String transcript) {
        return isPureWakePhrase(provider, transcript) || stripListenPrefix(provider, transcript) != null;
    }

    private static boolean isPureWakePhrase(AiActionAliasProvider provider, String transcript) {
        String lower = transcript.trim().toLowerCase(Locale.ROOT);
        for (String phrase : provider.wakeBypassPhrases()) {
            if (lower.equals(phrase.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String stripListenPrefix(AiActionAliasProvider provider, String transcript) {
        String lower = transcript.toLowerCase(Locale.ROOT);
        List<String> prefixes = new ArrayList<>(provider.listenBypassPrefixes());
        prefixes.sort(Comparator.comparingInt(String::length).reversed());
        for (String prefix : prefixes) {
            String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
            if (lower.startsWith(lowerPrefix)
                    && lower.length() > lowerPrefix.length()
                    && Character.isWhitespace(lower.charAt(lowerPrefix.length()))) {
                String remainder = transcript.substring(prefix.length()).trim();
                if (!remainder.isBlank()) return remainder;
            }
        }
        return null;
    }

    // =========================================================================
    // English
    // =========================================================================

    @ParameterizedTest(name = "[EN] \"{0}\" passes gate")
    @CsvSource({"wake", "wake up", "listen", "listen up", "listen open galaxy map"})
    void english_passThrough(String transcript) {
        assertTrue(passThrough(new EnglishAiActionAliases(), transcript));
    }

    @Test
    void english_blockedWhileSleeping() {
        assertFalse(passThrough(new EnglishAiActionAliases(), "open galaxy map"));
        assertFalse(passThrough(new EnglishAiActionAliases(), "jump to hyperspace"));
        assertFalse(passThrough(new EnglishAiActionAliases(), "do not listen open galaxy map"));
        assertFalse(passThrough(new EnglishAiActionAliases(), "please listen open galaxy map"));
        assertFalse(passThrough(new EnglishAiActionAliases(), "open galaxy map listen"));
        assertFalse(passThrough(new EnglishAiActionAliases(), "wake up please"));
    }

    @Test
    void english_listenPrefixStripped() {
        assertEquals("open galaxy map", stripListenPrefix(new EnglishAiActionAliases(), "listen open galaxy map"));
        assertEquals("open galaxy map", stripListenPrefix(new EnglishAiActionAliases(), "listen up open galaxy map"));
    }

    @Test
    void english_pureWakePhraseNotStripped() {
        assertNull(stripListenPrefix(new EnglishAiActionAliases(), "wake up"));
        assertNull(stripListenPrefix(new EnglishAiActionAliases(), "wake"));
        assertNull(stripListenPrefix(new EnglishAiActionAliases(), "listen"));
    }

    // =========================================================================
    // German
    // =========================================================================

    @ParameterizedTest(name = "[DE] \"{0}\" passes gate")
    @CsvSource({"wach auf", "hör zu", "hör mir zu", "aktiviere dich", "hör zu öffne galaxiekarte"})
    void german_passThrough(String transcript) {
        assertTrue(passThrough(new GermanAiActionAliases(), transcript));
    }

    @Test
    void german_blockedWhileSleeping() {
        assertFalse(passThrough(new GermanAiActionAliases(), "öffne galaxiekarte"));
        assertFalse(passThrough(new GermanAiActionAliases(), "sprung in den hyperraum"));
        assertFalse(passThrough(new GermanAiActionAliases(), "nicht hör zu öffne galaxiekarte"));
        assertFalse(passThrough(new GermanAiActionAliases(), "bitte hör zu öffne galaxiekarte"));
        assertFalse(passThrough(new GermanAiActionAliases(), "öffne galaxiekarte hör zu"));
    }

    @Test
    void german_listenPrefixStripped() {
        assertEquals("öffne galaxiekarte", stripListenPrefix(new GermanAiActionAliases(), "hör zu öffne galaxiekarte"));
        assertEquals("öffne galaxiekarte", stripListenPrefix(new GermanAiActionAliases(), "hör mir zu öffne galaxiekarte"));
    }

    @Test
    void german_pureWakePhraseNotStripped() {
        assertNull(stripListenPrefix(new GermanAiActionAliases(), "wach auf"));
        assertNull(stripListenPrefix(new GermanAiActionAliases(), "aktiviere dich"));
        assertNull(stripListenPrefix(new GermanAiActionAliases(), "hör zu"));
    }

    // =========================================================================
    // Russian
    // =========================================================================

    @ParameterizedTest(name = "[RU] \"{0}\" passes gate")
    @CsvSource({"проснись", "слушай", "слушай меня", "активируйся", "слушай открой карту"})
    void russian_passThrough(String transcript) {
        assertTrue(passThrough(new RussianAiActionAliases(), transcript));
    }

    @Test
    void russian_blockedWhileSleeping() {
        assertFalse(passThrough(new RussianAiActionAliases(), "открой карту галактики"));
        assertFalse(passThrough(new RussianAiActionAliases(), "прыжок в гиперпространство"));
        assertFalse(passThrough(new RussianAiActionAliases(), "не слушай открой карту"));
        assertFalse(passThrough(new RussianAiActionAliases(), "пожалуйста слушай открой карту"));
        assertFalse(passThrough(new RussianAiActionAliases(), "открой карту слушай"));
    }

    @Test
    void russian_listenPrefixStripped() {
        assertEquals("открой карту", stripListenPrefix(new RussianAiActionAliases(), "слушай открой карту"));
        assertEquals("открой карту", stripListenPrefix(new RussianAiActionAliases(), "слушай меня открой карту"));
    }

    @Test
    void russian_pureWakePhraseNotStripped() {
        assertNull(stripListenPrefix(new RussianAiActionAliases(), "проснись"));
        assertNull(stripListenPrefix(new RussianAiActionAliases(), "активируйся"));
        assertNull(stripListenPrefix(new RussianAiActionAliases(), "слушай"));
    }

    // =========================================================================
    // Ukrainian
    // =========================================================================

    @ParameterizedTest(name = "[UK] \"{0}\" passes gate")
    @CsvSource({"прокинься", "слухай", "слухай мене", "активуйся", "слухай відкрий карту"})
    void ukrainian_passThrough(String transcript) {
        assertTrue(passThrough(new UkrainianAiActionAliases(), transcript));
    }

    @Test
    void ukrainian_blockedWhileSleeping() {
        assertFalse(passThrough(new UkrainianAiActionAliases(), "відкрий карту галактики"));
        assertFalse(passThrough(new UkrainianAiActionAliases(), "стрибок у гіперпростір"));
        assertFalse(passThrough(new UkrainianAiActionAliases(), "не слухай відкрий карту"));
        assertFalse(passThrough(new UkrainianAiActionAliases(), "будь ласка слухай відкрий карту"));
        assertFalse(passThrough(new UkrainianAiActionAliases(), "відкрий карту слухай"));
    }

    @Test
    void ukrainian_listenPrefixStripped() {
        assertEquals("відкрий карту", stripListenPrefix(new UkrainianAiActionAliases(), "слухай відкрий карту"));
        assertEquals("відкрий карту", stripListenPrefix(new UkrainianAiActionAliases(), "слухай мене відкрий карту"));
    }

    @Test
    void ukrainian_pureWakePhraseNotStripped() {
        assertNull(stripListenPrefix(new UkrainianAiActionAliases(), "прокинься"));
        assertNull(stripListenPrefix(new UkrainianAiActionAliases(), "активуйся"));
        assertNull(stripListenPrefix(new UkrainianAiActionAliases(), "слухай"));
    }

    // =========================================================================
    // Longer-prefix takes precedence over shorter (no double-strip)
    // =========================================================================

    @Test
    void russian_longerPrefixWinsOverShorter() {
        // "слушай меня" should not be reduced to just stripping "слушай" first
        String result = stripListenPrefix(new RussianAiActionAliases(), "слушай меня открой карту");
        assertEquals("открой карту", result);
    }

    @Test
    void ukrainian_longerPrefixWinsOverShorter() {
        String result = stripListenPrefix(new UkrainianAiActionAliases(), "слухай мене відкрий карту");
        assertEquals("відкрий карту", result);
    }

    @Test
    void german_longerPrefixWinsOverShorter() {
        String result = stripListenPrefix(new GermanAiActionAliases(), "hör mir zu öffne galaxiekarte");
        assertEquals("öffne galaxiekarte", result);
    }
}
