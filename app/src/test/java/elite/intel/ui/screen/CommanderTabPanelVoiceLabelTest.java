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
    void normalizeVoiceToFemaleKeepsTheLogicalNameForEdgeNotTheShortName() {
        runAsEdge(() -> {
            assertEquals(EdgeVoices.JENNIFER.name(),
                    CommanderTabPanel.normalizeVoiceToFemale(EdgeVoices.JENNIFER.name()));
            // A raw/legacy ShortName still normalizes to a logical name, keeping the stored value in the
            // same vocabulary as the dropdown's option list (enum names, see initData()).
            assertEquals(EdgeVoices.MARY.name(),
                    CommanderTabPanel.normalizeVoiceToFemale(EdgeVoices.MARY.defaultShortName()));
            // Ship voices are female-only: a legacy male voice resolves to the default female's logical name.
            assertEquals(EdgeVoices.DEFAULT_FEMALE.name(),
                    CommanderTabPanel.normalizeVoiceToFemale(EdgeVoices.JAKE.name()));
        });
    }

    @Test
    void normalizedEdgeSelectionStillResolvesToTheCorrectEdgeProviderVoice() {
        runAsEdge(() -> {
            String normalized = CommanderTabPanel.normalizeVoiceToFemale(EdgeVoices.JENNIFER.name());
            // This mirrors exactly how EdgeTTSImpl.enqueue() turns a fleet-grid voice selection into the
            // ShortName used for synthesis, proving the demo/play-preview path still reaches the right
            // Edge voice after the logical name round-trips through the fleet grid.
            assertEquals(EdgeVoices.JENNIFER.defaultShortName(),
                    EdgeVoices.femaleShortNameOrDefault(normalized));
        });
    }

    @Test
    void googleNormalizationIsUnaffectedByTheEdgeFix() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        try {
            session.setTtsProvider(TtsProvider.GOOGLE);
            assertEquals(GoogleVoices.femaleOrDefault(GoogleVoices.EMMA.name()).name(),
                    CommanderTabPanel.normalizeVoiceToFemale(GoogleVoices.EMMA.name()));
        } finally {
            session.setTtsProvider(previousProvider);
        }
    }

    @Test
    void kokoroNormalizationIsUnaffectedByTheEdgeFix() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        try {
            session.setTtsProvider(TtsProvider.KOKORO);
            assertEquals(KokoroVoices.femaleOrDefault(KokoroVoices.NOVA.name()).name(),
                    CommanderTabPanel.normalizeVoiceToFemale(KokoroVoices.NOVA.name()));
        } finally {
            session.setTtsProvider(previousProvider);
        }
    }
}
