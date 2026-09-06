package elite.intel.ui.screen;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.edge.EdgeVoices;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Fleet Voice Configuration must show only the logical voice identity plus its friendly description
 * (e.g. "Mary - American female"), never a TTS provider's native voice id. Edge Read Aloud went through two
 * regressions on the way here:
 * <ol>
 *     <li>The fleet grid's Edge combo options and normalized selection were built from
 *     {@link EdgeVoices#defaultShortName()} instead of the logical enum name, so
 *     {@link CommanderTabPanel#voiceLabel} received an already-resolved ShortName and re-appended another one
 *     on top of it (e.g. "Mary - en-US-EmmaMultilingualNeural").</li>
 *     <li>After that was fixed, Edge showed only the bare logical name ("Mary") with no description, unlike
 *     Google/Kokoro which both show "DisplayName - accent"/"description". {@link EdgeVoices} and
 *     {@link GoogleVoices} enum names match one-for-one by design (same accents too), so
 *     {@link CommanderTabPanel#edgeVoiceLabel} now reuses {@link GoogleVoices}'s descriptor instead of
 *     duplicating "American female" / "British female" literals in {@link EdgeVoices}.</li>
 * </ol>
 */
class CommanderTabPanelVoiceLabelTest {

    private static void runAsEdge(Runnable body) {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        Language previousLanguage = session.getLanguage();
        try {
            session.setTtsProvider(TtsProvider.EDGE);
            session.setLanguage(Language.EN); // the desired display text ("American female") is English-only
            body.run();
        } finally {
            session.setTtsProvider(previousProvider);
            session.setLanguage(previousLanguage);
        }
    }

    @Test
    void edgeLabelIsTheFriendlyLogicalDescriptionForTheStoredEnumName() {
        runAsEdge(() -> {
            assertEquals("Mary - American female", CommanderTabPanel.edgeVoiceLabel(EdgeVoices.MARY.name()));
            assertEquals("Anna - British female", CommanderTabPanel.edgeVoiceLabel(EdgeVoices.ANNA.name()));
            assertEquals("Emma - American female", CommanderTabPanel.edgeVoiceLabel(EdgeVoices.EMMA.name()));
            assertEquals("Jennifer - American female", CommanderTabPanel.edgeVoiceLabel(EdgeVoices.JENNIFER.name()));
            assertEquals("Olivia - British female", CommanderTabPanel.edgeVoiceLabel(EdgeVoices.OLIVIA.name()));
            assertEquals("Rachel - American female", CommanderTabPanel.edgeVoiceLabel(EdgeVoices.RACHEL.name()));
        });
    }

    @Test
    void edgeLabelIsTheFriendlyDescriptionEvenForALegacyPersistedShortName() {
        runAsEdge(() -> {
            // A ship saved while the ShortName-leak bug was live (or any other provider-native value) must
            // still resolve to the same friendly logical label, not the ShortName.
            assertEquals("Mary - American female",
                    CommanderTabPanel.edgeVoiceLabel(EdgeVoices.MARY.defaultShortName()));
        });
    }

    @Test
    void edgeLabelNeverLeaksTheProviderNativeShortName() {
        runAsEdge(() -> {
            for (EdgeVoices voice : EdgeVoices.values()) {
                String label = CommanderTabPanel.edgeVoiceLabel(voice.name());
                assertFalse(label.contains(voice.defaultShortName()),
                        "label leaked the Edge ShortName: " + label);
                assertFalse(label.toLowerCase().contains("neural"),
                        "label leaked Edge protocol naming: " + label);
            }
        });
    }

    @Test
    void edgeAndGooglePresentTheSameFriendlyDescriptionForTheSameLogicalIdentity() {
        // EdgeVoices and GoogleVoices enum names (and accents) match one-for-one by design; the fleet grid
        // must read identically for both providers rather than inventing separate wording for Edge.
        runAsEdge(() -> {
            for (EdgeVoices voice : EdgeVoices.values()) {
                String expected = voice.displayName() + " - " + GoogleVoices.valueOf(voice.name()).getDescription();
                assertEquals(expected, CommanderTabPanel.edgeVoiceLabel(voice.name()),
                        "Edge label diverged from the established Google terminology for " + voice.name());
            }
        });
    }

    @Test
    void normalizeVoiceKeepsTheLogicalNameForEdgeNotTheShortName() {
        runAsEdge(() -> {
            assertEquals(EdgeVoices.JENNIFER.name(),
                    CommanderTabPanel.normalizeVoice(EdgeVoices.JENNIFER.name()));
            // A raw/legacy ShortName still normalizes to a logical name, keeping the stored value in the
            // same vocabulary as the dropdown's option list (enum names, see initData()).
            assertEquals(EdgeVoices.MARY.name(),
                    CommanderTabPanel.normalizeVoice(EdgeVoices.MARY.defaultShortName()));
            // A male voice is a valid selection now, so it must stay selected rather than snap to the default.
            assertEquals(EdgeVoices.JAKE.name(), CommanderTabPanel.normalizeVoice(EdgeVoices.JAKE.name()));
            // Only a name this engine cannot place falls back to the default.
            assertEquals(EdgeVoices.DEFAULT_VOICE.name(),
                    CommanderTabPanel.normalizeVoice(KokoroVoices.BELLA.name()));
        });
    }

    @Test
    void normalizedEdgeSelectionStillResolvesToTheCorrectEdgeProviderVoice() {
        runAsEdge(() -> {
            String normalized = CommanderTabPanel.normalizeVoice(EdgeVoices.JENNIFER.name());
            // This mirrors exactly how EdgeTTSImpl.enqueue() turns a fleet-grid voice selection into the
            // ShortName used for synthesis, proving the demo/play-preview path still reaches the right
            // Edge voice after the logical name round-trips through the fleet grid.
            assertEquals(EdgeVoices.JENNIFER.defaultShortName(),
                    EdgeVoices.shortNameOrDefault(normalized));
        });
    }

    @Test
    void googleNormalizationIsUnaffectedByTheEdgeFix() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        try {
            session.setTtsProvider(TtsProvider.GOOGLE);
            assertEquals(GoogleVoices.EMMA.name(),
                    CommanderTabPanel.normalizeVoice(GoogleVoices.EMMA.name()));
            assertEquals(GoogleVoices.JAKE.name(),
                    CommanderTabPanel.normalizeVoice(GoogleVoices.JAKE.name()));
        } finally {
            session.setTtsProvider(previousProvider);
        }
    }

    /**
     * A carrier can hold a voice that has since been removed from the curated Kokoro cast. The grid has to show
     * the default it will actually be heard in - not the stale name, which the non-editable combo would reject
     * and quietly replace with whatever was selected instead, and not "Random", which would promise a stranger
     * per transmission when the channel is going to use one fixed voice.
     * <p>
     * A carrier with nothing stored is the separate case that really is "not picked", and keeps drawing a
     * stranger.
     */
    @Test
    void aCarrierVoiceThatHasLeftTheRadioRosterShowsTheVoiceItWillBeHeardIn() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        Language previousLanguage = session.getLanguage();
        try {
            session.setTtsProvider(TtsProvider.KOKORO);
            session.setLanguage(Language.EN); // RadioVoicing hands the Cyrillic locales to Edge instead

            // The commander's pick is kept whenever the engine still carries it.
            assertEquals(KokoroVoices.GEORGE.name(), CommanderTabPanel.carrierVoiceCell(KokoroVoices.GEORGE.name()));
            // Named as a string on purpose: the point is a name that no longer compiles against the enum,
            // which is what a commander who picked it before it was removed still has in the database.
            assertEquals(KokoroVoices.DEFAULT_VOICE.name(), CommanderTabPanel.carrierVoiceCell("ZH_YUNYANG"),
                    "a voice removed from the cast is heard as the default, so it must be shown as the default");
            assertEquals(CommanderTabPanel.RANDOM_VOICE, CommanderTabPanel.carrierVoiceCell(null),
                    "never picked - still a stranger per transmission");
            assertEquals(CommanderTabPanel.RANDOM_VOICE, CommanderTabPanel.carrierVoiceCell(""));
            // Not tested against another engine's roster: the three engines share enum names by design
            // (see this class's header), so a Google name is often a Kokoro name too.
        } finally {
            session.setTtsProvider(previousProvider);
            session.setLanguage(previousLanguage);
        }
    }

    @Test
    void kokoroNormalizationIsUnaffectedByTheEdgeFix() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        try {
            session.setTtsProvider(TtsProvider.KOKORO);
            assertEquals(KokoroVoices.NOVA.name(),
                    CommanderTabPanel.normalizeVoice(KokoroVoices.NOVA.name()));
            assertEquals(KokoroVoices.GEORGE.name(),
                    CommanderTabPanel.normalizeVoice(KokoroVoices.GEORGE.name()));
        } finally {
            session.setTtsProvider(previousProvider);
        }
    }
}
