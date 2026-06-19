package elite.intel.ai.brain.i18n;

import elite.intel.i18n.Language;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class AiActionAliasTextProvider {

    private static final String BUNDLE_NAME = "i18n.ai_action_aliases";
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

    private AiActionAliasTextProvider() {
    }

    public static String getText(Language language, String key) {
        return resolveText(locale(language), key);
    }

    /**
     * Whether the alias bundle for the given language actually defines this key,
     * checking the language bundle then the ROOT (English base) bundle. Unlike
     * {@link #getText} this does NOT fall back to returning the key itself, so a
     * present-but-equal-to-id phrase (e.g. {@code interrupt=interrupt}) counts as
     * existing, while a missing key (e.g. an action with no localized phrase) does not.
     */
    public static boolean hasKey(Language language, String key) {
        Locale locale = locale(language);
        if (getBundle(locale).containsKey(key)) return true;
        return getBundle(Locale.ROOT).containsKey(key);
    }

    private static String resolveText(Locale locale, String key) {
        ResourceBundle selected = getBundle(locale);
        if (selected.containsKey(key)) return selected.getString(key);
        ResourceBundle fallback = getBundle(Locale.ROOT);
        if (fallback.containsKey(key)) return fallback.getString(key);
        return key;
    }

    private static ResourceBundle getBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BUNDLE_NAME, locale, NO_FALLBACK_CONTROL);
        } catch (MissingResourceException e) {
            return ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT, NO_FALLBACK_CONTROL);
        }
    }

    private static Locale locale(Language language) {
        return switch (language) {
            case RU -> Locale.forLanguageTag("ru");
            case UK -> Locale.forLanguageTag("uk");
            case DE -> Locale.GERMAN;
            case FR -> Locale.FRENCH;
            case EN -> Locale.ROOT;
            case ES -> Locale.forLanguageTag("es");
            case IT -> Locale.ITALIAN;
            case PT -> Locale.forLanguageTag("pt");
        };
    }
}