package elite.intel.ai.hands;

/** Thrown when applying a working copy to the game bindings directory fails. */
public class BindingsApplyException extends Exception {
    private final String localizationKey;

    public BindingsApplyException(String message) {
        this(null, message, null);
    }

    public BindingsApplyException(String message, Throwable cause) {
        this(null, message, cause);
    }

    private BindingsApplyException(String localizationKey, String message, Throwable cause) {
        super(message, cause);
        this.localizationKey = localizationKey;
    }

    /**
     * Creates an exception whose user-facing message should be resolved from the GUI bundle.
     */
    public static BindingsApplyException localized(String localizationKey, String fallbackMessage) {
        return new BindingsApplyException(localizationKey, fallbackMessage, null);
    }

    /**
     * Returns the GUI bundle key for the user-facing message, or {@code null} for technical errors.
     */
    public String localizationKey() {
        return localizationKey;
    }
}
