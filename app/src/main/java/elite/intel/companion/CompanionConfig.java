package elite.intel.companion;

/**
 * The single place for the companion's currently-hardcoded settings, pending real GUI/DB-backed values.
 * Centralized so the placeholders can later be swapped for persisted configuration in one spot.
 */
public final class CompanionConfig {

    // TODO: back these by GUI/DB settings.
    private static final boolean COMPANION_MODE_ON = true;
    private static final String CONFIRMATION_CODE_WORD = "password";

    private CompanionConfig() {
    }

    /** Whether companion mode replaces the legacy command mode. */
    public static boolean companionModeOn() {
        return COMPANION_MODE_ON;
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
