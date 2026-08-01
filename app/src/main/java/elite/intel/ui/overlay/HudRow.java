package elite.intel.ui.overlay;

/**
 * One line inside a {@link HudObjective}.
 * <p>
 * Deliberately structural rather than domain-typed: a row is either a
 * label/value pair or a label with a progress bar. The overlay renderer knows
 * nothing about missions, bio samples or trade routes - it only knows how to
 * draw these two shapes, so a new kind of objective never requires a rendering
 * change.
 *
 * @param label   left-hand caption, rendered in caps per ED_HUD_REFERENCE.md
 * @param value   right-aligned value; ignored when {@code max > 0}
 * @param current progress numerator; meaningful only when {@code max > 0}
 * @param max     progress denominator; {@code 0} means "not a progress row"
 * @param state   drives the value colour, never the layout
 */
public record HudRow(String label, String value, int current, int max, State state) {

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
        return new HudRow(label, value, 0, 0, State.NORMAL);
    }

    /**
     * A label/value row carrying a semantic state.
     */
    public static HudRow of(String label, String value, State state) {
        return new HudRow(label, value, 0, 0, state);
    }

    /**
     * A progress row. The renderer draws a bar plus a "current / max" readout,
     * so callers never format the numbers themselves.
     */
    public static HudRow progress(String label, int current, int max) {
        return new HudRow(label, null, current, max, State.NORMAL);
    }

    /**
     * A progress row carrying a semantic state.
     */
    public static HudRow progress(String label, int current, int max, State state) {
        return new HudRow(label, null, current, max, state);
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
