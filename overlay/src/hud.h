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
#include <limits.h>

#define PROTOCOL_VERSION 1

// "No position requested." Deliberately not -1, and not any small negative
// number: a screen coordinate is legitimately negative on both platforms. A
// monitor placed left of or above the primary one starts at a negative offset,
// and a window nudged past the top or left edge sits at a negative coordinate
// on its own. Using -1 as the sentinel made every such position unstorable -
// the card came back centred with only its Y remembered - because the guard
// that applied it could not tell the commander's coordinate from "unset".
// Must match OverlayProtocol.POSITION_UNSET on the Java side.
#define HUD_POS_UNSET INT_MIN

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

/// One colour of the card, as cairo wants it: linear 0-1 per channel.
typedef struct { double r, g, b; } Rgb;

/// A colour the commander can change, and its slot in the palette.
///
/// The roles are the ones the renderer actually asks for, not every colour on
/// screen: the card's own background is set by `CFG alpha` and stays out of
/// this list, because a commander who dims the backdrop has already said what
/// they want behind the text.
///
/// Wire names are `col_<role>` in HUD_COLOR_NAMES order, so a role maps onto a
/// slot by index and the names cannot drift out of step with the enum the way a
/// switch would. Must match HudOverlayColor on the Java side.
typedef enum {
    HUD_COL_PRIMARY,     // objective title, and any row value with no state
    HUD_COL_SUCCESS,     // a row reading "good"
    HUD_COL_WARNING,     // a row reading "warn"
    HUD_COL_DANGER,      // a row reading "critical"
    HUD_COL_DISABLED,    // subtitle, row labels, the empty part of a progress bar
    HUD_COL_USER,        // the commander's own words
    HUD_COL_AI,          // the ship AI's reply
    HUD_COL_RADIO,       // station control, carriers, other commanders
    HUD_COL_COUNT
} ColorRole;

/// The shipped colour of each role, as 0xRRGGBB.
///
/// These are the defaults a commander gets before they touch anything, and the
/// app sends the whole palette on every start - so this table only decides what
/// the binary draws when it is driven by hand (test-feed.sh) or by an app too
/// old to send colours. It mirrors HudPalette.java, which is where the same
/// colours are defined for the desktop window.
#define HUD_COL_DEFAULT_PRIMARY  0xFF7100
#define HUD_COL_DEFAULT_SUCCESS  0x4FC56B
#define HUD_COL_DEFAULT_WARNING  0xFFB000
#define HUD_COL_DEFAULT_DANGER   0xD94F4F
#define HUD_COL_DEFAULT_DISABLED 0x6E4A28
#define HUD_COL_DEFAULT_USER     0x4FC56B
#define HUD_COL_DEFAULT_AI       0x72A2B4
// Radio traffic: somebody else on the channel, not the ship's own AI. Violet,
// matched to the AI colour's brightness and saturation rather than picked by eye
// - all three lanes sit near 0.61 relative luminance at ~0.36 saturation, so
// only the hue tells them apart (~134, ~196, ~274 degrees).
#define HUD_COL_DEFAULT_RADIO    0xB78CD9

/// 0xRRGGBB as an Rgb initialiser, so the defaults above read as the hex a
/// commander sees in the settings dialog rather than as three rounded decimals.
#define HUD_RGB(hex) { ((hex) >> 16 & 0xFF) / 255.0, ((hex) >> 8 & 0xFF) / 255.0, ((hex) & 0xFF) / 255.0 }

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

/// Who a conversation line is from - the only thing that decides its colour.
///
/// The values are the wire codes, and they extend the 0/1 flag this field used
/// to be: a binary built before SPK_RADIO existed reads a 2 as "true" and draws
/// the line in the AI colour, which is what it did before radio had a colour of
/// its own. So the app can send the new code to an old overlay without the
/// protocol version having to change.
typedef enum {
    SPK_COMMANDER = 0,
    SPK_AI        = 1,
    SPK_RADIO     = 2        // station control, carriers, other commanders
} Speaker;

typedef struct {
    char speaker[64];
    char text[MAX_TEXT];
    Speaker kind;
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
    int  want_x, want_y;     // requested position; HUD_POS_UNSET means "leave as is"
    VrPosition vr_position;  // VR only; the desktop shells ignore it

    /// Desktop only; the VR shell ignores both, the mirror of vr_position above.
    /// A VR card is already placed in the world and rotated to face the
    /// commander, so shearing it as well would fight that placement rather than
    /// add to it - and the whole point of the effect is to sit in the plane of a
    /// monitor, which a headset does not have.
    double tilt;             // shear at the screen edge; 0 disables the effect
    int    tilt_width;       // logical card width while tilted, see hud_tilt.c

    /// Text colours, indexed by ColorRole. Set by `CFG col_<role>=RRGGBB`; the
    /// app sends the whole palette on start, so a commander's choices survive a
    /// restart of either side.
    Rgb palette[HUD_COL_COUNT];

    /// Draw for a capture tool rather than for the commander's own eyes.
    ///
    /// Set once from --capture and never by the protocol, because it is a
    /// property of who is looking at the window, not of the card. It turns off
    /// the two things the desktop overlay does BECAUSE it is seen on a monitor:
    /// the lean, which is a perspective trick that only works in the plane of a
    /// screen, and the see-through background, which a capture tool composites
    /// against black or against whatever is behind the window. Both are wrong
    /// once the window is a texture in a headset.
    int capture;
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

/// Fills exactly the card's own rectangle at the configured alpha.
///
/// The difference from hud_paint_background matters only once a shell draws
/// through a transform: cairo_paint covers the whole clip, which after a shear is
/// the whole bounding box rather than the card, so the sheared corners would come
/// out filled instead of transparent. Filling the card's rectangle through the
/// same transform yields the parallelogram for free.
void hud_paint_panel(cairo_t *cr, int width, int height);

// -- desktop geometry --------------------------------------------------------
//
// Leaning the card so it sits like a cockpit panel rather than flat on the glass.
// Lives outside the shells because both of them need identical geometry, and
// outside hud_render because the renderer must not know: it draws the same card
// it always did, into a context that happens to be sheared, which is what keeps
// the typewriter, the colours and every glyph exactly as they are. Text stays
// vector-crisp because Pango rasterises through the transform rather than being
// warped after the fact.

/// The shear for a card centred at (`card_center_x`, `card_center_y`) on a screen
/// of `screen_width` x `screen_height`.
///
/// BOTH axes matter and the vertical one is not decoration: measured off a real
/// cockpit, the lean reverses across eye level, so a card high on the right leans
/// the opposite way to one low on the right. See hud_tilt.c for the numbers.
/// Returns 0 when the effect is off, and along either centre line.
double hud_tilt_slope(int card_center_x, int card_center_y,
                      int screen_width, int screen_height);

/// How tall a `width` x `height` card becomes once sheared. The width is
/// unchanged - the shear is purely vertical - so no matching width call exists.
int hud_tilt_height(double slope, int width, int height);

/// Shears `cr` in place, ready for hud_paint_panel and hud_render. A slope of 0
/// leaves the context alone, so shells need no branch of their own.
void hud_tilt_apply(cairo_t *cr, double slope, int width);

/// The card's logical width right now: the configured width, CAPPED at
/// `tilt_width` while the lean is on.
///
/// A cap rather than an override, so a commander who chose a narrower card still
/// gets it; only a card too wide to stay legible at an angle is pulled in, and
/// that is reported once. Desktop shells measure and render at this, and it is
/// deliberately not `model.width` directly, because in BOTH mode the VR child is
/// fed the very same CFG lines and must keep its own full width.
int hud_card_width(void);

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
