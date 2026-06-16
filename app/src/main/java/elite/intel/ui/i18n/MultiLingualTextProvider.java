package elite.intel.ui.i18n;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class MultiLingualTextProvider {

    private static final String BUNDLE_NAME = "i18n.gui";
    // Disable JVM locale fallback so missing translated keys fall through to English explicitly below.
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

    private MultiLingualTextProvider() {
    }

    public static String getText(String key, Object... args) {
        String pattern = resolveText(locale(), key);
        return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    public static String getText(Language language, String key, Object... args) {
        String pattern = resolveText(locale(language), key);
        return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    private static String resolveText(Locale locale, String key) {
        ResourceBundle selectedBundle = getBundle(locale);
        if (selectedBundle.containsKey(key)) {
            return selectedBundle.getString(key);
        }

        ResourceBundle fallbackBundle = getBundle(Locale.ENGLISH);
        if (fallbackBundle.containsKey(key)) {
            return fallbackBundle.getString(key);
        }

        return key;
    }

    private static ResourceBundle getBundle(Locale locale) {
        try {
            Locale bundleLocale = Locale.ENGLISH.equals(locale) ? Locale.ROOT : locale;
            return ResourceBundle.getBundle(BUNDLE_NAME, bundleLocale, NO_FALLBACK_CONTROL);
        } catch (MissingResourceException e) {
            return ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT, NO_FALLBACK_CONTROL);
        }
    }

    /**
     * Lowercase language tag of the active UI language (e.g. {@code "en"}, {@code "ru"}).
     * Intended for selecting localized resource files (manuals, credits) by language suffix.
     */
    public static String currentLanguageTag() {
        return locale().getLanguage();
    }

    private static Locale locale() {
        Language language = SystemSession.getInstance().getLanguage();
        return locale(language);
    }

    private static Locale locale(Language language) {
        return switch (language) {
            case RU -> Locale.forLanguageTag("ru");
            case UK -> Locale.forLanguageTag("uk");
            case DE -> Locale.GERMAN;
            case FR -> Locale.FRENCH;
            case EN -> Locale.ENGLISH;
            case ES -> Locale.forLanguageTag("es");
        };
    }
}
