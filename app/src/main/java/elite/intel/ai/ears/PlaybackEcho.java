package elite.intel.ai.ears;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Whether the hands-free microphone may be hearing the app's own voice right now.
 * <p>
 * A commander who runs hands-free on speakers hears VEGA and the radio through the same room the microphone
 * listens to, so the VAD opens on our own voice and the transcript of it comes back as an order - she answers
 * herself, or cuts herself off. Whether it is speakers or headphones cannot be known, so the rule is the same
 * for both: a hands-free capture heard while anything plays ({@link IsSpeakingEvent}, which VEGA and radio
 * transmissions both raise), or within {@link #TAIL_MS} after it stops, admits nothing but an exact interrupt
 * phrase. The tail covers what is still leaving the speaker and the room after playback reports done.
 * <p>
 * Push-to-talk never consults this - a held button is a deliberate order and still talks over her.
 */
public final class PlaybackEcho {

    static final long TAIL_MS = 300;
    private static final long TAIL_NANOS = TimeUnit.MILLISECONDS.toNanos(TAIL_MS);

    private final LongSupplier nanoClock;
    private volatile boolean playing;
    private volatile long audibleUntilNanos;

    public PlaybackEcho() {
        this(System::nanoTime);
    }

    PlaybackEcho(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
        this.audibleUntilNanos = nanoClock.getAsLong();
    }

    public void onPlayback(boolean isPlaying) {
        if (!isPlaying) {
            // The tail is armed before playing drops, so a capture thread reading between the two writes
            // still sees one of them as audible.
            audibleUntilNanos = nanoClock.getAsLong() + TAIL_NANOS;
        }
        playing = isPlaying;
    }

    public boolean isAudible() {
        return playing || nanoClock.getAsLong() - audibleUntilNanos < 0;
    }
}
