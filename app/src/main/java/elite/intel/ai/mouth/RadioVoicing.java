package elite.intel.ai.mouth;

import elite.intel.gameapi.GameLanguage;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Which engine voices radio transmissions, and why it is not simply the main mouth.
 * <p>
 * A transmission is the other side of a comms link, not the ship's own voice, so it is deliberately spoken by
 * a local engine and a random voice - accents and strangers are what make the galaxy sound populated.
 * A local engine owns that job: it is free, and its multi-speaker model gives the variety the channel lives on.
 * <p>
 * A transmission is the only thing we say that we did not write: the words are the game client's own, in the
 * <em>client's</em> language, which is independent of the language the commander set here. Supertonic voices
 * them for every client: it reads every language the game ships in, Russian included, and takes the language
 * per call. A commander on Supertonic voices radio with the main mouth; a commander on Google gets Supertonic
 * loaded beside it.
 * <p>
 * Every local engine reads this once at start, so exactly one engine ever claims a radio request: the main
 * mouth voices radio only when it is also the radio engine, and a dedicated {@code RADIO_MOUTH} service runs
 * the radio engine alongside the main mouth when the two differ. The cloud mouths never touch radio.
 */
public final class RadioVoicing {

    private RadioVoicing() {
    }

    /**
     * The radio engine. Google is never a radio engine - it is the paid main mouth, and radio is chatter, not
     * narration.
     */
    public static TtsProvider engine() {
        return TtsProvider.SUPERTONIC;
    }

    /**
     * The language a radio transmission is written in: the game client's, when its journal header says which
     * it is, otherwise the commander's own - a commander whose client we cannot identify is most likely playing
     * it in the language they speak to us.
     */
    public static Language transmissionLanguage() {
        return GameLanguage.getInstance().language().orElseGet(() -> SystemSession.getInstance().getLanguage());
    }

    /**
     * Whether the given engine is the one that must voice radio. Read once per service start, not per
     * request.
     */
    public static boolean isRadioEngine(TtsProvider provider) {
        return engine() == provider;
    }
}
