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
     * Google TTS voices every language we ship, so it imposes nothing. The local Kokoro TTS constrains
     * only Cyrillic: its phonemizer cannot read the script at all, so RU/UK have to be answered in
     * English or they would not be spoken. Every other language is Latin-script and Kokoro speaks it,
     * using its nearest voice where it has no native one — German is voiced with an accent, which beats
     * answering a German commander in English.
     *
     * @param systemSession the session containing system language and TTS configuration details
     * @return the session's language, except when the local TTS would have to voice Cyrillic, in which
     * case English
     */
    public static Language resolveEffectiveAiResponseLanguage(SystemSession systemSession) {
        Language sessionLanguage = systemSession.getLanguage();

        if (isGoogleTtsConfiguredAndUsable(systemSession)) {
            return sessionLanguage;
        }

        return sessionLanguage.isCyrillicScript() ? Language.EN : sessionLanguage;
    }

    public static boolean isGoogleTtsConfiguredAndUsable(SystemSession systemSession) {
        if (systemSession.useLocalTTS()) {
            return false;
        }
        return KeyDetector.detectProvider(systemSession.getTtsApiKey(), "TTS") == ProviderEnum.GOOGLE_TTS;
    }
}
