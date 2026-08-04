// Desktop card geometry: leaning the card like a cockpit panel.
//
// Elite draws its own side panels on surfaces angled in toward the pilot, and the
// idea here is that the overlay should sit in that world rather than flat on the
// glass in front of it. See hud.h for why this is neither in the shells (both
// need identical geometry) nor in hud_render (the renderer must not know).
//
// The transform is a pure VERTICAL SHEAR, not a rotation and not a perspective
// projection. Cairo's matrix is affine, so genuine foreshortening - a trapezoid
// with a shorter far edge - is not available without warping the finished pixels,
// which would cost a resampling pass and, far worse, soften every glyph. A shear
// keeps text rasterised through the transform at full quality, and at the angles
// worth using the missing foreshortening is not what the eye notices.
//
// WHAT IT COSTS, measured before any of this was written: the shear moves a row's
// far end down (or up) by slope * width. Once that exceeds one line of text, a
// label on the left and its right-aligned value stop reading as the same row and
// the card becomes unreadable - the rows visually interleave. That product, not
// the angle, is the real constraint, which is why a leaning card is also a
// narrower one. At the shipped defaults that is 0.15 * 620 = 93px against a 25px
// row pitch, so a label and its right-aligned value are nearly four rows apart at
// full lean. That is a known cost of the angle asked for, not an oversight: it
// cannot be tuned away, only designed away, by giving the objective rows a
// narrower column than the card or dropping the right-alignment entirely.

#include "hud.h"

#include <math.h>
#include <stdio.h>

int hud_card_width(void) {
    int configured = model.width > 0 ? model.width : 760;
    if (model.tilt == 0.0 || model.tilt_width <= 0) return configured;

    // A CAP, not a replacement. tilt_width used to win outright, which quietly
    // made the commander's own width setting - stored in the database, sent on
    // every connect - do nothing at all whenever the lean was on. A setting that
    // is accepted, persisted and ignored is a bug report waiting to happen.
    //
    // A cap is the honest form of the constraint, because the constraint really
    // is one-sided: a row's far end shears by slope * width, so a card WIDER than
    // this stops reading as rows, while a NARROWER one the commander chose on
    // purpose is fine and is now honoured.
    if (configured <= model.tilt_width) return configured;

    // Said once per distinct clamp, and only on the desktop path since the VR
    // shell never calls this. Without it the cap is exactly the silent override
    // it replaced; with it, "I set 760 and got 620" is answerable from the log.
    static int announced_for = -1;
    if (announced_for != configured) {
        announced_for = configured;
        fprintf(stderr, "overlay: leaning caps the HUD at %dpx (configured %dpx);"
                        " CFG tilt=0 restores the full width\n",
                model.tilt_width, configured);
    }
    return model.tilt_width;
}

/// -1 at one edge of the screen, 0 dead centre, +1 at the other, ramped so the
/// full value is reached at TILT_FULL_AT of the way out rather than only at the
/// very edge.
///
/// 0.60 rather than the 0.25 first tried. The full angle belongs to the CORNERS -
/// that is what "NW/SW/SE/NE = angle, N/S/E/W = flat" means - and at 0.25 on a
/// 3440-wide screen the ramp saturated 430px off centre, so a card barely north
/// of west already had the whole corner lean on it and the effect snapped between
/// flat and full instead of growing into it. At 0.60 the lean builds across most
/// of the quadrant and only reaches full out near the corner itself.
///
/// Clamped because a card dragged half off screen, or onto a monitor the shell
/// measured nothing about, must not out-lean any real cockpit panel.
static double axis(int card_center, int extent) {
    const double TILT_FULL_AT = 0.60;
    double half = extent / 2.0;
    double n = (card_center - half) / half / TILT_FULL_AT;
    if (n < -1.0) return -1.0;
    if (n >  1.0) return  1.0;
    return n;
}

/// Names the quadrant a normalised position falls in, for the diagnostic below.
static const char *quadrant(double nx, double ny) {
    const char *v = ny < -0.05 ? "N" : (ny > 0.05 ? "S" : "");
    const char *h = nx < -0.05 ? "W" : (nx > 0.05 ? "E" : "");
    if (!*v && !*h) return "centre";
    if (!*v) return h;
    if (!*h) return v;
    return ny < 0 ? (nx < 0 ? "NW" : "NE") : (nx < 0 ? "SW" : "SE");
}

double hud_tilt_slope(int card_center_x, int card_center_y,
                      int screen_width, int screen_height) {
    if (model.tilt == 0.0 || screen_width <= 0 || screen_height <= 0) return 0.0;

    // The PRODUCT of both offsets, not the horizontal one alone. Measured off a
    // real cockpit, the lean reverses across eye level: on the right of the
    // screen Elite's upper HUD text rises to the right (-0.17 to -0.20) while the
    // lower dash panel of the same cockpit falls to the right (+0.08 to +0.11),
    // passing through nearly flat at mid height (-0.04). The lower LEFT panel
    // rises to the right (-0.05 to -0.08), mirroring the lower right.
    //
    // That is what a cockpit wrapping around the pilot does: a flat panel tangent
    // to a sphere picks up an in-plane rotation proportional to how far off centre
    // it is in BOTH axes, and vanishes along either axis through the middle. A
    // rule using only horizontal position gets two of the four quadrants
    // backwards, which is exactly how it looked.
    //
    // Sign check against all four measurements, with y positive downward:
    //   left  + below  ->  (-)(+) = -  rises right   matches lower-left panel
    //   right + below  ->  (+)(+) = +  falls right   matches lower-right panel
    //   right + above  ->  (+)(-) = -  rises right   matches upper-right text
    //   left  + above  ->  (-)(-) = +  falls right   the untested quadrant
    double nx = axis(card_center_x, screen_width);
    double ny = axis(card_center_y, screen_height);

    // A consequence worth knowing rather than hiding: a card parked on either
    // centre line comes out flat however far along the other it sits. That is the
    // cockpit being honest - dead ahead has no rotation in it - and it makes the
    // lean something a commander tunes by dragging, which is what the effect was
    // asked for in the first place.
    double slope = model.tilt * nx * ny;

    // Said out loud, edge-triggered on a real change, because "the angle is
    // wrong" and "the card is not in the quadrant you think it is" look exactly
    // alike from a screenshot and are fixed in completely different places. This
    // reports which one it is: the screen the shell measured, where the card's
    // centre landed on it, and the lean that came out. One line per move, and
    // silence while the card sits still.
    static double reported = -999.0;
    if (slope < reported - 0.004 || slope > reported + 0.004) {
        reported = slope;
        fprintf(stderr, "overlay: HUD at (%d,%d) of %dx%d -> %s  nx=%+.2f ny=%+.2f slope=%+.3f\n",
                card_center_x, card_center_y, screen_width, screen_height,
                quadrant(nx, ny), nx, ny, slope);
    }
    return slope;
}

int hud_tilt_height(double slope, int width, int height) {
    double drop = fabs(slope * (double) width);
    return height + (int) (drop + 0.5);
}

void hud_tilt_apply(cairo_t *cr, double slope, int width) {
    double drop = slope * (double) width;

    // A negative slope lifts the right edge above y=0, so the whole card is
    // pushed down by exactly as much as it rose. Without this the top corner is
    // clipped away by the surface edge.
    if (drop < 0.0) cairo_translate(cr, 0.0, -drop);

    // x is untouched (xx=1, xy=0); only y gains a term proportional to x. That is
    // the entire effect.
    cairo_matrix_t m;
    cairo_matrix_init(&m, 1.0, slope, 0.0, 1.0, 0.0, 0.0);
    cairo_transform(cr, &m);
}
