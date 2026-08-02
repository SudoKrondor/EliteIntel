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
#include <unistd.h>
typedef void *DynLib;
#define DYN_OPEN(name)     dlopen(name, RTLD_LAZY | RTLD_LOCAL)
#define DYN_CLOSE(lib)     dlclose(lib)
#define DYN_SYM(lib, sym)  dlsym(lib, sym)
#endif

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

    canvas_free(canvas);
    canvas->surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width, height);
    if (cairo_surface_status(canvas->surface) != CAIRO_STATUS_SUCCESS) return 0;

    canvas->rgba = malloc((size_t) width * (size_t) height * 4);
    if (!canvas->rgba) return 0;

    canvas->width = width;
    canvas->height = height;
    // Nothing has been drawn into the new surface yet, and SteamVR is still
    // cropping to the old one, so the bounds must be re-sent whatever they were.
    canvas->content = 0;
    return 1;
}

/// Rounds a content height up to the texture height we allocate for it.
///
/// Handing SetOverlayRaw a new width or height makes the compositor throw the
/// backing texture away and build another one, and a commander sees that as the
/// card blinking. Rounding to a step means an ordinary change - a reply one row
/// taller, a mining row appearing - reuses the texture it already has and only
/// moves the crop, so the size the compositor sees changes a handful of times a
/// session instead of a handful of times a sentence.
static int canvas_alloc_for(int content) {
    if (content < 1) content = 1;
    if (content > TEXTURE_MAX_PX) content = TEXTURE_MAX_PX;
    int steps = (content + TEXTURE_STEP_PX - 1) / TEXTURE_STEP_PX;
    return steps * TEXTURE_STEP_PX;
}

static unsigned char unpremultiply(unsigned value, unsigned alpha) {
    unsigned result = (value * 255u + alpha / 2) / alpha;
    return (unsigned char) (result > 255u ? 255u : result);
}

/// Converts cairo's premultiplied BGRA into the straight RGBA SetOverlayRaw
/// expects.
///
/// Skipping the un-premultiply would darken every anti-aliased edge, which on a
/// HUD that is mostly text means every glyph gets a dirty outline. Rows are
/// copied through the surface stride rather than assuming it is width * 4,
/// because cairo is free to pad rows and does on some widths.
static void to_rgba(const Canvas *canvas) {
    const unsigned char *src = cairo_image_surface_get_data(canvas->surface);
    int stride = cairo_image_surface_get_stride(canvas->surface);
    unsigned char *dst = canvas->rgba;

    for (int y = 0; y < canvas->height; y++) {
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
}

/// Draws the current model and hands the frame to SteamVR. Returns 0 only if a
/// buffer could not be allocated, which is fatal to the session.
static int present(struct VR_IVROverlay_FnTable *overlay, VROverlayHandle_t handle, Canvas *canvas) {
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
    if (!canvas_resize(canvas, width, canvas_alloc_for(content))) return 0;

    cairo_t *cr = cairo_create(canvas->surface);
    hud_paint_background(cr);
    hud_render(cr, canvas->width, 1);
    cairo_destroy(cr);
    cairo_surface_flush(canvas->surface);

    // Crop the padding off the bottom. Only when it moves: the bounds are what
    // decide the card's shape in the world, so re-sending the same ones every
    // frame is work the compositor does not need.
    if (content != canvas->content) {
        canvas->content = content;
        struct VRTextureBounds_t bounds = {
            0.0f, 0.0f, 1.0f, (float) content / (float) canvas->height};
        overlay->SetOverlayTextureBounds(handle, &bounds);
    }

    to_rgba(canvas);
    overlay->SetOverlayRaw(handle, canvas->rgba,
                           (uint32_t) canvas->width, (uint32_t) canvas->height, 4);
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
    // find out when the pipe or the compositor goes quiet instead.
    struct VR_IVRSystem_FnTable *system = interface_table(vr, IVRSystem_Version);

    char key[] = "elite.intel.hud";
    char title[] = "EliteIntel HUD";
    VROverlayHandle_t handle = 0;
    if (overlay->CreateOverlay(key, title, &handle) != EVROverlayError_VROverlayError_None) {
        if (announce) fprintf(stderr, "overlay: SteamVR would not create the overlay\n");
        vr->shutdown();
        return SESSION_FAILED;
    }

    overlay->SetOverlayWidthInMeters(handle, HUD_WIDTH_M);
    VrPosition placed = model.vr_position;
    place_overlay(overlay, handle, placed);
    overlay->ShowOverlay(handle);
    hud_report_mode("vr", NULL);

    Canvas canvas = {0};
    SessionOutcome outcome = SESSION_ENDED;
    int quit = 0, eof = 0, dirty = 1;

    while (!quit && !eof) {
        if (hud_pump_stdin(TYPEWRITER_MS, &eof, &quit)) dirty = 1;
        if (hud_tick_typewriter()) dirty = 1;
        if (system && steamvr_is_quitting(system)) { outcome = SESSION_LOST; break; }

        // Moving the card takes effect while the commander is wearing the
        // headset, so they can try placements against the cockpit they are
        // actually flying rather than restart the overlay to see each one.
        if (model.vr_position != placed) {
            placed = model.vr_position;
            place_overlay(overlay, handle, placed);
        }

        if (dirty) {
            if (!present(overlay, handle, &canvas)) {
                fprintf(stderr, "overlay: out of memory drawing the VR overlay\n");
                outcome = SESSION_FAILED;
                break;
            }
            dirty = 0;
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
