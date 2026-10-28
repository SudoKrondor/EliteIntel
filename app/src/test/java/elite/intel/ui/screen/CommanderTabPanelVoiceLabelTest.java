package elite.intel.ui.screen;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The fleet grid stores and shows a ship's voice by its logical enum name, resolved against the engine that is
 * speaking.
 */
class CommanderTabPanelVoiceLabelTest {

    @Test
    void googleNormalizationKeepsAVoiceGoogleCarries() {
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
     * A carrier can hold a voice the radio cast no longer carries - a Kokoro voice that was not refitted for
     * Supertonic, say. The grid has to show
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
        Language previousLanguage = session.getLanguage();
        try {
            session.setLanguage(Language.EN); // with no journal in the test, the radio language falls back to the app language

            // The commander's pick is kept whenever the engine still carries it.
            assertEquals(SupertonicVoices.GEORGE.name(), CommanderTabPanel.carrierVoiceCell(SupertonicVoices.GEORGE.name()));
            // Named as a string on purpose: the point is a name that no longer compiles against the enum,
            // which is what a commander who picked it before it was removed still has in the database.
            assertEquals(SupertonicVoices.DEFAULT_VOICE.name(), CommanderTabPanel.carrierVoiceCell("ZH_YUNYANG"),
                    "a voice removed from the cast is heard as the default, so it must be shown as the default");
            assertEquals(CommanderTabPanel.RANDOM_VOICE, CommanderTabPanel.carrierVoiceCell(null),
                    "never picked - still a stranger per transmission");
            assertEquals(CommanderTabPanel.RANDOM_VOICE, CommanderTabPanel.carrierVoiceCell(""));
            // Not tested against another engine's roster: the engines share enum names by design, so a
            // Google name is often a Supertonic name too.
        } finally {
            session.setLanguage(previousLanguage);
        }
    }

    @Test
    void kokoroNormalizationKeepsAVoiceKokoroCarries() {
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
