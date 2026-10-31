package elite.intel.ai.ears;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Playback counts as audible to the hands-free microphone while VEGA or the radio plays and for a short tail
 * after, so a commander on speakers is never transcribed hearing her.
 */
class PlaybackEchoTest {

    private final AtomicLong nowNanos = new AtomicLong(1_000_000_000L);
    private final AtomicBoolean playing = new AtomicBoolean();
    private final PlaybackEcho echo = new PlaybackEcho(nowNanos::get, playing::get);

    @Test
    void silentBeforeAnythingHasPlayed() {
        assertFalse(echo.isAudible());
    }

    // WHY no onPlayback call: the Mouth greets before the microphone starts, so no "started" event reaches it.
    @Test
    void audibleWhilePlaying() {
        playing.set(true);
        advanceMs(10_000);
        assertTrue(echo.isAudible());
    }

    @Test
    void audibleThroughTheTailThenSilent() {
        playing.set(true);
        stop();

        advanceMs(PlaybackEcho.TAIL_MS - 1);
        assertTrue(echo.isAudible(), "the speakers and the room are still ringing");

        advanceMs(1);
        assertFalse(echo.isAudible());
    }

    @Test
    void playbackResumingInsideTheTailIsAudibleAgain() {
        playing.set(true);
        stop();
        advanceMs(100);
        playing.set(true);
        advanceMs(PlaybackEcho.TAIL_MS * 10);
        assertTrue(echo.isAudible());
    }

    private void stop() {
        playing.set(false);
        echo.onPlaybackStopped();
    }

    private void advanceMs(long ms) {
        nowNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(ms));
    }
}
