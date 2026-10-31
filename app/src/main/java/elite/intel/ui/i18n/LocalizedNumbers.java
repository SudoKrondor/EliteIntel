package elite.intel.ui.i18n;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats numbers the commander reads on screen - HUD overlay cards and Swing widgets - in the
 * language they chose in the app.
 * <p>
 * WHY the chosen language rather than the JVM default: the default locale is whatever the operating
 * system happens to be set to, which is neither what the commander picked here nor stable across
 * machines. An Italian commander running the app in Italian sees "1.500.000 cr" either way, but a
 * commander running an English app on an Italian Windows would otherwise get Italian separators
 * beside English labels, and every test asserting a formatted figure would pass or fail depending on
 * the developer's machine.
 * <p>
 * WHY not everywhere: this is for text a human reads. Numbers on the wire - the overlay protocol's
 * config values, anything parsed back - stay {@link Locale#ROOT} so the reader is not guessing at
 * separators.
 */
public final class LocalizedNumbers {

    private LocalizedNumbers() {
    }

    /**
     * A whole number with the commander's own grouping separators, e.g. {@code 1,500,000} in English
     * and {@code 1.500.000} in Italian.
     */
    public static String grouped(long value) {
        return NumberFormat.getIntegerInstance(locale()).format(value);
    }

    /**
     * The same, in a language named explicitly rather than read from the session - for a figure that sits
     * inside a sentence whose language is already decided (see {@code DisplayNumerals}).
     */
    public static String grouped(long value, Language language) {
        return NumberFormat.getIntegerInstance(locale(language)).format(value);
    }

    /**
     * A number that has a fractional part, grouped and with the language's own decimal mark: {@code 1.02} in
     * English, {@code 1,02} in German. Two fraction digits at most, which is all the credit rounding produces.
     */
    public static String decimal(double value, Language language) {
        NumberFormat format = NumberFormat.getNumberInstance(locale(language));
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    /**
     * The locale to format numbers in, for callers that need a {@link NumberFormat} of their own.
     */
    public static Locale locale() {
        return locale(SystemSession.getInstance().getLanguage());
    }

    /**
     * WHY this is not the resource-bundle locale: bundles are selected by the pseudo-tag
     * {@code ptbz}, which no locale data exists for. Number formatting needs a real locale, so the
     * two Portuguese variants map to the countries they actually are.
     */
    static Locale locale(Language language) {
        return switch (language) {
            case EN -> Locale.ENGLISH;
            case RU -> Locale.forLanguageTag("ru");
            case UK -> Locale.forLanguageTag("uk");
            case DE -> Locale.GERMAN;
            case FR -> Locale.FRENCH;
            case ES -> Locale.forLanguageTag("es");
            case IT -> Locale.ITALIAN;
            case PT -> Locale.forLanguageTag("pt-PT");
            case PTBZ -> Locale.forLanguageTag("pt-BR");
        };
    }
}
