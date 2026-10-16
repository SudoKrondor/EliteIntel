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
 * <p>
 * A transmission is the only thing we say that we did not write: the words are the game client's own, in the
 * <em>client's</em> language, which is independent of the language the commander set here. Kokoro voices them,
 * except under a Russian client: its chatter is Cyrillic, which Kokoro's phonemizer cannot read at all, so
 * Supertonic voices it (see {@link GameLanguage#isRussian()}). A commander on Supertonic with any other client
 * gets Kokoro's wider cast on the channel, loaded beside it; a commander on Kokoro with a Russian client gets
 * Supertonic beside it.
 * <p>
 * Every local engine reads this once at start, so exactly one engine ever claims a radio request: the main
 * mouth voices radio only when it is also the radio engine, and a dedicated {@code RADIO_MOUTH} service runs
 * the radio engine alongside the main mouth when the two differ. The cloud mouths never touch radio.
 */
public final class RadioVoicing {

    private RadioVoicing() {
    }

    /**
     * The radio engine for the game client the commander is running. Google is never a radio engine - it is
     * the paid main mouth, and radio is chatter, not narration.
     */
    public static TtsProvider engine() {
        return engineFor(isRussianClient());
    }

    /**
     * Seam for tests: the radio engine for a client that is, or is not, the Russian edition.
     */
    static TtsProvider engineFor(boolean russianClient) {
        return russianClient ? TtsProvider.SUPERTONIC : TtsProvider.KOKORO;
    }

    /**
     * Whether transmissions are written in Russian - a language this app does not otherwise speak, which only
     * Supertonic reads. {@link #transmissionLanguage()} has no answer for it.
     */
    public static boolean isRussianClient() {
        return GameLanguage.getInstance().isRussian();
    }

    /**
     * The language a radio transmission is written in: the game client's, when its journal header says which
     * it is, otherwise the commander's own - a commander whose client we cannot identify is most likely playing
     * it in the language they speak to us. Under a Russian client this is the commander's language too, and
     * means nothing for the transmission itself (see {@link #isRussianClient()}).
     */
    public static Language transmissionLanguage() {
        return GameLanguage.getInstance().language().orElseGet(() -> SystemSession.getInstance().getLanguage());
    }

    /**
     * Whether the given engine is the one that must voice radio. Read once per service start, not per
     * request: the answer changes only with the game client's language, which restarts the mouth - see
     * {@code FileheaderEventSubscriber}.
     */
    public static boolean isRadioEngine(TtsProvider provider) {
        return engine() == provider;
    }
}
