package elite.intel.ai.mouth;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kokoro is the only engine that can fail to voice a language outright, and Cyrillic is the only language it
 * fails on. The rule is pinned here because three places lean on it - the stored setting the session hands
 * back, the mouth the factory builds, and the segment the settings panel greys out - and all three would
 * degrade into silence rather than an error if it drifted.
 */
class TtsProviderLanguageTest {

    @Test
    void onlyKokoroIsBeatenByCyrillic() {
        for (Language language : Language.values()) {
            assertTrue(TtsProvider.EDGE.canVoice(language), "Edge carries every language: " + language);
            assertTrue(TtsProvider.GOOGLE.canVoice(language), "Google carries every language: " + language);
            assertTrue(TtsProvider.SUPERTONIC.canVoice(language), "Supertonic carries every language: " + language);
            assertEquals(!language.isCyrillicScript(), TtsProvider.KOKORO.canVoice(language),
                    "Kokoro against " + language);
        }
    }

    @Test
    void anEngineThatCannotSpeakTheLanguageIsReplacedByTheOtherLocalOne() {
        for (Language language : Language.values()) {
            for (TtsProvider selected : TtsProvider.values()) {
                TtsProvider resolved = TtsProvider.forLanguage(selected, language);
                assertTrue(resolved.canVoice(language), selected + " under " + language + " resolved to " + resolved);
                if (selected.canVoice(language)) {
                    assertEquals(selected, resolved, "a usable selection is never second-guessed: " + selected);
                } else {
                    assertEquals(TtsProvider.SUPERTONIC, resolved,
                            "the stand-in must be local and keyless, like the choice it replaces: " + language);
                }
            }
        }
    }

    /**
     * A session speaks two languages - the commander's, and the game client's on the radio - and Kokoro has
     * to read both to be offered at all. Whichever side is Cyrillic, Supertonic is the local engine; the cloud
     * engines are never second-guessed.
     */
    @Test
    void kokoroMustVoiceBothTheCommanderAndTheGameClient() {
        for (Language commander : Language.values()) {
            for (Language client : Language.values()) {
                boolean bothLatin = !commander.isCyrillicScript() && !client.isCyrillicScript();
                assertEquals(bothLatin, TtsProvider.KOKORO.canVoiceSession(commander, client),
                        "Kokoro for a " + commander + " commander on a " + client + " client");
                assertEquals(bothLatin ? TtsProvider.KOKORO : TtsProvider.SUPERTONIC,
                        TtsProvider.forSession(TtsProvider.KOKORO, commander, client));
                for (TtsProvider other : TtsProvider.values()) {
                    if (other == TtsProvider.KOKORO) continue;
                    assertEquals(other, TtsProvider.forSession(other, commander, client),
                            other + " reads every script and is kept for a " + commander + " commander on a " + client + " client");
                }
            }
        }
    }

    /**
     * Google speaks Cyrillic, so a Russian commander who pays for it keeps it: this rule withdraws Kokoro, not
     * the cloud.
     */
    @Test
    void googleSurvivesACyrillicLanguage() {
        assertEquals(TtsProvider.GOOGLE, TtsProvider.forLanguage(TtsProvider.GOOGLE, Language.RU));
        assertEquals(TtsProvider.GOOGLE, TtsProvider.forLanguage(TtsProvider.GOOGLE, Language.UK));
        assertFalse(TtsProvider.KOKORO.canVoice(Language.RU));
    }

    /**
     * Both local engines are local, and only they are: the settings panel's LOCAL segment and the radio rule
     * ({@code RadioVoicing}) both read this.
     */
    @Test
    void exactlyTheTwoOnnxEnginesAreLocal() {
        assertTrue(TtsProvider.KOKORO.isLocal());
        assertTrue(TtsProvider.SUPERTONIC.isLocal());
        assertFalse(TtsProvider.EDGE.isLocal());
        assertFalse(TtsProvider.GOOGLE.isLocal());
    }

    /**
     * A stored value this build does not know - or none at all - is Kokoro, the shipped default, so a commander
     * who rolls back from a build with more engines still has a voice.
     */
    @Test
    void unknownStoredValuesFallBackToKokoro() {
        assertEquals(TtsProvider.KOKORO, TtsProvider.fromStored(null));
        assertEquals(TtsProvider.KOKORO, TtsProvider.fromStored(""));
        assertEquals(TtsProvider.KOKORO, TtsProvider.fromStored("ELEVENLABS"));
        assertEquals(TtsProvider.SUPERTONIC, TtsProvider.fromStored("supertonic"));
    }
}
