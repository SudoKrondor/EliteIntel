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
 * Kokoro's 53-speaker cast is the first choice, but it cannot do Cyrillic at all (no Cyrillic front end in its
 * phonemizer - see {@link Language#isCyrillicScript()}), so for Russian and Ukrainian commanders Supertonic
 * voices radio instead - still local, still free, ten speakers rather than fifty-three.
 * <p>
 * When the main mouth is itself a local engine it voices radio too, whichever local engine it is: a commander
 * who picked Supertonic as their voice does not also get Kokoro loaded beside it for the sake of the wider
 * cast - two ONNX models resident for one channel is not worth it.
 * <p>
 * Every local engine reads this once at start, so exactly one engine ever claims a radio request: the main
 * mouth voices radio only when it is also the radio engine, and a dedicated {@code RADIO_MOUTH} service runs
 * the radio engine alongside the main mouth when the two differ. The cloud mouths never touch radio.
 */
public final class RadioVoicing {

    private RadioVoicing() {
    }

    /**
     * The engine that voices radio: the main mouth itself when it is a local engine, otherwise Kokoro for
     * the Latin-script locales and Supertonic for the Cyrillic ones. Google is never a radio engine - it is
     * the paid main mouth, and radio is chatter, not narration - and neither is Edge, now that a local
     * engine can read Cyrillic.
     *
     * @param mainMouth the engine voicing VEGA. The stored selection is enough here even though a keyless
     *                  Google is replaced by a local engine at start-up ({@code ApiFactory}): the stand-in
     *                  is exactly the local engine that would voice Google's radio, so the answer is the
     *                  same either way. Only the decision whether a separate radio service is needed has
     *                  to see the substitution, and that is made where it happens, in the controller.
     */
    public static TtsProvider engineFor(Language language, TtsProvider mainMouth) {
        if (mainMouth.isLocal()) return mainMouth;
        return TtsProvider.forLanguage(TtsProvider.KOKORO, language);
    }

    /**
     * The radio engine for the language and main mouth the commander is running.
     */
    public static TtsProvider engine() {
        SystemSession session = SystemSession.getInstance();
        return engineFor(session.getLanguage(), session.getTtsProvider());
    }

    /**
     * Whether the given engine is the one that must voice radio. Read once per service start, not per
     * request: the answer changes only with the language or the engine, and either restarts the mouth.
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
     * WHY the channel stays closed even though Supertonic could now pronounce it: the radio engine is picked
     * for the commander's language, not the client's, so a Latin-script commander on a Russian client still
     * hands Cyrillic to Kokoro, and a Russian commander on a Russian client is on Supertonic as their main
     * mouth, where narration and radio share ONE queue and chatter would queue in front of the commander's
     * answers. Reopening it means picking the radio engine from the client's language as well, and giving
     * that case its own radio instance - a change in its own right, not a flip of this method.
     */
    public static boolean isAvailable() {
        return !GameLanguage.getInstance().isCyrillicScript();
    }
}
