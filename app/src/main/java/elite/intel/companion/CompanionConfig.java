package elite.intel.companion;

import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The single place for the companion's settings. The companion-mode toggle is DB-backed (parallel to
 * conversation mode) and read through {@link SystemSession}; the confirmation code word is still a
 * hardcoded placeholder pending its own GUI/DB-backed value.
 */
public final class CompanionConfig {

    // TODO: back the confirmation code word by GUI/DB settings.
    private static final String CONFIRMATION_CODE_WORD = "password";

    // TODO: back the companion name by GUI/DB settings.
    private static final String COMPANION_NAME = "Vega";

    // Localization key (i18n.llm bundle) for the name's spoken/STT form per language: Latin "Vega" for
    // Latin-script languages, "Вега" for Cyrillic (ru/uk). Input matching only - never used in the prompt.
    private static final String NAME_SPOKEN_KEY = "companion.name.spoken";

    private CompanionConfig() {
    }

    /** The companion's own name, woven into its persona prompt. */
    public static String companionName() {
        return COMPANION_NAME;
    }

    /**
     * The name forms recognized as a leading vocative on commander INPUT (the reflex vocative strip): the
     * canonical name plus its localized spoken/STT form for the current session language (e.g. Cyrillic "Вега"
     * for ru/uk, from the i18n.llm bundle). Input matching only - the prompt always uses {@link #companionName()}.
     * <p>
     * TODO: when the name becomes GUI/DB-configurable, its spoken variants must follow the configured value.
     */
    public static List<String> companionNameForms() {
        Language language = SystemSession.getInstance().getLanguage();
        Set<String> forms = new LinkedHashSet<>();
        forms.add(COMPANION_NAME);
        String spoken = LlmTextProvider.getText(language, NAME_SPOKEN_KEY);
        if (spoken != null && !spoken.isBlank()) {
            forms.add(spoken.trim());
        }
        return List.copyOf(forms);
    }

    /**
     * Whether companion mode replaces the legacy command mode. DB-backed (defaults off).
     */
    public static boolean companionModeOn() {
        return SystemSession.getInstance().companionModeOn();
    }

    /** The spoken code word that confirms a frozen dangerous action (§2.13). */
    public static String confirmationCodeWord() {
        return CONFIRMATION_CODE_WORD;
    }

    /** Whether the commander input is exactly the confirmation code word (trimmed, case-insensitive). */
    public static boolean isConfirmationCodeWord(String input) {
        return input != null && input.strip().equalsIgnoreCase(CONFIRMATION_CODE_WORD);
    }
}
