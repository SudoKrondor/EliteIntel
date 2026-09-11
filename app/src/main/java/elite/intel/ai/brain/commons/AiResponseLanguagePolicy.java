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
     * Every engine this app ships - Google, Edge, and the local Supertonic engine - voices every language we
     * ship, Cyrillic included, so none of them impose anything here today: the session language is always the
     * effective one. The English-for-Cyrillic fallback is kept as a guard for an engine that cannot voice the
     * configured language (see {@link elite.intel.ai.mouth.TtsProvider#canVoice}), which no current
     * engine/language pair triggers, rather than deleted and rediscovered the hard way if one ever does.
     *
     * @param systemSession the session containing system language and TTS configuration details
     * @return the session's language, except when the active TTS engine cannot voice it, in which
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
