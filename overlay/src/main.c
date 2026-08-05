// Entry point and shell selection for the HUD overlay.
//
// One rule governs this file: a commander with no headset must get exactly the
// overlay they had before VR existed. So the desktop shell is the default, VR
// is opt-in, and every VR failure - no runtime, no headset, a broken SteamVR -
// falls back to the desktop shell rather than leaving the commander with no
// overlay at all. There is no path here that ends in nothing on screen.

#include "hud.h"

#include <stdio.h>
#include <string.h>

/// Unknown arguments are ignored, not rejected, the same way the protocol
/// reader ignores lines it does not understand: the app and the binary ship
/// together but can be a version apart after a partial update, and an overlay
/// that refuses to start over an unrecognised flag is worse than one that
/// starts in its default mode.
static VrMode parse_vr_mode(int argc, char **argv) {
    VrMode mode = VR_MODE_OFF;
    for (int i = 1; i < argc; i++) {
        if      (!strcmp(argv[i], "--vr"))      mode = VR_MODE_ON;
        else if (!strcmp(argv[i], "--vr=on"))   mode = VR_MODE_ON;
        else if (!strcmp(argv[i], "--vr=auto")) mode = VR_MODE_AUTO;
        else if (!strcmp(argv[i], "--vr=only")) mode = VR_MODE_ONLY;
        else if (!strcmp(argv[i], "--vr=off"))  mode = VR_MODE_OFF;
    }
    return mode;
}

/// Whether to draw for a VR capture tool instead of for the commander's monitor.
///
/// A mode of its own rather than a variant of --vr because it is not VR as far
/// as this binary is concerned: there is no SteamVR, no compositor and no
/// headset here, just a window drawn so that something else can pin it. Which
/// also means it never probes SteamVR and never falls back - it is a desktop
/// window by construction, and cannot fail the way an overlay can.
static int parse_capture(int argc, char **argv) {
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "--capture")) return 1;
    }
    return 0;
}

static const char *fallback_reason(VrMode mode, VrResult result) {
    switch (result) {
        case HUD_VR_UNAVAILABLE:
            return mode == VR_MODE_AUTO ? "no headset detected"
                                        : "SteamVR runtime not installed";
        case HUD_VR_NOT_BUILT:
            return "VR shell not built in this version";
        default:
            return "SteamVR overlay failed to start";
    }
}

int main(int argc, char **argv) {
    VrMode mode = parse_vr_mode(argc, argv);
    const char *reason = NULL;

    // Read before anything draws, and never touched again: the shells and the
    // renderer branch on it while building their first frame.
    model.capture = parse_capture(argc, argv);
    if (model.capture) {
        hud_report_mode("capture", NULL);
        return hud_run_desktop(argc, argv);
    }

    if (mode != VR_MODE_OFF) {
        VrResult result = hud_run_vr(mode);
        if (result == HUD_VR_OK) return 0;

        reason = fallback_reason(mode, result);

        // ONLY exists for the "both at once" setting, where the app runs a
        // desktop overlay AND a VR one as two children. That child must never
        // fall back: a second desktop window would land exactly on top of the
        // first one, and the commander would drag one and watch the other stay
        // put. Better to exit and let the desktop child be the whole overlay.
        if (mode == VR_MODE_ONLY) {
            fprintf(stderr, "overlay: %s; --vr=only, so exiting rather than opening a window\n", reason);
            hud_report_mode("none", reason);
            return 3;
        }

        // stderr is discarded by the parent process, so this line is for a
        // commander running the binary by hand to find out why. The app learns
        // the same thing from the MODE line below, which is on stdout.
        fprintf(stderr, "overlay: %s; falling back to the desktop overlay\n", reason);
    }

    // Reported before the shell runs, not after: hud_run_desktop only returns
    // when the overlay is closing.
    hud_report_mode("desktop", reason);
    return hud_run_desktop(argc, argv);
}
