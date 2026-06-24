package elite.intel.gameapi.i18n;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Random;
import java.util.ResourceBundle;

public final class EventsTextProvider {

    private static final String BUNDLE_NAME = "i18n.ed_events";
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

    private static final Random RANDOM = new Random();

    private EventsTextProvider() {
    }

    public static String getText(Language language, String key, Object... args) {
        String pattern = resolveText(locale(language), key);
        return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    public static String getText(String key, Object... args) {
        String pattern = resolveText(locale(), key);
        return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    private static Locale locale() {
        Language language = SystemSession.getInstance().getLanguage();
        return locale(language);
    }

    private static String resolveText(Locale locale, String key) {
        ResourceBundle selected = getBundle(locale);
        if (selected.containsKey(key)) return pickVariant(selected.getString(key));
        ResourceBundle fallback = getBundle(Locale.ENGLISH);
        if (fallback.containsKey(key)) return pickVariant(fallback.getString(key));
        return key;
    }

    private static String pickVariant(String raw) {
        if (!raw.contains("|")) return raw;
        String[] parts = raw.split("\\|");
        return parts[RANDOM.nextInt(parts.length)].trim();
    }

    private static ResourceBundle getBundle(Locale locale) {
        try {
            Locale bundleLocale = Locale.ENGLISH.equals(locale) ? Locale.ROOT : locale;
            return ResourceBundle.getBundle(BUNDLE_NAME, bundleLocale, NO_FALLBACK_CONTROL);
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
            case EN -> Locale.ENGLISH;
            case ES -> Locale.forLanguageTag("es");
            case PT -> Locale.forLanguageTag("pt");
            case IT -> Locale.ITALIAN;
        };
    }
}
