package elite.intel.ai.ears;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Playback counts as audible to the hands-free microphone while VEGA or the radio plays and for a short tail
 * after, so a commander on speakers is never transcribed hearing her.
 */
class PlaybackEchoTest {

    private final AtomicLong nowNanos = new AtomicLong(1_000_000_000L);
    private final PlaybackEcho echo = new PlaybackEcho(nowNanos::get);

    @Test
    void silentBeforeAnythingHasPlayed() {
        assertFalse(echo.isAudible());
    }

    @Test
    void audibleWhilePlaying() {
        echo.onPlayback(true);
        advanceMs(10_000);
        assertTrue(echo.isAudible());
    }

    @Test
    void audibleThroughTheTailThenSilent() {
        echo.onPlayback(true);
        echo.onPlayback(false);

        advanceMs(PlaybackEcho.TAIL_MS - 1);
        assertTrue(echo.isAudible(), "the speakers and the room are still ringing");

        advanceMs(1);
        assertFalse(echo.isAudible());
    }

    @Test
    void playbackResumingInsideTheTailIsAudibleAgain() {
        echo.onPlayback(true);
        echo.onPlayback(false);
        advanceMs(100);
        echo.onPlayback(true);
        advanceMs(PlaybackEcho.TAIL_MS * 10);
        assertTrue(echo.isAudible());
    }

    private void advanceMs(long ms) {
        nowNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(ms));
    }
}
