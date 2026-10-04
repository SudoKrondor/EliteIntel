package elite.intel.ui.overlay;

import java.util.List;

/**
 * One line inside a {@link HudObjective}.
 * <p>
 * Deliberately structural rather than domain-typed: a row is a label/value
 * pair, a label with a progress bar, or a label/value pair with a track of
 * coloured stops drawn between them. The overlay renderer knows nothing about
 * missions, bio samples or trade routes - it only knows how to draw these
 * shapes, so a new kind of objective never requires a rendering change.
 *
 * @param label   left-hand caption, rendered in caps per ED_HUD_REFERENCE.md
 * @param value   right-aligned value; ignored when {@code max > 0}
 * @param current progress numerator; meaningful only when {@code max > 0}
 * @param max     progress denominator; {@code 0} means "not a progress row"
 * @param state   drives the value colour, never the layout
 * @param track   stops drawn as dots on a line between label and value, each
 *                coloured by its own state; empty for every other row
 */
public record HudRow(String label, String value, int current, int max, State state, List<State> track) {

    /**
     * The most stops a track carries. The renderer lays the stops out on a fixed
     * pitch of this many slots, so a shortening track visibly shortens instead of
     * spreading out, and drops anything past it. Must match {@code MAX_TRACK} in
     * {@code overlay/src/hud.h}.
     */
    public static final int MAX_TRACK = 7;

    public HudRow {
        track = track == null ? List.of() : List.copyOf(track.subList(0, Math.min(track.size(), MAX_TRACK)));
    }

    /**
     * Semantic state of a row's value, mapped to a HudPalette role at paint time.
     */
    public enum State {
        /**
         * Normal working value - the HUD's primary colour.
         */
        NORMAL,
        /**
         * Positive/complete - green.
         */
        GOOD,
        /**
         * Needs attention - amber.
         */
        WARN,
        /**
         * Failing or expired - red.
         */
        CRITICAL
    }

    /**
     * A plain label/value row in the normal state.
     */
    public static HudRow of(String label, String value) {
        return new HudRow(label, value, 0, 0, State.NORMAL, null);
    }

    /**
     * A label/value row carrying a semantic state.
     */
    public static HudRow of(String label, String value, State state) {
        return new HudRow(label, value, 0, 0, state, null);
    }

    /**
     * A progress row. The renderer draws a bar plus a "current / max" readout,
     * so callers never format the numbers themselves.
     */
    public static HudRow progress(String label, int current, int max) {
        return new HudRow(label, null, current, max, State.NORMAL, null);
    }

    /**
     * A progress row carrying a semantic state.
     */
    public static HudRow progress(String label, int current, int max, State state) {
        return new HudRow(label, null, current, max, state, null);
    }

    /**
     * A label/value row with a track of stops drawn between the two, first stop
     * nearest the label. Stops past {@link #MAX_TRACK} are dropped.
     */
    public static HudRow track(String label, String value, List<State> stops) {
        return new HudRow(label, value, 0, 0, State.NORMAL, stops);
    }

    /**
     * True when this row carries a track of stops.
     */
    public boolean hasTrack() {
        return !track.isEmpty();
    }

    /**
     * True when this row should render as a bar rather than a value.
     */
    public boolean hasProgress() {
        return max > 0;
    }

    /**
     * Completion in {@code [0,1]}; {@code 0} for non-progress rows.
     */
    public double fraction() {
        if (max <= 0) return 0;
        return Math.max(0d, Math.min(1d, current / (double) max));
    }
}
