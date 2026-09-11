package elite.intel.ai.mouth;

import elite.intel.gameapi.GameLanguage;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/**
 * Which engine voices radio transmissions, and why it is not simply the main mouth.
 * <p>
 * A transmission is the other side of a comms link, not the ship's own voice, so it is deliberately spoken by
 * a different engine and a random voice - accents and strangers are what make the galaxy sound populated.
 * Supertonic owns that job everywhere: it is local, free, and its 10-speaker multilingual model gives the
 * variety the channel lives on. Unlike Kokoro, the local engine this one replaced, it needs no Cyrillic
 * exception - it voices Russian and Ukrainian radio the same way it voices everything else.
 * <p>
 * Every mouth consults this before touching a radio request, so exactly one engine ever claims one: the main
 * mouth voices radio only when it is also the radio engine, and a dedicated {@code RADIO_MOUTH} service runs
 * the radio engine alongside the main mouth when the two differ.
 */
public final class RadioVoicing {

    private RadioVoicing() {
    }

    /**
     * The engine that voices radio for a language: Supertonic for every language this app ships.
     * Google is never a radio engine - it is the paid main mouth, and radio is chatter, not narration.
     */
    public static TtsProvider engineFor(Language language) {
        return TtsProvider.forLanguage(TtsProvider.SUPERTONIC, language);
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

    /**
     * Whether radio transmissions can be voiced at all in this game session.
     * <p>
     * A transmission is the only thing we say that we did not write: the words are the game client's own, in
     * the client's language, which is independent of the language the commander set here. A Russian client
     * therefore hands Cyrillic to an engine chosen for a commander who may not be running Cyrillic at all, and
     * the result is either silence or a voice reading a script it cannot pronounce. The channel is closed
     * instead, and the commander is shown why rather than left with a toggle that does nothing.
     * <p>
     * Russian is the whole of the Cyrillic case: Frontier ships no Ukrainian client.
     * <p>
     * WHY the channel is closed rather than handed to Edge, which could pronounce it: where it would cost
     * nothing - a Latin-script commander, whose main mouth is a different engine, so radio would run as a
     * separate RADIO-role service that the main voice never waits on - is the rare case. In the common one,
     * a Russian commander on a Russian client, Edge is already the main mouth, and this engine puts narration
     * and radio through ONE queue (see {@code EdgeTTSImpl.Role}), so chatter would queue in front of the
     * commander's answers. Reopening it means giving radio its own Edge instance, not flipping this method.
     */
    public static boolean isAvailable() {
        return !GameLanguage.getInstance().isCyrillicScript();
    }
}
