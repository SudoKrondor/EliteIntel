package elite.intel.ai.ears;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whether the commander is holding the push-to-talk button right now, published by {@link PushToTalkService}.
 * <p>
 * The companion's own use of the button is timed by the capture loop off {@code PttButtonStateEvent} and does
 * not need this. This exists for the other listener: a commander on speakers rather than headphones has the
 * jukebox playing into the same room the microphone is in, so an audiobook or a song with words is sitting on
 * top of every word they say. The music comes down while the button is held.
 * <p>
 * Like {@link elite.intel.ai.mouth.VoiceLevelTap} this is a detector and nothing more - it reports the button,
 * it does not decide what the button means. How far to duck and how fast belongs to the listener, which is why
 * no decibel figure appears here and why the ears package has no idea the jukebox exists.
 * <p>
 * <b>Why a level rather than an event.</b> The reader is the jukebox playback thread, which is already asking
 * once per block of audio and must never block on a bus dispatch; a released button that fails to reach the
 * jukebox would leave the music ducked for good, so the state is read fresh rather than mirrored.
 */
public final class PushToTalkHoldTap {

    private static final AtomicBoolean HELD = new AtomicBoolean(false);

    private PushToTalkHoldTap() {
    }

    /**
     * Records the button going down or coming back up.
     */
    public static void observe(boolean held) {
        HELD.set(held);
    }

    /**
     * Whether the button is held at this instant. False whenever push-to-talk is off, because nothing then
     * reports a press.
     */
    public static boolean isHeld() {
        return HELD.get();
    }

    /**
     * Forgets the button, so it reads as up immediately. For tests and shutdown.
     */
    public static void reset() {
        HELD.set(false);
    }
}
