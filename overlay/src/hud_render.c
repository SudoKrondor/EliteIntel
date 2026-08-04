// Drawing for the HUD overlay: objective card on top, conversation below.
// Platform-agnostic; produces pixels into any cairo context the shell supplies.

#include "hud.h"

#include <pango/pangocairo.h>
#include <stdio.h>
#include <string.h>

// -- palette (mirrors HudPalette roles; see ED_HUD_REFERENCE.md) -------------

typedef struct { double r, g, b; } Rgb;

static const Rgb COL_PRIMARY  = {1.00, 0.44, 0.00};
static const Rgb COL_SUCCESS  = {0.31, 0.77, 0.42};
static const Rgb COL_WARNING  = {1.00, 0.69, 0.00};
static const Rgb COL_DANGER   = {0.85, 0.31, 0.31};
static const Rgb COL_DISABLED = {0.43, 0.29, 0.16};
static const Rgb COL_USER     = {0.31, 0.77, 0.42};
static const Rgb COL_AI       = {0.45, 0.64, 0.71};
static const Rgb COL_PANEL    = {0.06, 0.09, 0.13};

static int sz(int base) { return (int) (base * model.scale + 0.5); }

static void set_rgb(cairo_t *cr, Rgb c, double a) {
    cairo_set_source_rgba(cr, c.r, c.g, c.b, a);
}

static Rgb color_for(State s) {
    switch (s) {
        case ST_GOOD:     return COL_SUCCESS;
        case ST_WARN:     return COL_WARNING;
        case ST_CRITICAL: return COL_DANGER;
        default:          return COL_PRIMARY;
    }
}

void hud_paint_background(cairo_t *cr) {
    // SOURCE, not OVER: replace the buffer outright. The background carries the
    // configured alpha; glyphs are drawn opaque on top.
    cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
    cairo_set_source_rgba(cr, COL_PANEL.r, COL_PANEL.g, COL_PANEL.b, model.alpha);
    cairo_paint(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_OVER);
}

void hud_paint_panel(cairo_t *cr, int width, int height) {
    // Same colour and the same replace-don't-blend rule as above, confined to the
    // card. See hud.h for why a transformed shell cannot use cairo_paint.
    cairo_save(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
    cairo_set_source_rgba(cr, COL_PANEL.r, COL_PANEL.g, COL_PANEL.b, model.alpha);
    cairo_rectangle(cr, 0, 0, width, height);
    cairo_fill(cr);
    cairo_restore(cr);
}

// -- layout / rendering ------------------------------------------------------

static PangoLayout *make_layout(cairo_t *cr, int size, int bold) {
    PangoLayout *l = pango_cairo_create_layout(cr);
    char desc[64];
    snprintf(desc, sizeof(desc), "Sans %s%d", bold ? "Bold " : "", sz(size));
    PangoFontDescription *fd = pango_font_description_from_string(desc);
    pango_layout_set_font_description(l, fd);
    pango_font_description_free(fd);
    return l;
}

static int layout_height(PangoLayout *l, const char *s) {
    int w, h;
    pango_layout_set_text(l, s, -1);
    pango_layout_get_pixel_size(l, &w, &h);
    return h;
}

static void draw_at(cairo_t *cr, PangoLayout *l, double x, double y, const char *s, Rgb c) {
    pango_layout_set_text(l, s, -1);
    set_rgb(cr, c, 1.0);          // text is always fully opaque
    cairo_move_to(cr, x, y);
    pango_cairo_show_layout(cr, l);
}

static void draw_right(cairo_t *cr, PangoLayout *l, double right, double y, const char *s, Rgb c) {
    int w, h;
    pango_layout_set_text(l, s, -1);
    pango_layout_get_pixel_size(l, &w, &h);
    set_rgb(cr, c, 1.0);
    cairo_move_to(cr, right - w, y);
    pango_cairo_show_layout(cr, l);
}

/// Renders the whole frame and returns the height the content needs, so the
/// caller can size the window to it. Pass draw=0 to measure only.
int hud_render(cairo_t *cr, int width, int draw) {
    int pad = sz(14);
    int y = pad;

    PangoLayout *title = make_layout(cr, 15, 1);
    PangoLayout *body  = make_layout(cr, 12, 0);
    int title_h = layout_height(title, "Xy");
    int body_h  = layout_height(body, "Xy");
    int right   = width - pad;

    if (model.obj.present) {
        if (draw) draw_at(cr, title, pad, y, model.obj.title, COL_PRIMARY);
        y += title_h;

        if (model.obj.subtitle[0]) {
            if (draw) draw_at(cr, body, pad, y, model.obj.subtitle, COL_DISABLED);
            y += body_h + sz(4);
        }

        for (int i = 0; i < model.obj.row_count; i++) {
            Row *r = &model.obj.rows[i];
            if (draw) draw_at(cr, body, pad, y, r->label, COL_DISABLED);

            if (r->max > 0) {
                char readout[64];
                snprintf(readout, sizeof(readout), "%d / %d", r->current, r->max);
                if (draw) {
                    draw_right(cr, body, right, y, readout, color_for(r->state));

                    int tw, th;
                    pango_layout_set_text(body, readout, -1);
                    pango_layout_get_pixel_size(body, &tw, &th);
                    double bx = pad + sz(150);
                    double bw = right - bx - tw - sz(12);
                    if (bw > 0) {
                        double bh = sz(7), by = y + body_h * 0.5 - bh * 0.5;
                        set_rgb(cr, COL_DISABLED, 1.0);
                        cairo_rectangle(cr, bx, by, bw, bh);
                        cairo_fill(cr);
                        double frac = r->current / (double) r->max;
                        if (frac < 0) frac = 0;
                        if (frac > 1) frac = 1;
                        set_rgb(cr, color_for(r->state), 1.0);
                        cairo_rectangle(cr, bx, by, bw * frac, bh);
                        cairo_fill(cr);
                    }
                }
            } else if (r->value[0] && draw) {
                draw_right(cr, body, right, y, r->value, color_for(r->state));
            }
            y += body_h + sz(2);
        }

        y += sz(6);
        if (draw) {
            set_rgb(cr, COL_DISABLED, 0.6);
            cairo_set_line_width(cr, 1.0);
            cairo_move_to(cr, pad, y + 0.5);
            cairo_line_to(cr, right, y + 0.5);
            cairo_stroke(cr);
        }
        y += sz(6);
    }

    // Conversation. Wrapping is Pango's job, so long replies fold correctly in
    // every script rather than being cut.
    pango_layout_set_width(body, (width - pad * 2) * PANGO_SCALE);
    pango_layout_set_wrap(body, PANGO_WRAP_WORD_CHAR);
    for (int i = 0; i < model.line_count; i++) {
        Line *l = &model.lines[i];
        char buf[MAX_TEXT + 96];
        int tw, th;

        // Height is measured against the WHOLE line, never the part the
        // typewriter has revealed so far, so the card is its final size from the
        // first character and the text fills into space already reserved.
        //
        // Measuring the visible prefix instead grows the card every time a new
        // character wraps onto a new row - once a second or so while the AI
        // "types". On the desktop that is a window that jitters; in VR it is a
        // new overlay texture of a new size handed to the compositor mid-
        // sentence, which is the flicker a commander sees in the headset.
        snprintf(buf, sizeof(buf), "%s: %s", l->speaker, l->text);
        pango_layout_set_text(body, buf, -1);
        pango_layout_get_pixel_size(body, &tw, &th);

        if (draw) {
            snprintf(buf, sizeof(buf), "%s: %.*s", l->speaker, l->visible_bytes, l->text);
            pango_layout_set_text(body, buf, -1);
            set_rgb(cr, l->ai ? COL_AI : COL_USER, 1.0);
            cairo_move_to(cr, pad, y);
            pango_cairo_show_layout(cr, body);
        }
        y += th;
    }
    pango_layout_set_width(body, -1);

    g_object_unref(title);
    g_object_unref(body);
    return y + pad;
}

