package elite.intel.ai.mouth;

import elite.intel.gameapi.GameLanguage;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Which engine voices radio transmissions, and why it is not simply the main mouth.
 * <p>
 * A transmission is the other side of a comms link, not the ship's own voice, so it is deliberately spoken by
 * a different engine and a random voice - accents and strangers are what make the galaxy sound populated.
 * A local engine owns that job: it is free, and its multi-speaker model gives the variety the channel lives on.
 * Kokoro's cast is the first choice, but it cannot do Cyrillic at all (no Cyrillic front end in its
 * phonemizer - see {@link Language#isCyrillicScript()}), so a Russian game client's chatter goes to Supertonic
 * instead - still local, still free, ten speakers rather than a few dozen.
 * <p>
 * A transmission is the only thing we say that we did not write: the words are the game client's own, in the
 * <em>client's</em> language, which is independent of the language the commander set here. So the radio engine
 * follows the client's language, not the commander's, and not the main mouth's: a commander on Supertonic
 * with an English client gets Kokoro's wider cast on the channel, loaded beside it. The other way round does
 * not arise: a Russian client withdraws Kokoro as the main mouth too ({@link TtsProvider#forSession}), so the
 * Supertonic that has to be resident for the chatter is also the one narrating. The channel used to be closed
 * for a Russian client instead.
 * <p>
 * Every local engine reads this once at start, so exactly one engine ever claims a radio request: the main
 * mouth voices radio only when it is also the radio engine, and a dedicated {@code RADIO_MOUTH} service runs
 * the radio engine alongside the main mouth when the two differ. The cloud mouths never touch radio.
 */
public final class RadioVoicing {

    private RadioVoicing() {
    }

    /**
     * The engine that voices a transmission written in {@code transmissionLanguage}: Kokoro wherever it can
     * pronounce the script, Supertonic where it cannot (see {@link TtsProvider#forLanguage}). Google is never
     * a radio engine - it is the paid main mouth, and radio is chatter, not narration - and neither is Edge,
     * now that a local engine can read Cyrillic.
     */
    public static TtsProvider engineFor(Language transmissionLanguage) {
        return TtsProvider.forLanguage(TtsProvider.KOKORO, transmissionLanguage);
    }

    /**
     * The radio engine for the game client the commander is running.
     */
    public static TtsProvider engine() {
        return engineFor(transmissionLanguage());
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
     * request: the answer changes only with the game client's language (or the commander's, standing in for
     * an unknown client), and either restarts the mouth - see {@code FileheaderEventSubscriber} and
     * {@code AppController#onLanguageChangedEvent}.
     */
    public static boolean isRadioEngine(TtsProvider provider) {
        return engine() == provider;
    }
}
