package elite.intel.ai.brain.actions.customcommand;

/**
 * Thrown when a custom command step references a parameter that cannot be resolved from the invocation context.
 * This indicates either a missing optional parameter or a misconfigured step param mapping.
 */
public final class UnresolvedCustomCommandParamException extends RuntimeException {

    private final String paramName;

    public UnresolvedCustomCommandParamException(String paramName, String template) {
        super("Unresolved custom command parameter '" + paramName + "' in template: " + template);
        this.paramName = paramName;
    }

    public String getParamName() {
        return paramName;
    }
}
