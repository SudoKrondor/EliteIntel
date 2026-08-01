package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The overlay ships as a separate binary, so it can simply be absent: stripped by an installer, lost
 * by an auto-update, or never built on a contributor's machine. {@code AiTabPanel} decides whether to
 * flip its toggle from what {@code start()} returns, so a start that fails must say so rather than
 * throw or claim success - otherwise the button says HIDE OVERLAY with nothing on screen.
 */
class NativeHudOverlayLifecycleTest {

    @Test
    void aMissingBinaryFailsToStartInsteadOfThrowing() {
        NativeHudOverlay overlay = new NativeHudOverlay(() -> Path.of("/nonexistent/elite-intel-overlay"));

        boolean started = assertDoesNotThrow(overlay::start);

        assertFalse(started, "a missing binary must leave the toggle honest");
        assertFalse(overlay.isRunning());
    }

    /**
     * A directory is a path that exists and still is not something we can run.
     */
    @Test
    void aPathThatIsNotAFileFailsTheSameWay() {
        NativeHudOverlay overlay = new NativeHudOverlay(() -> Path.of(System.getProperty("java.io.tmpdir")));

        assertFalse(overlay.start());
        assertFalse(overlay.isRunning());
    }

    /**
     * Teardown runs on the failure path too: nothing was started, and stop() must not care.
     */
    @Test
    void stoppingSomethingThatNeverStartedIsHarmless() {
        NativeHudOverlay overlay = new NativeHudOverlay(() -> Path.of("/nonexistent/elite-intel-overlay"));
        overlay.start();

        assertDoesNotThrow(overlay::stop);
        assertDoesNotThrow(overlay::stop, "a double stop is a normal toggle sequence");
        assertFalse(overlay.isRunning());
    }
}
