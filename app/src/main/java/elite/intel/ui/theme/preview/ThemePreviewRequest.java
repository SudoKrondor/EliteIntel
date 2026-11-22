package elite.intel.ui.theme.preview;

import elite.intel.i18n.Language;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the preview process is asked to draw, passed to it as command-line arguments.
 *
 * @param mode             the preview a commander sees, or the role map behind it
 * @param language         the language of the sample text, the app's own
 * @param textAntialiasing how the app on screen anti-aliases text, so the preview matches it pixel for pixel
 * @param lcdContrast      the screen's LCD text contrast, or 0 for the look and feel's default
 * @param scale            the screen's scale factor (2.0 on a 200% screen), so the preview is sharp there
 */
public record ThemePreviewRequest(Mode mode, Language language, TextAntialiasing textAntialiasing, int lcdContrast,
                                  double scale) {

    public enum Mode {PREVIEW, ROLE_MAP}

    /**
     * The text anti-aliasing hints Swing understands, by a name that survives the trip through a command line.
     * <p>
     * WHY it is passed at all: a headless process gets no desktop text hints, so without the app's own value the
     * preview's text comes out heavier than the app's.
     */
    public enum TextAntialiasing {
        DEFAULT(RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT),
        OFF(RenderingHints.VALUE_TEXT_ANTIALIAS_OFF),
        ON(RenderingHints.VALUE_TEXT_ANTIALIAS_ON),
        GASP(RenderingHints.VALUE_TEXT_ANTIALIAS_GASP),
        LCD_HRGB(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB),
        LCD_HBGR(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR),
        LCD_VRGB(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB),
        LCD_VBGR(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);

        private final Object hint;

        TextAntialiasing(Object hint) {
            this.hint = hint;
        }

        public Object hint() {
            return hint;
        }

        /** The name for a hint value as Swing holds it, DEFAULT for none or one this enum does not list. */
        public static TextAntialiasing of(Object hint) {
            return Arrays.stream(values()).filter(value -> value.hint.equals(hint)).findFirst().orElse(DEFAULT);
        }
    }

    private static final String MODE = "--mode=";
    private static final String LANGUAGE = "--language=";
    private static final String TEXT = "--text-antialiasing=";
    private static final String CONTRAST = "--lcd-contrast=";
    private static final String SCALE = "--scale=";

    public ThemePreviewRequest {
        if (scale <= 0) throw new IllegalArgumentException("scale must be positive: " + scale);
    }

    /** The same request with another mode: the preview and its role map are drawn alike in every other way. */
    public ThemePreviewRequest withMode(Mode other) {
        return new ThemePreviewRequest(other, language, textAntialiasing, lcdContrast, scale);
    }

    List<String> toArguments() {
        List<String> args = new ArrayList<>();
        args.add(MODE + mode.name());
        args.add(LANGUAGE + language.name());
        args.add(TEXT + textAntialiasing.name());
        args.add(CONTRAST + lcdContrast);
        args.add(SCALE + String.format(Locale.ROOT, "%s", scale));
        return args;
    }

    static ThemePreviewRequest fromArguments(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (String arg : args) {
            int split = arg.indexOf('=');
            if (split > 0) values.put(arg.substring(0, split + 1), arg.substring(split + 1));
        }
        return new ThemePreviewRequest(
                Mode.valueOf(required(values, MODE)),
                Language.valueOf(required(values, LANGUAGE)),
                TextAntialiasing.valueOf(required(values, TEXT)),
                Integer.parseInt(required(values, CONTRAST)),
                Double.parseDouble(required(values, SCALE)));
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) throw new IllegalArgumentException("Missing argument " + key);
        return value;
    }
}
