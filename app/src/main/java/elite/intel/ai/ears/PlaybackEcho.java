package elite.intel.ai.ears;

import elite.intel.ai.mouth.VocalisationHandle;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
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
 * Whether something plays is read live ({@link VocalisationHandle#isAnyPlaying()}), never remembered from the
 * {@link IsSpeakingEvent} stream: the microphone starts after the Mouth has already queued its startup lines, so it
 * would miss the one "started" event of that burst and transcribe all of it as orders. The "stopped" event still
 * arms the tail.
 * <p>
 * Push-to-talk never consults this - a held button is a deliberate order and still talks over her.
 */
public final class PlaybackEcho {

    static final long TAIL_MS = 300;
    private static final long TAIL_NANOS = TimeUnit.MILLISECONDS.toNanos(TAIL_MS);

    private final LongSupplier nanoClock;
    private final BooleanSupplier playingNow;
    private volatile long audibleUntilNanos;

    public PlaybackEcho() {
        this(System::nanoTime, VocalisationHandle::isAnyPlaying);
    }

    PlaybackEcho(LongSupplier nanoClock, BooleanSupplier playingNow) {
        this.nanoClock = nanoClock;
        this.playingNow = playingNow;
        this.audibleUntilNanos = nanoClock.getAsLong();
    }

    /**
     * Playback has just stopped: the room is still ringing for {@link #TAIL_MS}.
     */
    public void onPlaybackStopped() {
        audibleUntilNanos = nanoClock.getAsLong() + TAIL_NANOS;
    }

    public boolean isAudible() {
        return playingNow.getAsBoolean() || nanoClock.getAsLong() - audibleUntilNanos < 0;
    }
}
