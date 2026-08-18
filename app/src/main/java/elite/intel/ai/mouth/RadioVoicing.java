package elite.intel.ai.mouth;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Which engine voices radio transmissions, and why it is not simply the main mouth.
 * <p>
 * A transmission is the other side of a comms link, not the ship's own voice, so it is deliberately spoken by
 * a different engine and a random voice - accents and strangers are what make the galaxy sound populated.
 * Kokoro owns that job everywhere it can: it is local, free, and its 53-speaker multi-language model gives the
 * variety the channel lives on. It cannot do Cyrillic at all (no Cyrillic front end in its phonemizer - see
 * {@link Language#isCyrillicScript()}), so for Russian and Ukrainian commanders Edge Read Aloud voices radio
 * instead; Edge is keyless, so this costs a network round trip and nothing else.
 * <p>
 * Every mouth consults this before touching a radio request, so exactly one engine ever claims one: the main
 * mouth voices radio only when it is also the radio engine, and a dedicated {@code RADIO_MOUTH} service runs
 * the radio engine alongside the main mouth when the two differ.
 */
public final class RadioVoicing {

    private RadioVoicing() {
    }

    /**
     * The engine that voices radio for a language: Edge for the Cyrillic locales, Kokoro for the rest.
     * Google is never a radio engine - it is the paid main mouth, and radio is chatter, not narration.
     */
    public static TtsProvider engineFor(Language language) {
        return language.isCyrillicScript() ? TtsProvider.EDGE : TtsProvider.KOKORO;
    }

    /**
     * The radio engine for the language the commander is running.
     */
    public static TtsProvider engine() {
        return engineFor(SystemSession.getInstance().getLanguage());
    }

    /**
     * Whether the given engine is the one that must voice radio right now.
     */
    public static boolean isRadioEngine(TtsProvider provider) {
        return engine() == provider;
    }
}
