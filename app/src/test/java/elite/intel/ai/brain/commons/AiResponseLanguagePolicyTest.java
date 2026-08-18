package elite.intel.ai.brain.commons;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiResponseLanguagePolicyTest {
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
