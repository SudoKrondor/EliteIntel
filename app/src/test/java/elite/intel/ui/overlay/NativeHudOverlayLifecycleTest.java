package elite.intel.ui.overlay;

import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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

    /**
     * The restore guard, which is where the bug actually lived. Persisting a negative coordinate always
     * worked; what failed was applying one, because the guard read it as the "unset" sentinel and left the
     * window at its centred default with only the y honoured.
     */
    @Test
    void aNegativeStoredCoordinateCountsAsAPosition() {
        SystemSession session = SystemSession.getInstance();
        SystemSession.HudOverlayLayout previous = session.getHudOverlayLayout();
        try {
            session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(
                    0.5, 1.0, 760, -3, 590, "DESKTOP", "TOP_RIGHT"));
            assertTrue(new NativeHudOverlay(() -> Path.of("/nonexistent/elite-intel-overlay"))
                    .hasStoredPosition(), "a card parked past the left edge has a position to restore");

            session.setHudOverlayLayout(new SystemSession.HudOverlayLayout(
                    0.5, 1.0, 760, OverlayProtocol.POSITION_UNSET, OverlayProtocol.POSITION_UNSET,
                    "DESKTOP", "TOP_RIGHT"));
            assertFalse(new NativeHudOverlay(() -> Path.of("/nonexistent/elite-intel-overlay"))
                    .hasStoredPosition(), "a never-positioned card must still open where it defaults to");
        } finally {
            session.setHudOverlayLayout(previous);
        }
    }
}
