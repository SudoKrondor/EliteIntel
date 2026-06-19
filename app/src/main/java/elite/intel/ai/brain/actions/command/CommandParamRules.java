package elite.intel.ai.brain.actions.command;

import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;

import java.util.List;

/**
 * Shared formatter for command parameter rules injected into the action-extraction
 * system prompt. Single owner of the per-parameter block format so the built-in
 * (self-describing) and custom-command paths cannot drift apart.
 *
 * Format must match the original inline loop in
 * CustomCommandRegistry.appendCustomCommandParamRules byte-for-byte.
 */
public final class CommandParamRules {

    private CommandParamRules() {
    }

    /**
     * Appends one command's parameter block:
     *   "  <actionKey>:\n"
     *   "    <name> (<type>[, required])[ - <description>][. E.g.: <ex1>, <ex2>]\n"
     *   "      Hint: <extractionHint>\n"   (only when hint present)
     * Caller is responsible for the section header and for deciding which commands
     * are active. No-op effect if params is empty (caller should pre-filter).
     */
    public static void appendCommandBlock(String actionKey, List<CustomCommandParameterSpec> params, StringBuilder sb) {
        sb.append("  ").append(actionKey).append(":\n");
        for (CustomCommandParameterSpec param : params) {
            sb.append("    ").append(param.getName())
              .append(" (").append(param.getType());
            if (param.isRequired()) sb.append(", required");
            sb.append(")");
            if (!param.getDescription().isBlank()) {
                sb.append(" - ").append(param.getDescription());
            }
            List<String> examples = param.getExamples();
            if (!examples.isEmpty()) {
                sb.append(". E.g.: ").append(String.join(", ", examples));
            }
            sb.append("\n");
            if (param.getExtractionHint() != null && !param.getExtractionHint().isBlank()) {
                sb.append("      Hint: ").append(param.getExtractionHint()).append("\n");
            }
        }
    }
}
