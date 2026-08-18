package elite.intel.ai.brain.commons;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiResponseLanguagePolicyTest {
    @Test
    void edgeCloudTtsKeepsTheConfiguredCyrillicLanguage() {
        SystemSession session = SystemSession.getInstance();
        boolean previousLocal = session.useLocalTTS();
        String previousKey = session.getTtsApiKey();
        Language previousLanguage = session.getLanguage();
        try {
            session.setUseLocalTTS(false);
            session.setTtsApiKey("edge://");
            session.setLanguage(Language.UK);

            assertEquals(Language.UK, AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(session));
        } finally {
            session.setUseLocalTTS(previousLocal);
            session.setTtsApiKey(previousKey);
            session.setLanguage(previousLanguage);
        }
    }
}
