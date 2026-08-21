package elite.intel.ai.ears;

/**
 * What stands between the microphone and the companion, decided once per finished utterance.
 * <p>
 * There are two gates and never more than one of them is in force. <b>Push-to-talk</b>, when enabled, makes
 * the mapped controller button the only thing that opens the microphone: the capture window in the STT loop
 * is the button itself, so audio heard with the button up is never collected and never reaches a transcript.
 * That makes {@link #CLOSED_PUSH_TO_TALK} a backstop rather than the everyday path - it catches an utterance
 * that was already in flight when the mode changed under it. <b>Sleep/Wake</b> is the hands-free gate: closed, she is not listening
 * at all: the AI tab button reopens her, and so does a spoken wake phrase, which is the one thing a
 * sleeping companion still listens for. A listen-prefixed order ("listen, open the galaxy map") gets through
 * the same way, which is why {@link #CLOSED_ASLEEP} is a gate to be asked about a transcript rather than a
 * verdict on one.
 * <p>
 * Sleep is not consulted under push-to-talk. The button already gates the microphone there, so asking a
 * second gate could only refuse an utterance the commander deliberately pressed for; it is also why the
 * Sleep/Wake button is disabled while push-to-talk is on.
 * <p>
 * A discarded transcript dies here, before it costs an AI round trip.
 */
public enum MicrophoneGate {

    /**
     * The commander held the button: an explicit order, routed as a push-to-talk capture.
     */
    OPEN_PUSH_TO_TALK,
    /**
     * Neither gate is closed: routed as an ordinary hands-free utterance.
     */
    OPEN_HANDS_FREE,
    /**
     * Push-to-talk is armed and the button was not held for this utterance - which now means the capture
     * started before the mode did: the transcript is dropped.
     */
    CLOSED_PUSH_TO_TALK,
    /**
     * Hands-free and asleep: only a wake phrase, or a listen-prefixed order, gets past.
     */
    CLOSED_ASLEEP;

    public static MicrophoneGate decide(boolean capturedWithPttHeld, boolean pushToTalkEnabled, boolean sleeping) {
        if (capturedWithPttHeld) return OPEN_PUSH_TO_TALK;
        if (pushToTalkEnabled) return CLOSED_PUSH_TO_TALK;
        if (sleeping) return CLOSED_ASLEEP;
        return OPEN_HANDS_FREE;
    }
}
