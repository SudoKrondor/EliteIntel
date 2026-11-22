package elite.intel.ui.theme;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Which text in a theme is hard to read: each text role against every background it is drawn on, by the WCAG
 * contrast ratio. Below {@link #MINIMUM} the Color Themes tab warns. A warning never stops a save: the commander's
 * eyes and screen are theirs.
 * <p>
 * A role drawn see-through (the accent is) is first laid over its background, and a see-through background over the
 * application background, so the ratio is of the colours that actually reach the screen.
 * <p>
 * Disabled text is left out on purpose: WCAG exempts it, and it is meant to recede.
 */
public final class ThemeContrast {

    /** WCAG AA for body text. */
    public static final double MINIMUM = 4.5;

    /** A hair above {@link #MINIMUM}, so a fixed colour stays readable after rounding to whole channel values. */
    static final double FIXED_RATIO = MINIMUM + 0.1;

    /** A text role and one background it is drawn on. */
    public record Pair(ThemeRole text, ThemeRole background) {
    }

    /** Every text role and the backgrounds it is drawn on in the app. */
    static final List<Pair> PAIRS = pairs();

    /** A pair below {@link #MINIMUM}, with the colours that reach the screen. */
    public record Warning(ThemeRole text, ThemeRole background, double ratio, Color textColor, Color backgroundColor) {
        public Pair pair() {
            return new Pair(text, background);
        }
    }

    private ThemeContrast() {
    }

    /** The pairs below {@link #MINIMUM}, lowest ratio first. */
    public static List<Warning> check(ColorTheme theme) {
        List<Warning> warnings = new ArrayList<>();
        for (Pair pair : PAIRS) {
            Color background = shownBackground(theme, pair.background());
            Color text = opaque(theme.color(pair.text()), background);
            double ratio = ratio(text, background);
            if (ratio < MINIMUM) warnings.add(new Warning(pair.text(), pair.background(), ratio, text, background));
        }
        warnings.sort((a, b) -> Double.compare(a.ratio(), b.ratio()));
        return warnings;
    }

    /** The ratio of one pair as the screen shows it. */
    static double ratio(ColorTheme theme, Pair pair) {
        Color background = shownBackground(theme, pair.background());
        return ratio(opaque(theme.color(pair.text()), background), background);
    }

    /** A background as the screen shows it: laid over the application background when it is see-through. */
    static Color shownBackground(ColorTheme theme, ThemeRole background) {
        Color application = opaque(theme.color(ThemeRole.APPLICATION_BACKGROUND), Color.BLACK);
        return background == ThemeRole.APPLICATION_BACKGROUND ? application : opaque(theme.color(background), application);
    }

    /**
     * {@code moving} moved away from {@code fixed} - lighter if it is the lighter of the two, darker if not - only as
     * far as it must go to read at {@link #FIXED_RATIO}, so it keeps its hue as far as it can. Opaque. Empty when that
     * way cannot get there: a colour is never flipped from light to dark, which would read worse on everything else
     * it sits on or under.
     */
    static Optional<Color> apart(Color moving, Color fixed) {
        if (ratio(moving, fixed) >= FIXED_RATIO) return Optional.of(moving);
        if (ratio(farEnd(moving, fixed), fixed) < FIXED_RATIO) return Optional.empty();
        double low = 0;
        double high = 1;
        for (int step = 0; step < 24; step++) {
            double middle = (low + high) / 2;
            if (ratio(away(moving, fixed, middle), fixed) >= FIXED_RATIO) high = middle;
            else low = middle;
        }
        return Optional.of(away(moving, fixed, high));
    }

    /**
     * {@code moving} moved {@code share} of the way from itself to white (when it is the lighter) or black.
     * <p>
     * WHY white and black: they are the far ends of contrast, not colours of the app; the mix stops long before.
     */
    static Color away(Color moving, Color fixed, double share) {
        Color end = farEnd(moving, fixed);
        return new Color(
                (int) Math.round(moving.getRed() + (end.getRed() - moving.getRed()) * share),
                (int) Math.round(moving.getGreen() + (end.getGreen() - moving.getGreen()) * share),
                (int) Math.round(moving.getBlue() + (end.getBlue() - moving.getBlue()) * share));
    }

    /** The WCAG contrast ratio of two opaque colours, from 1 (none) to 21 (black on white). */
    static double ratio(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** {@code color} laid over {@code below}, as the screen shows it. */
    static Color opaque(Color color, Color below) {
        double alpha = color.getAlpha() / 255.0;
        return new Color(
                (int) Math.round(color.getRed() * alpha + below.getRed() * (1 - alpha)),
                (int) Math.round(color.getGreen() * alpha + below.getGreen() * (1 - alpha)),
                (int) Math.round(color.getBlue() * alpha + below.getBlue() * (1 - alpha)));
    }

    private static Color farEnd(Color moving, Color fixed) {
        return luminance(moving) >= luminance(fixed) ? Color.WHITE : Color.BLACK;
    }

    private static double luminance(Color color) {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /**
     * WHY these backgrounds: where each role is painted, read from the widgets that use it. Selected text sits on four
     * fills - the accent fill (selections, segments, pressed subtle buttons), the two selected-tab fills and the pressed
     * button - and checking it on the accent alone once fixed it into black on dark tabs (2026-11-14).
     */
    private static List<Pair> pairs() {
        List<Pair> pairs = new ArrayList<>();
        for (ThemeRole text : List.of(ThemeRole.PRIMARY_TEXT, ThemeRole.SECONDARY_TEXT, ThemeRole.READOUT_LABEL,
                ThemeRole.USER_INPUT_LOG_TEXT, ThemeRole.ASSISTANT_RESPONSE_LOG_TEXT, ThemeRole.SYSTEM_LOG_TEXT,
                ThemeRole.SYSTEM_LOG_TIMESTAMP_TEXT, ThemeRole.SHIP_NAME_TEXT, ThemeRole.CREDITS_TEXT,
                ThemeRole.PRIMARY_ACTION, ThemeRole.SUCCESS, ThemeRole.WARNING, ThemeRole.DANGER, ThemeRole.INFORMATION)) {
            pairs.add(new Pair(text, ThemeRole.APPLICATION_BACKGROUND));
        }
        pairs.add(new Pair(ThemeRole.VALUE_TEXT, ThemeRole.FIELD_BACKGROUND));
        pairs.add(new Pair(ThemeRole.INPUT_FOCUS, ThemeRole.FIELD_BACKGROUND));
        pairs.add(new Pair(ThemeRole.BUTTON_TEXT, ThemeRole.PRIMARY_BUTTON_BACKGROUND));
        for (ThemeRole fill : List.of(ThemeRole.ACCENT_FILL, ThemeRole.MAIN_TAB_ACTIVE_BACKGROUND,
                ThemeRole.SECTION_TAB_ACTIVE_BACKGROUND, ThemeRole.PRIMARY_BUTTON_PRESSED_BACKGROUND)) {
            pairs.add(new Pair(ThemeRole.SELECTED_TEXT, fill));
        }
        pairs.add(new Pair(ThemeRole.DIALOG_TITLE_TEXT, ThemeRole.DIALOG_HEADER_BACKGROUND));
        pairs.add(new Pair(ThemeRole.MONOSPACE_TEXT, ThemeRole.LOG_BACKGROUND));
        return pairs;
    }
}
