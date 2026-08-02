package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What each display mode actually launches.
 * <p>
 * The flags are the whole contract between the setting the commander picks and
 * the binary's behaviour, and getting one wrong is not a crash - it is a second
 * overlay window nobody asked for, or a VR overlay that silently never appears.
 */
class HudDisplayModeChildrenTest {

    private static final Path BINARY = Path.of("/opt/elite/elite-intel-overlay");

    @Test
    void desktopLaunchesOneChildWithNoFlags() {
        List<NativeHudOverlay.ChildSpec> specs =
                NativeHudOverlay.childSpecs(BINARY, HudDisplayMode.DESKTOP);

        assertEquals(1, specs.size());
        assertEquals(List.of(BINARY.toString()), specs.get(0).command(),
                "the default must invoke the binary exactly as every version before VR existed");
    }

    /**
     * VR alone falls back to a window rather than showing nothing, so a commander
     * whose SteamVR is not running still gets their HUD.
     */
    @Test
    void vrAloneIsAllowedToFallBackToAWindow() {
        List<NativeHudOverlay.ChildSpec> specs =
                NativeHudOverlay.childSpecs(BINARY, HudDisplayMode.VR);

        assertEquals(1, specs.size());
        assertTrue(specs.get(0).command().contains("--vr=on"));
    }

    /**
     * The one that matters: in BOTH mode the desktop child is already drawing a
     * window, so the VR child must be the no-fallback flavour. With {@code
     * --vr=on} a machine without SteamVR would get two identical windows stacked
     * exactly on top of each other - the commander drags one and the other stays
     * put, which looks like the overlay being broken rather than VR being absent.
     */
    @Test
    void bothRunsADesktopChildAndAVrChildThatNeverFallsBack() {
        List<NativeHudOverlay.ChildSpec> specs =
                NativeHudOverlay.childSpecs(BINARY, HudDisplayMode.BOTH);

        assertEquals(2, specs.size());
        assertEquals(List.of(BINARY.toString()), specs.get(0).command(),
                "the desktop child is spawned exactly as it is in DESKTOP mode");
        assertTrue(specs.get(1).command().contains("--vr=only"),
                "the VR child must exit rather than open a second window");
    }

    /**
     * A stored mode is read leniently in every direction: a row written before the
     * column existed, a value from a newer build, or anything hand-edited. Falling
     * back to DESKTOP is the one answer that always leaves something on screen.
     */
    @Test
    void anUnreadableStoredModeFallsBackToTheDesktop() {
        assertEquals(HudDisplayMode.DESKTOP, NativeHudOverlay.parseDisplayMode(null));
        assertEquals(HudDisplayMode.DESKTOP, NativeHudOverlay.parseDisplayMode(""));
        assertEquals(HudDisplayMode.DESKTOP, NativeHudOverlay.parseDisplayMode("HOLOGRAM"));
    }

    @Test
    void aStoredModeIsReadBackWhateverItsCase() {
        assertEquals(HudDisplayMode.VR, NativeHudOverlay.parseDisplayMode("VR"));
        assertEquals(HudDisplayMode.BOTH, NativeHudOverlay.parseDisplayMode("both"));
        assertEquals(HudDisplayMode.DESKTOP, NativeHudOverlay.parseDisplayMode(" Desktop "));
    }

    /**
     * The role is what tells a commander reading a log which of two overlays
     * reported something.
     */
    @Test
    void everyChildIsNamedInTheLog() {
        for (HudDisplayMode mode : HudDisplayMode.values()) {
            for (NativeHudOverlay.ChildSpec spec : NativeHudOverlay.childSpecs(BINARY, mode)) {
                assertTrue(spec.role() != null && !spec.role().isBlank(),
                        mode + " spawned an unnamed child");
            }
        }
    }
}
