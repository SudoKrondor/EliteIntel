package elite.intel.ai.brain.commons;

import elite.intel.ai.KeyDetector;
import elite.intel.ai.ProviderEnum;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

public final class AiResponseLanguagePolicy {

    private AiResponseLanguagePolicy() {
    }


    /**
     * Resolves the effective AI response language based on the system session configuration
     * and available Text-to-Speech (TTS) settings.
     * <p>
     * KOKORO Supports English, French, and Spanish.
     *
     * @param systemSession the session containing system language and TTS configuration details
     * @return the resolved language for AI responses; it will be the session's language if Google TTS
     * is configured and usable, or defaults to English unless the session's language is French or Spanish
     */
    public static Language resolveEffectiveAiResponseLanguage(SystemSession systemSession) {
        boolean isGoogle = isGoogleTtsConfiguredAndUsable(systemSession);

        if (isGoogle) {
            return systemSession.getLanguage();
        }

        Language sessionLanguage = systemSession.getLanguage();
        if (sessionLanguage == Language.FR || sessionLanguage == Language.ES || sessionLanguage == Language.PT) {
            return sessionLanguage;
        }

        return Language.EN;
    }

    public static boolean isGoogleTtsConfiguredAndUsable(SystemSession systemSession) {
        if (systemSession.useLocalTTS()) {
            return false;
        }
        return KeyDetector.detectProvider(systemSession.getTtsApiKey(), "TTS") == ProviderEnum.GOOGLE_TTS;
    }
}
