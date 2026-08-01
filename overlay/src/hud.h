// Platform-agnostic core of the EliteIntel HUD overlay.
//
// The model, the stdin protocol and all drawing live here and are shared by
// every platform shell. A shell owns only: creating a translucent always-on-top
// window, pumping its event loop, dragging, and presenting a finished ARGB
// buffer. Everything a commander actually sees is drawn by hud_render().

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

typedef struct {
    Objective obj;
    Objective staging;       // filled by OBJ/ROW/BAR, committed by END
    Line lines[MAX_LINES];
    int  line_count;
    double alpha;            // background alpha; text is always opaque
    double scale;
    int  width;
    int  want_x, want_y;     // requested position; -1 means "leave as is"
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

#endif
