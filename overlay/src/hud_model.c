// Model, protocol parsing and typewriter for the HUD overlay.
// Platform-agnostic; see hud.h.

#include "hud.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

Model model = {
    .alpha = 0.25, .scale = 1.0, .width = 760, .want_x = -1, .want_y = -1,
};

// -- helpers -----------------------------------------------------------------





static State parse_state(const char *s) {
    if (!s) return ST_NORMAL;
    if (!strcmp(s, "good"))     return ST_GOOD;
    if (!strcmp(s, "warn"))     return ST_WARN;
    if (!strcmp(s, "critical")) return ST_CRITICAL;
    return ST_NORMAL;
}

/// Advances a byte index past one whole UTF-8 character. Advancing by bytes
/// would split multi-byte glyphs and render replacement characters mid-word,
/// which matters for every non-English locale the app ships.
static int next_utf8(const char *s, int i) {
    if (!s[i]) return i;
    i++;
    while ((s[i] & 0xC0) == 0x80) i++;
    return i;
}

// -- protocol ----------------------------------------------------------------

/// Splits a tab-separated line in place. Returns the field count.
static int split_tabs(char *line, char *fields[], int max) {
    int n = 0;
    char *p = line;
    while (n < max) {
        fields[n++] = p;
        char *tab = strchr(p, '\t');
        if (!tab) break;
        *tab = '\0';
        p = tab + 1;
    }
    return n;
}

static void push_line(const char *speaker, const char *text, int ai) {
    if (model.line_count == MAX_LINES) {
        memmove(&model.lines[0], &model.lines[1], sizeof(Line) * (MAX_LINES - 1));
        model.line_count--;
    }
    Line *l = &model.lines[model.line_count++];
    snprintf(l->speaker, sizeof(l->speaker), "%s", speaker);
    snprintf(l->text, sizeof(l->text), "%s", text);
    l->ai = ai;
    l->visible_bytes = 0;
    // Any still-typing earlier line is completed, so a fast exchange never
    // strands a half-written line above the new one.
    for (int i = 0; i < model.line_count - 1; i++) {
        model.lines[i].visible_bytes = (int) strlen(model.lines[i].text);
    }
}

static void apply_cfg(char *fields[], int n) {
    for (int i = 1; i < n; i++) {
        char *eq = strchr(fields[i], '=');
        if (!eq) continue;
        *eq = '\0';
        const char *k = fields[i], *v = eq + 1;
        if      (!strcmp(k, "alpha")) model.alpha = atof(v);
        else if (!strcmp(k, "scale")) model.scale = atof(v);
        else if (!strcmp(k, "width")) model.width = atoi(v);
        else if (!strcmp(k, "x"))     model.want_x = atoi(v);
        else if (!strcmp(k, "y"))     model.want_y = atoi(v);
    }
}

/// Returns 1 when the command changed what is on screen.
int hud_handle_command(char *line, int *quit) {
    char *f[16];
    int n = split_tabs(line, f, 16);
    if (n == 0) return 0;

    if (!strcmp(f[0], "V")) {
        if (n < 2 || atoi(f[1]) != PROTOCOL_VERSION) {
            fprintf(stderr, "overlay: unsupported protocol version %s (need %d)\n",
                    n > 1 ? f[1] : "?", PROTOCOL_VERSION);
            exit(3);
        }
        return 0;
    }
    if (!strcmp(f[0], "QUIT")) { *quit = 1; return 0; }
    if (!strcmp(f[0], "CFG"))  { apply_cfg(f, n); return 1; }

    if (!strcmp(f[0], "OBJ")) {
        memset(&model.staging, 0, sizeof(model.staging));
        snprintf(model.staging.title, sizeof(model.staging.title), "%s", n > 1 ? f[1] : "");
        snprintf(model.staging.subtitle, sizeof(model.staging.subtitle), "%s", n > 2 ? f[2] : "");
        model.staging.present = 1;
        return 0;
    }
    if (!strcmp(f[0], "ROW") && model.staging.row_count < MAX_ROWS) {
        Row *r = &model.staging.rows[model.staging.row_count++];
        memset(r, 0, sizeof(*r));
        snprintf(r->label, sizeof(r->label), "%s", n > 1 ? f[1] : "");
        snprintf(r->value, sizeof(r->value), "%s", n > 2 ? f[2] : "");
        r->state = parse_state(n > 3 ? f[3] : NULL);
        return 0;
    }
    if (!strcmp(f[0], "BAR") && model.staging.row_count < MAX_ROWS) {
        Row *r = &model.staging.rows[model.staging.row_count++];
        memset(r, 0, sizeof(*r));
        snprintf(r->label, sizeof(r->label), "%s", n > 1 ? f[1] : "");
        r->current = n > 2 ? atoi(f[2]) : 0;
        r->max     = n > 3 ? atoi(f[3]) : 0;
        r->state   = parse_state(n > 4 ? f[4] : NULL);
        return 0;
    }
    if (!strcmp(f[0], "END")) {
        // Commit atomically: the card never renders half-built.
        model.obj = model.staging;
        return 1;
    }
    if (!strcmp(f[0], "CLR")) {
        memset(&model.obj, 0, sizeof(model.obj));
        return 1;
    }
    if (!strcmp(f[0], "SAY")) {
        push_line(n > 1 ? f[1] : "", n > 3 ? f[3] : "", n > 2 ? atoi(f[2]) : 0);
        return 1;
    }
    return 0;  // unknown verb: ignored on purpose, see PROTOCOL.md
}


/// Reports the window's position to the app, the one thing sent back up the
/// pipe. The window is dragged with the mouse, so the app cannot know where it
/// ended up unless it is told; without this the position is the one setting a
/// restart cannot restore. Flushed immediately because the app reads by line.
void hud_report_position(int x, int y) {
    printf("POS\t%d\t%d\n", x, y);
    fflush(stdout);
}

void hud_report_mode(const char *mode, const char *detail) {
    if (detail) printf("MODE\t%s\t%s\n", mode, detail);
    else        printf("MODE\t%s\n", mode);
    fflush(stdout);
}

/// Splits raw stdin bytes into protocol lines. Owns the line buffer so both
/// shells get identical behaviour: the buffering rule is part of the protocol,
/// not part of a window.
///
/// An over-long line is DISCARDED, not fatal and not resynced mid-line. The X11
/// shell used to exit when the buffer filled with no newline in it, so one long
/// AI reply killed the overlay; the Win32 shell instead cleared the buffer and
/// fed the tail of that line to the parser as if it were a fresh command. Both
/// were the same missing decision. The producer bounds line length, so reaching
/// this is already a bug somewhere else - drop the line and keep drawing.
int hud_feed(const char *bytes, int len, int *quit) {
    static char buf[MAX_LINE];
    static int  used = 0;
    static int  discarding = 0;
    int dirty = 0;

    for (int i = 0; i < len; i++) {
        char c = bytes[i];
        if (c == '\n') {
            if (discarding) {
                discarding = 0;
            } else {
                buf[used] = '\0';
                if (hud_handle_command(buf, quit)) dirty = 1;
            }
            used = 0;
            continue;
        }
        if (c == '\r') continue;              // tolerate CRLF from a Windows producer
        if (discarding) continue;
        if (used == MAX_LINE - 1) {
            discarding = 1;
            used = 0;
            continue;
        }
        buf[used++] = c;
    }
    return dirty;
}

/// Advances the newest line by one character. Shells call this on a timer; the
/// overlay owns the animation so it never depends on pipe timing.
int hud_tick_typewriter(void) {
    if (model.line_count == 0) return 0;
    Line *l = &model.lines[model.line_count - 1];
    int len = (int) strlen(l->text);
    if (l->visible_bytes >= len) return 0;
    l->visible_bytes = next_utf8(l->text, l->visible_bytes);
    return 1;
}
