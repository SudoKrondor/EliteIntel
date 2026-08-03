// Platform-agnostic core of the EliteIntel HUD overlay.
//
// The model, the stdin protocol and all drawing live here and are shared by
// every platform shell. A shell owns only: creating a translucent always-on-top
// window, pumping its event loop, dragging, and presenting a finished ARGB
// buffer. Everything a commander actually sees is drawn by hud_render().
//
// There are three shells: platform_win32.c and platform_x11.c present to a
// desktop window, platform_openvr.c presents to the SteamVR compositor. Which
// one runs is decided in main.c and nowhere else.

#ifndef EI_HUD_H
#define EI_HUD_H

#include <cairo/cairo.h>

#define PROTOCOL_VERSION 1

#define MAX_ROWS   8
// Exchanges kept on screen. Lines wrap, so a chatty reply is already several
// rows tall; more than this and the transcript grows over the canopy.
#define MAX_LINES  3
#define MAX_TEXT   1024
// Longest protocol line accepted. Comfortably above anything the producer emits
// (it clamps spoken text well below MAX_TEXT), so a line reaching this is a bug
// on the other side and gets dropped rather than truncated into a half command.
#define MAX_LINE   8192
#define TYPEWRITER_MS 25

typedef enum { ST_NORMAL, ST_GOOD, ST_WARN, ST_CRITICAL } State;

typedef struct {
    char  label[128];
    char  value[128];
    int   current, max;      // max > 0 => progress bar
    State state;
} Row;

typedef struct {
    char title[256];
    char subtitle[256];
    Row  rows[MAX_ROWS];
    int  row_count;
    int  present;
} Objective;

typedef struct {
    char speaker[64];
    char text[MAX_TEXT];
    int  ai;
    int  visible_bytes;      // typewriter cursor, always on a UTF-8 boundary
} Line;

/// Where the card sits in the headset, as a point of the compass on the
/// commander's forward view - TOP is straight above centre, RIGHT is off to the
/// right at eye level. Only the VR shell reads this; a desktop window is placed
/// by dragging it.
///
/// Named for the view rather than for the compass (NORTH/EAST/...) because
/// Elite already has a compass, pointing at planetary north, and a HUD setting
/// that says NORTH would be read as pointing at that.
typedef enum {
    HUD_VR_TOP,
    HUD_VR_TOP_RIGHT,
    HUD_VR_RIGHT,
    HUD_VR_BOTTOM_RIGHT,
    HUD_VR_BOTTOM,
    HUD_VR_BOTTOM_LEFT,
    HUD_VR_LEFT,
    HUD_VR_TOP_LEFT
} VrPosition;

typedef struct {
    Objective obj;
    Objective staging;       // filled by OBJ/ROW/BAR, committed by END
    Line lines[MAX_LINES];
    int  line_count;
    double alpha;            // background alpha; text is always opaque
    double scale;
    int  width;
    int  want_x, want_y;     // requested position; -1 means "leave as is"
    VrPosition vr_position;  // VR only; the desktop shells ignore it
} Model;

extern Model model;

/// Applies one protocol line (modifies it in place). Returns 1 when the screen
/// must be redrawn. Sets *quit on QUIT.
int hud_handle_command(char *line, int *quit);

/// Feeds raw stdin bytes in, splitting them into protocol lines and applying
/// each. Returns 1 when the screen must be redrawn. Sets *quit on QUIT. Shells
/// read bytes and hand them here; they own no parsing state of their own.
int hud_feed(const char *bytes, int len, int *quit);

/// Tells the app where the window now is. Shells call this when a drag ends, so
/// the position survives a restart like every other overlay setting.
void hud_report_position(int x, int y);

/// Advances the typewriter by one character. Returns 1 when something changed.
int hud_tick_typewriter(void);

/// Draws the current model. Pass draw=0 to measure only; returns the height the
/// content needs so the shell can size the window to it.
int hud_render(cairo_t *cr, int width, int draw);

/// Fills the background at the configured alpha, replacing (not blending) the
/// buffer. Shells call this before hud_render.
void hud_paint_background(cairo_t *cr);

/// Tells the app which shell actually came up, and why, when the answer is not
/// the one it asked for. Sent once at startup; detail may be NULL.
void hud_report_mode(const char *mode, const char *detail);

/// Drains whatever is on stdin into the model, waiting at most timeout_ms for
/// something to arrive. Returns 1 when the screen must be redrawn, sets *eof
/// when the parent closed the pipe, and *quit on QUIT.
int hud_pump_stdin(int timeout_ms, int *eof, int *quit);

// -- shells ------------------------------------------------------------------

/// What the app asked for on the command line. OFF is the default and the only
/// mode that existed before VR support, so a spawn with no arguments gets
/// exactly the overlay it always got.
typedef enum {
    VR_MODE_OFF,        // desktop shell; never even look for a headset
    VR_MODE_AUTO,       // VR only when a headset is actually connected
    VR_MODE_ON,         // VR whenever the SteamVR runtime is installed
    VR_MODE_ONLY        // VR or nothing: exits rather than opening a window
} VrMode;

/// Why the VR shell did not take over. Everything except OK falls back to the
/// desktop shell: a commander who asked for VR and cannot have it still gets an
/// overlay, never a blank screen.
typedef enum {
    HUD_VR_OK,          // the VR shell ran and exited normally
    HUD_VR_UNAVAILABLE, // no SteamVR runtime, or no headset in AUTO mode
    HUD_VR_NOT_BUILT,   // runtime is there but the VR shell does not exist yet
    HUD_VR_FAILED       // runtime is there and starting the overlay failed
} VrResult;

/// Runs the desktop shell for this platform (Win32 or X11) to completion.
int hud_run_desktop(int argc, char **argv);

/// Runs the SteamVR shell to completion, or reports why it cannot.
VrResult hud_run_vr(VrMode mode);

#endif
