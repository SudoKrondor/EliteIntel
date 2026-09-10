package elite.intel.ai.brain.commons;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiResponseLanguagePolicyTest {
    /**
     * The end of the Cyrillic English-fallback in practice: Kokoro is withdrawn from a Cyrillic commander at
     * the session read, so even a database that still stores it answers them in their own language rather than
     * in English.
     */
    @Test
    void aStoredKokoroNoLongerForcesEnglishOnACyrillicCommander() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        Language previousLanguage = session.getLanguage();
        try {
            session.setTtsProvider(TtsProvider.KOKORO);
            session.setLanguage(Language.RU);

            assertEquals(TtsProvider.EDGE, session.getTtsProvider(), "Kokoro cannot voice Cyrillic");
            assertEquals(Language.RU, AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(session));
        } finally {
            session.setTtsProvider(previousProvider);
            session.setLanguage(previousLanguage);
        }
    }

    @Test
    void edgeCloudTtsKeepsTheConfiguredCyrillicLanguage() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        Language previousLanguage = session.getLanguage();
        try {
            session.setTtsProvider(TtsProvider.EDGE);
            session.setLanguage(Language.UK);

            assertEquals(Language.UK, AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(session));
        } finally {
            session.setTtsProvider(previousProvider);
            session.setLanguage(previousLanguage);
        }
    }
}
