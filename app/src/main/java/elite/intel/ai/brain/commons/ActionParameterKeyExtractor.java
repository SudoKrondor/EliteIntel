package elite.intel.ai.brain.commons;

import elite.intel.ai.brain.AiActionsMap;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.ActionParameterSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to extract and manage action parameter hints from various sources such as alias placeholders
 * and inline JSON blocks. This class is responsible for parsing and consolidating information about
 * action parameters and their associated types, ensuring consistency and providing discovery mechanisms.
 * <br>
 * The extractor is a singleton that can be accessed via {@link #getInstance()}.
 */
public final class ActionParameterKeyExtractor {

    private static final ActionParameterKeyExtractor INSTANCE = new ActionParameterKeyExtractor();
    // Matches {…} blocks inside alias phrase groups, e.g. {lat:number, lon:number} or {state:true/false}.
    private static final Pattern PLACEHOLDER_BLOCK = Pattern.compile("\\{([^{}]+)}");
    // Matches inline JSON action+params examples embedded in the command rules prompt text.
    private static final Pattern JSON_ACTION_WITH_PARAMS = Pattern.compile(
            "\\{\\s*\"action\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"params\"\\s*:\\s*\\{([^{}]*)}\\s*}",
            Pattern.DOTALL
    );
    private static final Pattern JSON_PARAM = Pattern.compile("\"([A-Za-z0-9_]+)\"\\s*:\\s*(\"[^\"]*\"|-?\\d+(?:\\.\\d+)?|true|false)");
    private static final Pattern VALID_PARAM_NAME = Pattern.compile("[A-Za-z0-9_]+");

    private ActionParameterKeyExtractor() {
    }

    /** Returns the singleton extractor used by UI command parameter hints. */
    public static ActionParameterKeyExtractor getInstance() {
        return INSTANCE;
    }

    /**
     * Returns known parameter keys for an action ID, preserving discovery order and avoiding duplicates.
     */
    public List<String> parameterKeysForAction(String actionId) {
        return parameterHintsForAction(actionId).stream()
                .map(ActionParameterHint::name)
                .toList();
    }

    /**
     * Returns known parameter hints for an action ID. Type is best-effort and defaults to {@code string}.
     */
    public List<ActionParameterHint> parameterHintsForAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return List.of();
        }
        Map<String, ActionParameterHint> hints = extractAll().get(actionId.toLowerCase(java.util.Locale.ROOT));
        return hints == null ? List.of() : List.copyOf(hints.values());
    }

    /**
     * Extracts hints from both sources in order: aliases first, then JSON examples.
     * JSON examples run second so they can upgrade a {@code string} guess to {@code number} or
     * {@code boolean} when the prompt contains a typed example value for the same parameter.
     */
    private Map<String, Map<String, ActionParameterHint>> extractAll() {
        Map<String, Map<String, ActionParameterHint>> hintsByAction = new LinkedHashMap<>();
        extractAliasPlaceholders(hintsByAction);
        extractPromptJsonExamples(hintsByAction);
        extractRegistryParameters(hintsByAction);
        return hintsByAction;
    }

    /**
     * Third hint source: the self-describing command registry. Migrated built-in commands
     * declare a typed parameter schema via {@link IntelCommand#parameters()}; this is the
     * authoritative type source, so it runs last and upgrades the untyped {@code string}
     * guesses produced by alias placeholders (e.g. alias {@code {lat:X}} yields string,
     * the registry spec yields number). Only iterates commands present in the registry, so
     * non-migrated commands keep taking their hints from aliases/JSON examples untouched.
     */
    private void extractRegistryParameters(Map<String, Map<String, ActionParameterHint>> hintsByAction) {
        CommandRegistry.getInstance().byId().values().forEach(command -> {
            for (ActionParameterSpec spec : command.parameters()) {
                add(hintsByAction, command.id(), spec.getName(), spec.getType());
            }
        });
    }

    private void extractAliasPlaceholders(Map<String, Map<String, ActionParameterHint>> hintsByAction) {
        AiActionsMap.getInstance().actionMap(true).forEach((phraseGroup, actionId) -> {
            if (actionId == null || actionId.isBlank()) {
                return;
            }
            Matcher matcher = PLACEHOLDER_BLOCK.matcher(phraseGroup);
            while (matcher.find()) {
                for (String parameter : parametersFromPlaceholderBlock(matcher.group(1))) {
                    add(hintsByAction, actionId, parameter, typeFromPlaceholder(parameter));
                }
            }
        });
    }

    /** Parses the command-rules section of the LLM prompt for inline JSON examples like
     *  {@code {"action":"target_subsystem","params":{"key":"drive"}}} to extract typed param hints. */
    private void extractPromptJsonExamples(Map<String, Map<String, ActionParameterHint>> hintsByAction) {
        StringBuilder prompt = new StringBuilder();
        PromptFactory.getInstance().buildCommandRules(prompt);

        Matcher matcher = JSON_ACTION_WITH_PARAMS.matcher(prompt);
        while (matcher.find()) {
            String actionId = matcher.group(1);
            Matcher paramMatcher = JSON_PARAM.matcher(matcher.group(2));
            while (paramMatcher.find()) {
                add(hintsByAction, actionId, paramMatcher.group(1), typeFromJsonValue(paramMatcher.group(2)));
            }
        }
    }

    private static List<String> parametersFromPlaceholderBlock(String block) {
        if (block == null || block.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(block.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static String typeFromPlaceholder(String token) {
        String[] parts = token.trim().split(":", 2);
        if (parts.length < 2) {
            return "string";
        }
        String hint = parts[1].trim();
        if ("true/false".equalsIgnoreCase(hint) || "boolean".equalsIgnoreCase(hint)) return "boolean";
        if ("number".equalsIgnoreCase(hint) || "num".equalsIgnoreCase(hint)) return "number";
        return "string";
    }

    private static String parameterName(String token) {
        String name = token.trim().split(":", 2)[0].trim();
        return VALID_PARAM_NAME.matcher(name).matches() ? name : "";
    }

    private static String typeFromJsonValue(String value) {
        if (value == null || value.isBlank() || value.startsWith("\"")) {
            return "string";
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return "boolean";
        }
        return "number";
    }

    private static void add(Map<String, Map<String, ActionParameterHint>> hintsByAction, String actionId, String parameter, String type) {
        String name = parameterName(parameter);
        if (actionId == null || actionId.isBlank() || name.isBlank()) {
            return;
        }
        Map<String, ActionParameterHint> hints = hintsByAction.computeIfAbsent(
                actionId.toLowerCase(java.util.Locale.ROOT),
                ignored -> new LinkedHashMap<>()
        );
        ActionParameterHint existing = hints.get(name);
        // Allow upgrading a generic "string" guess to a more specific type; never downgrade.
        if (existing == null || "string".equals(existing.type()) && !"string".equals(type)) {
            hints.put(name, new ActionParameterHint(name, type));
        }
    }

    public record ActionParameterHint(String name, String type) {
        public ActionParameterHint {
            if (VALID_PARAM_NAME.matcher(name).matches()) {
                type = (type == null || type.isBlank()) ? "string" : type;
            } else {
                throw new IllegalArgumentException("Invalid parameter name: " + name);
            }
        }
    }
}
