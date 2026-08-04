// SteamVR shell for the HUD overlay.
//
// Not a window. An OpenVR overlay is a texture the SteamVR compositor draws
// inside the headset, and in VR that is the only thing a commander can see: the
// game renders straight to the HMD, so a top-most desktop window is not behind
// the game, it is outside the frame path entirely. This is why the desktop
// shells cannot be made to work in VR by changing window styles.
//
// SteamVR is also the widest net available. Every external VR overlay on PC -
// this one, OVRToolkit, OVRdrop, XSOverlay - is a SteamVR overlay, so anything
// running through SteamVR is covered (Index, Vive, Pimax, Varjo, Bigscreen, and
// Quest over Link/Air Link/Virtual Desktop/Steam Link). A commander whose game
// runs on a non-SteamVR runtime cannot be reached by any overlay app at all, and
// gets the desktop overlay instead of nothing.
//
// openvr_api is loaded at RUNTIME and never linked. A machine with no SteamVR
// must behave exactly as it did before VR support existed, and an import-table
// dependency on a DLL that is not installed would turn a missing runtime into a
// failure to start the overlay at all. Every path here ends in a VrResult the
// dispatcher can fall back from.

// readlink() and PATH_MAX are POSIX, and -std=c11 hides both behind
// __STRICT_ANSI__ unless a feature-test macro asks for them first.
#ifndef _WIN32
#define _POSIX_C_SOURCE 200809L
#endif

// We resolve every entry point ourselves, so nothing here may be declared
// dllimport - that expands to `extern "C"`, which does not compile as C.
#define OPENVR_API_NODLL

#include "hud.h"
#include "openvr_capi.h"

#include <math.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
typedef HMODULE DynLib;
#define DYN_OPEN(name)     LoadLibraryA(name)
#define DYN_CLOSE(lib)     FreeLibrary(lib)
// Through uintptr_t because FARPROC is a function pointer and gcc's
// -Wcast-function-type rejects casting it straight to void *.
#define DYN_SYM(lib, sym)  ((void *) (uintptr_t) GetProcAddress(lib, sym))
#else
#include <dlfcn.h>
#include <limits.h>
#include <time.h>
#include <unistd.h>
typedef void *DynLib;
#define DYN_OPEN(name)     dlopen(name, RTLD_LAZY | RTLD_LOCAL)
#define DYN_CLOSE(lib)     dlclose(lib)
#define DYN_SYM(lib, sym)  dlsym(lib, sym)
#endif

/// Milliseconds from an arbitrary origin, never going backwards. Only ever used
/// for differences, so the origin does not matter - but a wall clock would, and
/// a commander's clock moves when their timezone or NTP does.
static uint64_t now_ms(void) {
#ifdef _WIN32
    return (uint64_t) GetTickCount64();
#else
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return (uint64_t) t.tv_sec * 1000u + (uint64_t) (t.tv_nsec / 1000000);
#endif
}

// Placement. The card sits in the world and does NOT follow the head: a panel
// welded to your gaze is unreadable, because the eye never gets to settle on it
// and you cannot look away from it either. Anything worn on the face has to be
// glanced at, which means it has to hold still while you turn to it. See
// place_overlay for what "the world" is measured from.
//
// Which way the card sits from centre is the commander's to choose (CFG vrpos);
// how far off centre that is, is not. These two angles are the whole geometry:
// the card hangs on a sphere of HUD_DISTANCE_M around the seated origin, turned
// to face it, so every placement is the same size and the same distance away
// and only the direction changes.
//
// 30 and 18 degrees put the card's near edge just outside the middle of the
// view at its default width - clear of what the commander is aiming at, still
// inside a glance. Smaller and it sits over the reticle; larger and it needs a
// head turn rather than a glance.
#define HUD_DISTANCE_M   1.5f
#define HUD_YAW_DEG     30.0f
#define HUD_PITCH_DEG   18.0f
#define HUD_WIDTH_M      1.0f

// Texture allocation is rounded up to a multiple of this, and the unused tail
// is cropped off with texture bounds. See canvas_alloc_for.
#define TEXTURE_STEP_PX  128
#define TEXTURE_MAX_PX  4096

// How long to wait between attempts to attach to SteamVR, and how finely that
// wait is sliced so stdin keeps draining and the typewriter keeps its rhythm.
#define ATTACH_RETRY_MS  2000
#define WAIT_SLICE_MS    100

// How long to wait for the compositor to take the previous frame before drawing
// the next one. Not a frame budget - a ceiling, so a stalled compositor cannot
// stop us draining stdin. A headset frame is 8-14ms, so this is never reached
// while SteamVR is healthy.
#define FRAME_WAIT_MS      50

// How often the card is re-sent even though nothing about it changed.
//
// WHY at all: every other write here is edge-triggered - a texture is uploaded
// because a line arrived, a crop is sent because the height moved. Edge-
// triggered state has no way back if a single write is lost, and a lost write
// here is not a dropped frame, it is a card that stays on the last thing it
// managed to show while the app happily goes on talking to it. This is the
// level trigger underneath: whatever the compositor missed, it gets again
// within two seconds. It costs one upload per two seconds while idle.
//
// Measured from the last re-assert and NOT from the last frame drawn: tying it
// to the last frame meant a busy conversation, which is the one time any of
// this matters, reset the clock on every character and never re-asserted at all.
#define REASSERT_MS      2000

// How often the CROP is re-sent though it has not moved, as opposed to the
// texture above.
//
// Split off from REASSERT_MS because the two are not the same risk. A texture
// the compositor missed is the whole card frozen on an old reply, and costs one
// upload to put right. A crop it missed is the card at a slightly wrong height,
// and every crop write is already checked and re-sent from canvas->content on
// the next upload that gets through - so its level trigger is covering a case we
// have never actually seen, while sitting on the one beat a commander reported
// as a flicker. Rarer, because the principle stands (an edge-triggered write to
// the compositor gets a level trigger under it) but the rent it pays should
// match what it insures.
#define BOUNDS_REASSERT_MS 30000

// How long the compositor must refuse frames without a break before the overlay
// is thrown away and rebuilt. Long enough that a hiccup or a headset waking up
// never triggers it; short enough that a commander does not sit looking at a
// dead card. See reopen_overlay.
#define STALL_RECOVER_MS 5000

// The ceiling that wait doubles up to. A rebuild is a card that visibly vanishes
// and comes back, so a cause this cannot fix - which is every cause we have not
// thought of - must not cost a blink every five seconds for a whole session.
// Backing off turns "flickering" into "one blink a minute", while still coming
// back on its own the moment the compositor starts taking frames again.
#define STALL_RECOVER_MAX_MS 60000

// -- the openvr entry points we use ------------------------------------------

typedef bool (*FnIsRuntimeInstalled)(void);
typedef bool (*FnIsHmdPresent)(void);
typedef intptr_t (*FnInitInternal)(EVRInitError *error, EVRApplicationType type);
typedef void (*FnShutdownInternal)(void);
typedef intptr_t (*FnGetGenericInterface)(const char *version, EVRInitError *error);
typedef const char *(*FnInitErrorDescription)(EVRInitError error);

typedef struct {
    DynLib lib;
    FnIsRuntimeInstalled   is_runtime_installed;
    FnIsHmdPresent         is_hmd_present;
    FnInitInternal         init;
    FnShutdownInternal     shutdown;
    FnGetGenericInterface  get_interface;
    FnInitErrorDescription describe_error;      // optional: logging only
} OpenVr;

/// How one attempt to run as a SteamVR overlay ended.
typedef enum {
    SESSION_ENDED,      // the app said QUIT, or closed the pipe: we are done
    SESSION_NO_SERVER,  // SteamVR is not running (yet)
    SESSION_LOST,       // SteamVR was running and went away
    SESSION_FAILED      // SteamVR is there and would not give us an overlay
} SessionOutcome;

#ifndef _WIN32
/// Fills buf with "<directory of this binary>/<name>", or returns 0 if the path
/// cannot be resolved.
///
/// WHY: the Linux binary is built with no rpath, so a plain dlopen("libopenvr_
/// api.so") searches the system loader path and never finds the copy shipped
/// next to the overlay in distribution/overlays/. Windows needs no equivalent -
/// LoadLibrary searches the executable's own directory first.
static int beside_binary(const char *name, char *buf, size_t size) {
    char exe[PATH_MAX];
    ssize_t len = readlink("/proc/self/exe", exe, sizeof(exe) - 1);
    if (len <= 0) return 0;
    exe[len] = '\0';

    char *slash = strrchr(exe, '/');
    if (!slash) return 0;
    *slash = '\0';

    int written = snprintf(buf, size, "%s/%s", exe, name);
    return written > 0 && (size_t) written < size;
}
#endif

/// Opens whichever copy of openvr_api this machine has, or NULL when it has
/// none - which is the ordinary case for a flat-screen commander, not an error.
static DynLib open_openvr(void) {
#ifdef _WIN32
    // Our own copy ships beside the binary, and the executable's directory is
    // searched before anything else, so this finds ours before any SteamVR or
    // third-party copy already on the machine.
    return DYN_OPEN("openvr_api.dll");
#else
    char path[PATH_MAX];
    if (beside_binary("libopenvr_api.so", path, sizeof(path))) {
        DynLib lib = DYN_OPEN(path);
        if (lib) return lib;
    }
    // A system-wide install (distro package, or SteamVR's own copy already on
    // the loader path) is the fallback.
    return DYN_OPEN("libopenvr_api.so");
#endif
}

/// Resolves everything we call. A library that loads but is missing any of these
/// is not an openvr_api we can use - an unrelated file of the same name, or one
/// old enough to matter - and is treated as no VR at all rather than risked.
static int load_openvr(OpenVr *vr) {
    memset(vr, 0, sizeof(*vr));

    vr->lib = open_openvr();
    if (!vr->lib) return 0;

    vr->is_runtime_installed = (FnIsRuntimeInstalled) DYN_SYM(vr->lib, "VR_IsRuntimeInstalled");
    vr->is_hmd_present       = (FnIsHmdPresent) DYN_SYM(vr->lib, "VR_IsHmdPresent");
    vr->init                 = (FnInitInternal) DYN_SYM(vr->lib, "VR_InitInternal");
    vr->shutdown             = (FnShutdownInternal) DYN_SYM(vr->lib, "VR_ShutdownInternal");
    vr->get_interface        = (FnGetGenericInterface) DYN_SYM(vr->lib, "VR_GetGenericInterface");
    vr->describe_error       = (FnInitErrorDescription) DYN_SYM(vr->lib, "VR_GetVRInitErrorAsEnglishDescription");

    if (vr->is_runtime_installed && vr->is_hmd_present
        && vr->init && vr->shutdown && vr->get_interface) {
        return 1;
    }

    // Said out loud because the fallback reason the commander otherwise sees is
    // "runtime not installed", which would send them looking for a SteamVR
    // install that is already there.
    fprintf(stderr, "overlay: openvr_api loaded but is missing entry points; ignoring it\n");
    DYN_CLOSE(vr->lib);
    vr->lib = NULL;
    return 0;
}

/// Why a session did not end up drawing, in words the app can show a commander.
static const char *outcome_reason(SessionOutcome outcome) {
    switch (outcome) {
        case SESSION_NO_SERVER: return "SteamVR is not running";
        case SESSION_LOST:      return "SteamVR closed";
        default:                return "SteamVR would not start an overlay";
    }
}

static const char *describe(const OpenVr *vr, EVRInitError error) {
    if (!vr->describe_error) return "unknown error";
    const char *text = vr->describe_error(error);
    return text ? text : "unknown error";
}

/// Names an overlay error, for the one line a commander is asked to send back.
static const char *describe_overlay(struct VR_IVROverlay_FnTable *overlay, EVROverlayError error) {
    if (!overlay->GetOverlayErrorNameFromEnum) return "unknown overlay error";
    const char *text = overlay->GetOverlayErrorNameFromEnum(error);
    return text ? text : "unknown overlay error";
}

// -- drawing -----------------------------------------------------------------

/// The cairo surface the HUD is drawn into, plus the RGBA copy handed to
/// SteamVR. Kept together because they must always be the same size.
///
/// `content` is how much of that surface the card actually fills; the rest is
/// cropped away by the overlay's texture bounds and never seen. See
/// canvas_alloc_for for why the two are not the same number.
typedef struct {
    cairo_surface_t *surface;
    unsigned char   *rgba;
    int width, height;
    int content;
} Canvas;

static void canvas_free(Canvas *canvas) {
    if (canvas->surface) cairo_surface_destroy(canvas->surface);
    free(canvas->rgba);
    canvas->surface = NULL;
    canvas->rgba = NULL;
    canvas->width = canvas->height = canvas->content = 0;
}

static int canvas_resize(Canvas *canvas, int width, int height) {
    if (canvas->surface && canvas->width == width && canvas->height == height) return 1;

    // Said out loud because THIS is the event that makes the compositor throw its
    // backing texture away, and a commander sees that as the card blinking. With
    // the allocation grow-only (see canvas_alloc_for) a healthy session prints a
    // handful of these and then goes quiet; a session still blinking every few
    // seconds will print one per blink, which tells us in one glance whether the
    // remaining flicker is this or something else entirely.
    int old_width = canvas->width, old_height = canvas->height;

    canvas_free(canvas);
    canvas->surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width, height);
    if (cairo_surface_status(canvas->surface) != CAIRO_STATUS_SUCCESS) return 0;

    // calloc, not malloc: only the rows the card actually fills are converted
    // into this buffer (see to_rgba), and the tail below them is handed to
    // SteamVR untouched on the very first upload. Zeroed, that tail is
    // transparent; uninitialised, it is whatever was last in that heap page.
    canvas->rgba = calloc((size_t) width * (size_t) height, 4);
    if (!canvas->rgba) return 0;

    canvas->width = width;
    canvas->height = height;
    // Nothing has been drawn into the new surface yet, and SteamVR is still
    // cropping to the old one, so the bounds must be re-sent whatever they were.
    canvas->content = 0;

    if (old_height) fprintf(stderr, "overlay: HUD texture %dx%d -> %dx%d\n",
                            old_width, old_height, width, height);
    else            fprintf(stderr, "overlay: HUD texture %dx%d\n", width, height);
    return 1;
}

/// Rounds a content height up to the texture height we allocate for it, and
/// never returns less than we have already allocated.
///
/// Handing SetOverlayRaw a new width or height makes the compositor throw the
/// backing texture away and build another one, and a commander sees that as the
/// card blinking. Rounding to a step was half the answer: it stopped a reply
/// growing one row taller from resizing anything. It could not stop the card
/// SHRINKING back, and a conversation does nothing else - the third line pushes
/// the oldest one out, an objective row comes and goes, a long reply is replaced
/// by a short one - so the same step boundary was crossed downwards and upwards
/// again every few seconds, which is exactly the cadence this was reported at.
///
/// So the allocation only ever grows. After the first minute of a session it
/// stops changing altogether and every later frame is a texture update into a
/// texture the compositor already has, plus a crop. The cost is holding the
/// tallest buffer the session has needed; the saving is that only the rows the
/// card fills are ever converted into it (see to_rgba), so a card that shrinks
/// costs less work per frame, not more.
static int canvas_alloc_for(const Canvas *canvas, int content) {
    if (content < 1) content = 1;
    if (content > TEXTURE_MAX_PX) content = TEXTURE_MAX_PX;
    int steps = (content + TEXTURE_STEP_PX - 1) / TEXTURE_STEP_PX;
    int wanted = steps * TEXTURE_STEP_PX;
    return wanted > canvas->height ? wanted : canvas->height;
}

static unsigned char unpremultiply(unsigned value, unsigned alpha) {
    unsigned result = (value * 255u + alpha / 2) / alpha;
    return (unsigned char) (result > 255u ? 255u : result);
}

/// Converts the first `rows` rows of cairo's premultiplied BGRA into the
/// straight RGBA SetOverlayRaw expects, and blanks the rest.
///
/// Skipping the un-premultiply would darken every anti-aliased edge, which on a
/// HUD that is mostly text means every glyph gets a dirty outline. Rows are
/// copied through the surface stride rather than assuming it is width * 4,
/// because cairo is free to pad rows and does on some widths.
///
/// Only `rows` of them because the texture is now allocated for the tallest card
/// the session has shown (see canvas_alloc_for) while the crop shows only the
/// current one: converting the cropped-away tail would be a per-pixel divide per
/// pixel nobody can see. It is blanked rather than left alone so that no frame
/// where the compositor holds a taller crop than we meant - the one window where
/// the tail is visible at all - can show a band of an older reply.
static void to_rgba(const Canvas *canvas, int rows) {
    const unsigned char *src = cairo_image_surface_get_data(canvas->surface);
    int stride = cairo_image_surface_get_stride(canvas->surface);
    unsigned char *dst = canvas->rgba;

    if (rows > canvas->height) rows = canvas->height;

    for (int y = 0; y < rows; y++) {
        const unsigned char *row = src + (size_t) y * (size_t) stride;
        for (int x = 0; x < canvas->width; x++) {
            unsigned blue = row[0], green = row[1], red = row[2], alpha = row[3];
            if (alpha == 0) {
                dst[0] = dst[1] = dst[2] = dst[3] = 0;
            } else if (alpha == 255) {
                dst[0] = (unsigned char) red;
                dst[1] = (unsigned char) green;
                dst[2] = (unsigned char) blue;
                dst[3] = 255;
            } else {
                dst[0] = unpremultiply(red, alpha);
                dst[1] = unpremultiply(green, alpha);
                dst[2] = unpremultiply(blue, alpha);
                dst[3] = (unsigned char) alpha;
            }
            row += 4;
            dst += 4;
        }
    }

    if (rows < canvas->height) {
        memset(dst, 0, (size_t) (canvas->height - rows) * (size_t) canvas->width * 4);
    }
}

/// Draws the current model and hands the frame to SteamVR.
///
/// Returns 0 only if a buffer could not be allocated, which is fatal to the
/// session. Whether the COMPOSITOR took the frame is a different question and
/// comes back in *submitted - see the caller for why that is not folded into
/// the return value.
///
/// `reassert_bounds` re-sends the crop even when it has not moved, which is how a
/// crop the compositor has somehow lost gets itself back. It runs on its own slow
/// beat rather than the texture's; see BOUNDS_REASSERT_MS.
static int present(struct VR_IVROverlay_FnTable *overlay, VROverlayHandle_t handle,
                   Canvas *canvas, int reassert_bounds, EVROverlayError *submitted) {
    int width = model.width > 0 ? model.width : 760;

    // A surface of the right width has to exist before anything can be measured
    // into it; its height at this point does not matter.
    if (!canvas_resize(canvas, width, canvas->height > 0 ? canvas->height : TEXTURE_STEP_PX)) return 0;

    // Measure first, then size to the content, exactly as the desktop shells do
    // - the card grows and shrinks as objectives come and go.
    cairo_t *measure = cairo_create(canvas->surface);
    int content = hud_render(measure, width, 0);
    cairo_destroy(measure);
    if (content < 1) content = 1;
    if (content > TEXTURE_MAX_PX) content = TEXTURE_MAX_PX;
    if (!canvas_resize(canvas, width, canvas_alloc_for(canvas, content))) return 0;

    cairo_t *cr = cairo_create(canvas->surface);
    hud_paint_background(cr);
    hud_render(cr, canvas->width, 1);
    cairo_destroy(cr);
    cairo_surface_flush(canvas->surface);

    // Texture first, crop second, and never the other way round. The two
    // together are one frame: the crop is meaningless except as a description of
    // the texture it is cropping. Sending the crop first meant that between the
    // two calls the compositor held a NEW crop against the OLD texture and drew
    // the wrong slice of the wrong bitmap - one frame of a card that jumps or
    // shows a band of the previous reply. Paced against WaitFrameSync (see the
    // caller) both land in the same compositor frame and there is no between.
    to_rgba(canvas, content);
    EVROverlayError sent = overlay->SetOverlayRaw(
            handle, canvas->rgba, (uint32_t) canvas->width, (uint32_t) canvas->height, 4);

    if (sent == EVROverlayError_VROverlayError_None) {
        // Only when it moves, or when we are re-asserting: the crop decides the
        // card's shape in the world, so re-sending the same one every frame is
        // work the compositor does not need.
        if (content != canvas->content || reassert_bounds) {
            struct VRTextureBounds_t bounds = {
                0.0f, 0.0f, 1.0f, (float) content / (float) canvas->height};
            sent = overlay->SetOverlayTextureBounds(handle, &bounds);
            // Committed only once it has been taken. Recording it either way is
            // how the original bug worked: the "unchanged, so skip it" test just
            // above would then skip re-sending a crop the compositor never got,
            // and the card would sit at a stale height with nothing to correct
            // it. Zero on refusal, so the next upload carries the crop with it.
            canvas->content = sent == EVROverlayError_VROverlayError_None ? content : 0;
        }
    } else {
        // The compositor still holds the previous texture, so the crop it holds
        // is still the right one FOR THAT texture. Forget that we ever committed
        // this height, so the crop is re-sent alongside the first upload that
        // gets through rather than being skipped as unchanged.
        canvas->content = 0;
    }

    *submitted = sent;
    return 1;
}

// -- placement ---------------------------------------------------------------

/// Turns a placement into the yaw and pitch that carry it there, in radians.
/// Yaw is positive to the commander's right, pitch positive upward.
static void angles_for(VrPosition position, float *yaw, float *pitch) {
    // Spelled out rather than taken from math.h: M_PI is not in C11, and both
    // this file's _POSIX_C_SOURCE and MinGW's headers hide it by default.
    const float pi = 3.14159265358979323846f;
    const float y = HUD_YAW_DEG * pi / 180.0f;
    const float p = HUD_PITCH_DEG * pi / 180.0f;
    switch (position) {
        case HUD_VR_TOP:          *yaw =  0; *pitch =  p; break;
        case HUD_VR_TOP_RIGHT:    *yaw =  y; *pitch =  p; break;
        case HUD_VR_RIGHT:        *yaw =  y; *pitch =  0; break;
        case HUD_VR_BOTTOM_RIGHT: *yaw =  y; *pitch = -p; break;
        case HUD_VR_BOTTOM_LEFT:  *yaw = -y; *pitch = -p; break;
        case HUD_VR_LEFT:         *yaw = -y; *pitch =  0; break;
        case HUD_VR_TOP_LEFT:     *yaw = -y; *pitch =  p; break;
        default:                  *yaw =  0; *pitch = -p; break;   // HUD_VR_BOTTOM
    }
}

/// Hangs the card at `position` and points it back at the commander.
///
/// The transform is expressed in the SEATED universe, so SteamVR's "Reset
/// Seated Position" - and the game's own recentre, which goes through the same
/// call - is what decides where "ahead" is. That is the one control a commander
/// already has, already knows, and can reach with the headset ON; anything in
/// the app's settings window cannot be clicked from inside VR. It also survives
/// a restart for free: the seated origin is SteamVR's to remember, so there is
/// nothing here to persist.
///
/// The card is rotated to face the seated origin rather than left square to the
/// world, because a panel 30 degrees off to one side and still facing straight
/// ahead is read at an angle, and text is the first thing that costs.
static void place_overlay(struct VR_IVROverlay_FnTable *overlay,
                          VROverlayHandle_t handle, VrPosition position) {
    float yaw, pitch;
    angles_for(position, &yaw, &pitch);

    float s = sinf(yaw), c = cosf(yaw);
    float S = sinf(pitch), C = cosf(pitch);

    // Rotation is Ry(-yaw) * Rx(pitch), whose third column - the card's own
    // outward normal - comes out as the exact opposite of the direction it sits
    // in, which is what "facing the commander" means here. OpenVR is
    // right-handed with -Z forward, so the forward term is negated.
    struct HmdMatrix34_t place = {{
        {c, -s * S, -s * C,  HUD_DISTANCE_M * s * C},
        {0,      C,     -S,  HUD_DISTANCE_M * S},
        {s,  c * S,  c * C, -HUD_DISTANCE_M * c * C},
    }};
    overlay->SetOverlayTransformAbsolute(
            handle, ETrackingUniverseOrigin_TrackingUniverseSeated, &place);
}

// -- session -----------------------------------------------------------------

static void *interface_table(const OpenVr *vr, const char *version) {
    char name[128];
    snprintf(name, sizeof(name), "FnTable:%s", version);
    EVRInitError error = EVRInitError_VRInitError_None;
    void *table = (void *) vr->get_interface(name, &error);
    return error == EVRInitError_VRInitError_None ? table : NULL;
}

/// True once SteamVR has told us it is shutting down. Acknowledged so the
/// runtime does not sit waiting on us before it exits.
static int steamvr_is_quitting(struct VR_IVRSystem_FnTable *system) {
    struct VREvent_t event;
    while (system->PollNextEvent(&event, sizeof(event))) {
        if (event.eventType == EVREventType_VREvent_Quit) {
            system->AcknowledgeQuit_Exiting();
            return 1;
        }
    }
    return 0;
}

/// Records a change in whether the runtime is drawing our card, saying where the
/// answer came from.
///
/// Logged because this flag decides both whether we ask to be shown again and
/// whether a refused frame counts as a stall, so a log showing rebuilds without
/// showing what visibility was doing cannot be read. Transitions only - they are
/// human-paced, one per dashboard open or headset doff - and `how` distinguishes
/// the event stream from the poll that corrects it, which is the difference
/// between "the commander opened the dashboard" and "we were wrong".
static void set_visible(int *visible, int now_visible, const char *how) {
    if (*visible == now_visible) return;
    *visible = now_visible;
    fprintf(stderr, "overlay: SteamVR %s the HUD (%s)\n", now_visible ? "shows" : "hides", how);
}

/// Asks the runtime outright whether our card is being drawn, and corrects the
/// event-tracked answer with it.
///
/// WHY, when the events already say: because they only say it once. `visible` was
/// a flag rebuilt from a stream of edges, so a single OverlayShown that never
/// arrived - or arrived before we owned the handle to hear it on - left it stuck
/// at 0 for the rest of the session. Stuck at 0 means the reassert below asks to
/// be shown every two seconds forever, against a card that was never hidden, and
/// a re-show is not free to look at. One direct question on the same beat makes
/// that state unreachable instead of merely unlikely.
static void poll_visibility(struct VR_IVROverlay_FnTable *overlay,
                            VROverlayHandle_t handle, int *visible) {
    if (!overlay->IsOverlayVisible) return;         // older runtime: events only
    set_visible(visible, overlay->IsOverlayVisible(handle) ? 1 : 0, "poll");
}

/// Drains the overlay's OWN event queue, keeping `visible` current, and reports
/// whether SteamVR asked us to stop.
///
/// WHY this exists: holding an overlay handle means the runtime queues events
/// against it - shown, hidden, focus, dashboard, mouse, standby - whether or not
/// anyone ever reads them. Nothing here read them, so the queue only ever grew,
/// and an overlay whose events are never collected falls progressively further
/// behind and then stops updating altogether. Restarting the overlay emptied it
/// and bought another while, which is exactly the shape this was reported in.
/// Taking the headset off to use the desktop is what fills the queue fastest,
/// which is exactly when it was reported to stop. Draining is not bookkeeping,
/// it is the rent on the handle.
///
/// The IVRSystem queue drained by steamvr_is_quitting is a DIFFERENT queue and
/// does not cover this one.
static int drain_overlay_events(struct VR_IVROverlay_FnTable *overlay,
                                VROverlayHandle_t handle, int *visible) {
    struct VREvent_t event;
    int quitting = 0;

    while (overlay->PollNextOverlayEvent(handle, &event, sizeof(event))) {
        switch (event.eventType) {
            case EVREventType_VREvent_OverlayShown:  set_visible(visible, 1, "event"); break;
            case EVREventType_VREvent_OverlayHidden: set_visible(visible, 0, "event"); break;
            case EVREventType_VREvent_Quit:          quitting = 1; break;
            default: break;                 // discarded, but READ, which is the point
        }
    }
    return quitting;
}

/// Advances the typewriter by however many characters the clock owes it, and
/// reports whether anything moved. `typed_through` is the moment the last
/// character was due, and is carried across calls.
///
/// WHY the clock and not one character per pass: this loop's period is not the
/// typewriter's and never was. It waits on stdin, and now waits for a compositor
/// frame as well, so a character per pass reveals text at whatever rate the
/// headset happens to run at. In BOTH mode that is visible: the desktop child
/// types on a 25ms timer of its own, and the two are meant to be showing the
/// same sentence at the same moment, not the same sentence at two speeds.
static int advance_typewriter(uint64_t *typed_through) {
    uint64_t now = now_ms();
    int moved = 0;

    while (now - *typed_through >= TYPEWRITER_MS) {
        if (!hud_tick_typewriter()) {
            // Fully revealed. Start the clock again from here so the pause
            // before the next line does not bank characters that would then be
            // spent all at once the moment it arrives.
            *typed_through = now;
            break;
        }
        *typed_through += TYPEWRITER_MS;
        moved = 1;
    }
    return moved;
}

/// Creates the overlay and puts it where it belongs. Shared by the first attach
/// and by the stall recovery, so the two can never drift apart.
static int open_overlay(struct VR_IVROverlay_FnTable *overlay, VROverlayHandle_t *handle,
                        VrPosition position) {
    char key[] = "elite.intel.hud";
    char title[] = "EliteIntel HUD";

    if (overlay->CreateOverlay(key, title, handle) != EVROverlayError_VROverlayError_None) return 0;
    overlay->SetOverlayWidthInMeters(*handle, HUD_WIDTH_M);
    place_overlay(overlay, *handle, position);
    overlay->ShowOverlay(*handle);
    return 1;
}

/// Throws the overlay away and builds another one.
///
/// This is the automated form of the workaround commanders found for themselves
/// when the HUD went dead mid-session: close it, open it again, and it behaves
/// for a while. It is a BACKSTOP and no longer the answer to that bug - the
/// cause was found (an overlay event queue nobody drained; see
/// drain_overlay_events) and is fixed at the cause. What is left here is cover
/// for the same symptom arriving by some route we have not diagnosed, because a
/// card that is simply dead until somebody notices is the worst outcome
/// available and worth a blink to avoid.
///
/// That ordering is why the thresholds in note_submission are free to be
/// conservative: making a rebuild rarer now risks a slower recovery from an
/// unknown bug, not a return of the known one.
static int reopen_overlay(struct VR_IVROverlay_FnTable *overlay, VROverlayHandle_t *handle,
                          VrPosition position, Canvas *canvas) {
    overlay->DestroyOverlay(*handle);
    *handle = 0;
    if (!open_overlay(overlay, handle, position)) return 0;

    // A brand new overlay holds no texture and no crop, so nothing about the old
    // one may be carried over as still committed.
    canvas->content = 0;
    return 1;
}

/// How the compositor has been treating our frames lately.
///
/// Two clocks, because "refused while we are being drawn" and "refused at all"
/// need different answers. See note_submission.
typedef struct {
    uint64_t refusing_since;    // run of refusals WHILE VISIBLE, 0 if none
    uint64_t refusing_ever;     // run of refusals whatever the visibility, 0 if none
    int      reported;          // the current run has already been reported once
    uint64_t recover_after;     // how long this run must refuse to earn a rebuild
} FrameHealth;

static void health_reset(FrameHealth *health) {
    health->refusing_since = 0;
    health->refusing_ever = 0;
    health->reported = 0;
    health->recover_after = STALL_RECOVER_MS;
}

/// Doubles the wait a rebuild has to earn, up to the ceiling. Called after each
/// rebuild, cleared by health_reset the moment a frame gets through, so a run of
/// rebuilds that is fixing nothing gets rarer while a genuine one-off stall
/// costs the next stall nothing.
static void health_backoff(FrameHealth *health) {
    health->recover_after *= 2;
    if (health->recover_after > STALL_RECOVER_MAX_MS) health->recover_after = STALL_RECOVER_MAX_MS;
}

/// Records how a submission went and says whether the overlay needs rebuilding.
///
/// Sets *dirty on a refusal so the frame is drawn again rather than dropped: the
/// model has already moved on, and a dropped frame is a card that is quietly one
/// reply behind for good.
///
/// `visible` is what keeps this honest. A refused frame is only evidence of a
/// stall while the runtime is actually drawing us; when it is not - the dashboard
/// is up, the headset is in standby or off the commander's head - refusing our
/// frames is the correct behaviour and not a fault to recover from. Counting
/// those was the bug: the rebuild below cannot fix a hidden overlay, so it fired,
/// achieved nothing, fired again five seconds later, and went on doing that for
/// as long as the dashboard was open. Every one of those rebuilds is a card that
/// vanishes and comes back, which is what a commander sees as the HUD restarting
/// itself every few seconds.
static int note_submission(struct VR_IVROverlay_FnTable *overlay, EVROverlayError submitted,
                           FrameHealth *health, int visible, int *dirty) {
    if (submitted == EVROverlayError_VROverlayError_None) {
        if (health->reported) hud_report_mode("vr", NULL);
        health_reset(health);
        return 0;
    }

    uint64_t now = now_ms();
    if (!health->refusing_since) health->refusing_since = now;
    if (!health->refusing_ever) health->refusing_ever = now;
    if (!health->reported) {
        health->reported = 1;
        // Said once per run, not once per frame: a stalled compositor refuses
        // forty times a second and the log is the only place this can be seen.
        fprintf(stderr, "overlay: SteamVR refused the HUD frame: %s\n",
                describe_overlay(overlay, submitted));
        hud_report_mode("vr", describe_overlay(overlay, submitted));
    }
    *dirty = 1;

    if (!visible) {
        // Stopped rather than paused: a commander coming back from ten minutes
        // in the dashboard must not have those ten minutes counted against them
        // and land on an instant rebuild the moment the card is shown again.
        health->refusing_since = 0;

        // The valve, and the reason the other clock exists. Rebuilding is how
        // the overlay recovers from going dead altogether - the failure this
        // whole path was written for - and that failure is a runtime we are no
        // longer getting sane answers from. If it also stopped delivering our
        // OverlayShown, `visible` is stuck at 0 and the test above would have
        // quietly disarmed the only recovery there is. So visibility may DELAY a
        // rebuild, never veto one: a full minute of nothing getting through
        // earns one whatever the runtime claims about who can see us.
        return now - health->refusing_ever >= STALL_RECOVER_MAX_MS;
    }
    return now - health->refusing_since >= health->recover_after;
}

/// Runs as a SteamVR overlay until the app quits or SteamVR goes away.
///
/// `announce` is cleared on repeat attempts so a commander who never starts
/// SteamVR does not collect the same complaint every two seconds for a session.
static SessionOutcome run_session(const OpenVr *vr, int announce) {
    // Attaching as a Background app first is how "SteamVR is not running" is
    // told apart from "SteamVR refused us": a Background init fails immediately
    // with NoServerForBackgroundApp instead of starting the runtime. That
    // distinction is the point - a commander who chose BOTH and is playing flat
    // tonight must never have SteamVR launched at them by an overlay.
    EVRInitError error = EVRInitError_VRInitError_None;
    vr->init(&error, EVRApplicationType_VRApplication_Background);
    vr->shutdown();
    if (error == EVRInitError_VRInitError_Init_NoServerForBackgroundApp) return SESSION_NO_SERVER;

    error = EVRInitError_VRInitError_None;
    vr->init(&error, EVRApplicationType_VRApplication_Overlay);
    if (error != EVRInitError_VRInitError_None) {
        if (announce) fprintf(stderr, "overlay: SteamVR init failed: %s\n", describe(vr, error));
        vr->shutdown();
        return SESSION_FAILED;
    }

    struct VR_IVROverlay_FnTable *overlay = interface_table(vr, IVROverlay_Version);
    if (!overlay) {
        // Almost always a SteamVR older than the SDK this was built against.
        if (announce) fprintf(stderr, "overlay: SteamVR has no %s; update SteamVR\n", IVROverlay_Version);
        vr->shutdown();
        return SESSION_FAILED;
    }
    // Optional: without it we simply do not learn that SteamVR is closing, and
    // find out when the pipe or the compositor goes quiet instead. Said out loud
    // because it also means one of the two event queues stops being drained, and
    // an undrained queue is the failure this file has already been bitten by.
    struct VR_IVRSystem_FnTable *system = interface_table(vr, IVRSystem_Version);
    if (!system && announce) {
        fprintf(stderr, "overlay: SteamVR has no %s; shutdown will not be seen\n", IVRSystem_Version);
    }

    VrPosition placed = model.vr_position;
    VROverlayHandle_t handle = 0;
    if (!open_overlay(overlay, &handle, placed)) {
        if (announce) fprintf(stderr, "overlay: SteamVR would not create the overlay\n");
        vr->shutdown();
        return SESSION_FAILED;
    }
    hud_report_mode("vr", NULL);
    // The one line that says the VR shell is actually up, and the two facts about
    // the runtime that change how it behaves: which overlay interface we got, and
    // whether frame pacing exists at all. WaitFrameSync is optional and missing on
    // older SteamVR, which silently drops us back to submitting whenever we feel
    // like it - the exact condition the shimmering-while-typing fix depends on -
    // and there would otherwise be no way to tell that from the outside.
    fprintf(stderr, "overlay: attached to SteamVR as %s (frame pacing %s)\n",
            IVROverlay_Version, overlay->WaitFrameSync ? "available" : "UNAVAILABLE");

    Canvas canvas = {0};
    FrameHealth health;
    health_reset(&health);
    SessionOutcome outcome = SESSION_ENDED;
    int quit = 0, eof = 0, dirty = 1, visible = 1;
    uint64_t typed_through = now_ms();
    uint64_t last_reassert = now_ms();
    uint64_t last_bounds_reassert = now_ms();

    while (!quit && !eof) {
        if (hud_pump_stdin(TYPEWRITER_MS, &eof, &quit)) dirty = 1;
        if (advance_typewriter(&typed_through)) dirty = 1;
        if (system && steamvr_is_quitting(system)) { outcome = SESSION_LOST; break; }
        // Every pass, unconditionally. See drain_overlay_events for what an
        // undrained overlay queue costs.
        if (drain_overlay_events(overlay, handle, &visible)) { outcome = SESSION_LOST; break; }

        // Moving the card takes effect while the commander is wearing the
        // headset, so they can try placements against the cockpit they are
        // actually flying rather than restart the overlay to see each one.
        if (model.vr_position != placed) {
            placed = model.vr_position;
            place_overlay(overlay, handle, placed);
        }

        int reassert = now_ms() - last_reassert >= REASSERT_MS;
        if (reassert) {
            last_reassert = now_ms();
            // Asked before it is acted on, so the decision below is made against
            // what the runtime says right now rather than against whatever the
            // event stream last managed to tell us.
            poll_visibility(overlay, handle, &visible);
            // ShowOverlay at attach was an edge-triggered write like the rest.
            // If the runtime hid the card and we missed why, say again that we
            // want it - but only while it agrees the card is hidden, so this can
            // neither argue with a hide the runtime means to keep nor hammer a
            // card that was visible all along.
            if (!visible) overlay->ShowOverlay(handle);
        }

        int reassert_bounds = now_ms() - last_bounds_reassert >= BOUNDS_REASSERT_MS;
        if (reassert_bounds) last_bounds_reassert = now_ms();

        if (dirty || reassert) {
            // Pace to the compositor rather than to the typewriter. Left to
            // itself this loop redraws every time a character appears - forty
            // full textures a second, handed over with no idea whether the
            // compositor had finished with the last one. Waiting for the frame
            // makes each upload land in exactly one compositor frame, which is
            // what stops the card shimmering while the AI types, and it is also
            // what makes the texture and its crop below arrive together.
            if (overlay->WaitFrameSync) overlay->WaitFrameSync(FRAME_WAIT_MS);

            EVROverlayError submitted = EVROverlayError_VROverlayError_None;
            if (!present(overlay, handle, &canvas, reassert_bounds, &submitted)) {
                fprintf(stderr, "overlay: out of memory drawing the VR overlay\n");
                outcome = SESSION_FAILED;
                break;
            }
            dirty = 0;

            // A refused upload leaves the commander looking at the last frame
            // that got through while the app carries on narrating into it, so
            // the card sits quietly a conversation behind. It used to be silent
            // because nothing read this result.
            if (note_submission(overlay, submitted, &health, visible, &dirty)) {
                // From refusing_ever, not refusing_since: the visibility clock is
                // deliberately zeroed while the runtime says nobody can see us,
                // so reading it here would print the time since the epoch on
                // exactly the rebuild that most needs explaining.
                unsigned long stalled = (unsigned long) (now_ms() - health.refusing_ever);
                int blind = !visible;
                hud_report_mode("vr", "rebuilding the overlay after a stall");
                if (!reopen_overlay(overlay, &handle, placed, &canvas)) {
                    fprintf(stderr, "overlay: SteamVR would not rebuild the overlay\n");
                    outcome = SESSION_FAILED;
                    break;
                }
                // The run is cleared but the backoff is NOT: only a frame the
                // compositor actually takes proves the rebuild worked, and that
                // clears it in note_submission. Clearing it here would make every
                // rebuild look like the first one and put us straight back on a
                // five-second beat.
                health.refusing_since = 0;
                health.refusing_ever = 0;
                health.reported = 0;
                health_backoff(&health);
                // Every number here matters to whoever reads this log: how long
                // the compositor had been refusing says whether this was a real
                // stall or a threshold set too low, whether we were hidden says
                // which of the two clocks fired, and the next threshold says
                // whether the backoff is doing its job when these repeat.
                fprintf(stderr, "overlay: rebuilt the HUD after %lums of refused frames%s;"
                                " next rebuild needs %lums\n",
                        stalled, blind ? " while hidden" : "",
                        (unsigned long) health.recover_after);
                // A handle that was just created and shown is shown. Assumed
                // rather than waited for, so a runtime that does not send us
                // OverlayShown cannot leave stall detection switched off.
                visible = 1;
            }
        }
    }

    canvas_free(&canvas);
    overlay->DestroyOverlay(handle);
    vr->shutdown();
    return outcome;
}

/// Waits out one retry interval, still draining stdin and still animating, so
/// the model is current the moment SteamVR appears. Returns 0 when the app has
/// gone away and there is nothing left to wait for.
static int wait_for_steamvr(void) {
    for (int waited = 0; waited < ATTACH_RETRY_MS; waited += WAIT_SLICE_MS) {
        int eof = 0, quit = 0;
        hud_pump_stdin(WAIT_SLICE_MS, &eof, &quit);
        hud_tick_typewriter();
        if (eof || quit) return 0;
    }
    return 1;
}

VrResult hud_run_vr(VrMode mode) {
    OpenVr vr;
    if (!load_openvr(&vr)) return HUD_VR_UNAVAILABLE;

    VrResult result = HUD_VR_UNAVAILABLE;
    if (!vr.is_runtime_installed()) goto done;

    // AUTO requires a headset to actually be connected, so a commander who owns
    // one but is playing flat tonight keeps the desktop overlay and never has to
    // know the setting exists. The other modes are explicit choices, and accept
    // a headset SteamVR has not woken up yet.
    if (mode == VR_MODE_AUTO && !vr.is_hmd_present()) goto done;

    // NOT checked here: VR_IsInterfaceVersionValid. It asks the *runtime*, so it
    // answers no whenever SteamVR is merely not running yet - which would turn
    // the ordinary "app started before SteamVR" case into a hard version error
    // instead of the retry below. The interface version is settled where it can
    // actually be known, when the table is requested after a successful init.
    // ONLY is the VR half of the "both at once" setting, so a desktop overlay is
    // already on screen and retrying costs the commander nothing: SteamVR
    // started after the app, or restarted mid-session, both end with the HUD
    // appearing in the headset on its own. Every other mode is the only overlay
    // there is, so waiting would mean showing nothing at all - those fall back.
    //
    // Deliberately retried on ANY outcome but ENDED, rather than only on the
    // ones that look transient. Which error a runtime reports for "not running
    // yet" is not something this code can know for every SteamVR version, and
    // guessing wrong would strand the commander with no VR overlay for the whole
    // session. Retrying a genuinely permanent failure costs one init attempt
    // every two seconds and says so once.
    int announce = 1;
    for (;;) {
        SessionOutcome outcome = run_session(&vr, announce);
        if (outcome == SESSION_ENDED) {
            result = HUD_VR_OK;
            goto done;
        }
        if (mode != VR_MODE_ONLY) {
            result = outcome == SESSION_FAILED ? HUD_VR_FAILED : HUD_VR_UNAVAILABLE;
            goto done;
        }
        // Said once, on the first failure, so the app can show "waiting for
        // SteamVR" rather than leaving the commander to guess whether the VR
        // half of their overlay is coming. Attaching later reports "vr".
        if (announce) hud_report_mode("waiting", outcome_reason(outcome));
        if (!wait_for_steamvr()) {
            result = HUD_VR_OK;            // the app closed the pipe while we waited
            goto done;
        }
        // Attaching once and losing SteamVR later is worth reporting again; a
        // run of identical failures is not.
        announce = outcome == SESSION_LOST;
    }

done:
    DYN_CLOSE(vr.lib);
    return result;
}
