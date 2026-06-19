package elite.intel.ui.i18n;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.text.MessageFormat;
import java.util.*;

public final class MultiLingualTextProvider {

    private static final String BUNDLE_NAME = "i18n.gui";
    // Disable JVM locale fallback so missing translated keys fall through to English explicitly below.
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

    private static final Random RANDOM = new Random();

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
            return pickVariant(selectedBundle.getString(key));
        }

        ResourceBundle fallbackBundle = getBundle(Locale.ENGLISH);
        if (fallbackBundle.containsKey(key)) {
            return pickVariant(fallbackBundle.getString(key));
        }

        return key;
    }

    private static String pickVariant(String raw) {
        List<String> parts = splitTopLevelVariants(raw);
        if (parts.size() == 1) return raw;
        return parts.get(RANDOM.nextInt(parts.size())).trim();
    }

    private static List<String> splitTopLevelVariants(String raw) {
        if (!raw.contains("|")) return List.of(raw);

        List<String> parts = new ArrayList<>();
        int start = 0;
        int braceDepth = 0;
        boolean inQuote = false;

        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\'') {
                // Apostrophes inside words (for example, French "J'écoute") are
                // punctuation, not MessageFormat quote delimiters.
                if (i > 0
                        && i + 1 < raw.length()
                        && Character.isLetter(raw.charAt(i - 1))
                        && Character.isLetter(raw.charAt(i + 1))) {
                    continue;
                }
                if (i + 1 < raw.length() && raw.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    inQuote = !inQuote;
                }
                continue;
            }

            if (inQuote) continue;

            if (ch == '{') {
                braceDepth++;
            } else if (ch == '}' && braceDepth > 0) {
                braceDepth--;
            } else if (ch == '|' && braceDepth == 0) {
                parts.add(raw.substring(start, i).trim());
                start = i + 1;
            }
        }

        if (start == 0) return List.of(raw);
        parts.add(raw.substring(start).trim());
        return parts;
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
            case PT -> Locale.forLanguageTag("pt");
            case IT -> Locale.ITALIAN;
        };
    }
}
