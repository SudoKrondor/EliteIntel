package elite.intel.companion;

import elite.intel.session.SystemSession;

/**
 * The single place for the companion's settings. The companion-mode toggle is DB-backed (parallel to
 * conversation mode) and read through {@link SystemSession}; the confirmation code word is still a
 * hardcoded placeholder pending its own GUI/DB-backed value.
 */
public final class CompanionConfig {

    // TODO: back the confirmation code word by GUI/DB settings.
    private static final String CONFIRMATION_CODE_WORD = "password";

    private CompanionConfig() {
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
