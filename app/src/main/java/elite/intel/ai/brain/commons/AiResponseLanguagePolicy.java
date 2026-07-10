package elite.intel.ai.brain.commons;

import elite.intel.ai.KeyDetector;
import elite.intel.ai.ProviderEnum;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.EnumSet;

public final class AiResponseLanguagePolicy {

    /**
     * Languages the local Kokoro TTS can voice. Both Portuguese variants qualify: Kokoro's only
     * Portuguese model is Brazilian, so European Portuguese is spoken with a Brazilian accent
     * rather than falling back to English.
     */
    private static final EnumSet<Language> KOKORO_SPOKEN_LANGUAGES =
            EnumSet.of(Language.FR, Language.ES, Language.PT, Language.PTBZ, Language.IT);

    private AiResponseLanguagePolicy() {
    }


    /**
     * Resolves the effective AI response language based on the system session configuration
     * and available Text-to-Speech (TTS) settings.
     * <p>
     * KOKORO Supports English, French, Spanish, Portuguese, and Italian.
     *
     * @param systemSession the session containing system language and TTS configuration details
     * @return the resolved language for AI responses; it will be the session's language if Google TTS
     * is configured and usable, or defaults to English unless the session's language is one Kokoro can
     * voice ({@link #KOKORO_SPOKEN_LANGUAGES})
     */
    public static Language resolveEffectiveAiResponseLanguage(SystemSession systemSession) {
        boolean isGoogle = isGoogleTtsConfiguredAndUsable(systemSession);

        if (isGoogle) {
            return systemSession.getLanguage();
        }

        Language sessionLanguage = systemSession.getLanguage();
        if (KOKORO_SPOKEN_LANGUAGES.contains(sessionLanguage)) {
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
