package elite.intel.ai.brain.commons;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

public final class AiResponseLanguagePolicy {

    private AiResponseLanguagePolicy() {
    }


    /**
     * Resolves the effective AI response language based on the system session configuration
     * and available Text-to-Speech (TTS) settings.
     * <p>
     * Google, Edge and Supertonic voice every language we ship, so they impose nothing. The local Kokoro TTS
     * constrains only Cyrillic: its phonemizer cannot read the script at all, so RU/UK would have to be
     * answered in English or they would not be spoken. Every other language is Latin-script and Kokoro speaks
     * it, using its nearest voice where it has no native one — German is voiced with an accent, which beats
     * answering a German commander in English.
     * <p>
     * In practice a Cyrillic commander never reaches that fallback: {@code SystemSession.getTtsProvider()}
     * withdraws Kokoro from them entirely (see {@link elite.intel.ai.mouth.TtsProvider#forLanguage}), so they
     * are on Supertonic, Edge or Google and are answered in their own language. The English branch stays as
     * the guard that makes this method true of any engine, not only of today's four - which is why it asks
     * the engine ({@link TtsProvider#canVoice}) rather than naming the engines that pass.
     *
     * @param systemSession the session containing system language and TTS configuration details
     * @return the session's language, except when the local TTS would have to voice Cyrillic, in which
     * case English
     */
    public static Language resolveEffectiveAiResponseLanguage(SystemSession systemSession) {
        Language sessionLanguage = systemSession.getLanguage();

        if (systemSession.getTtsProvider().canVoice(sessionLanguage)) {
            return sessionLanguage;
        }

        return sessionLanguage.isCyrillicScript() ? Language.EN : sessionLanguage;
    }

    public static boolean isGoogleTtsConfiguredAndUsable(SystemSession systemSession) {
        return systemSession.getTtsProvider() == TtsProvider.GOOGLE;
    }
}
