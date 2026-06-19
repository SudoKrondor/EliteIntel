package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.brain.AiActionsMap;
import elite.intel.ai.brain.actions.command.CommandIds;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.i18n.AiActionLocalizations;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for validating custom command definitions. Provides methods to validate
 * actionKey formatting, uniqueness, parameter integrity, and step configurations.
 * This class is used to enforce proper custom command rules both at the context-independent
 * level (individual command validation) and context-dependent level (cross-command validation).
 *
 * The following validation aspects are supported:
 * - Action key format: patterns, length constraints, and collision detection with built-in commands.
 * - Cross-reference validation: uniqueness of action keys, phrase collisions, and step parameter references.
 * - Parameter validation: parameter names, types, and declared usage in templates.
 *
 * The class is not instantiable.
 */
public final class CustomCommandValidator {

    /** Strict snake_case: lowercase letters, digits, and underscores only. */
    static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_]+");
    static final int MIN_ACTION_KEY_LENGTH = 10;
    static final int MAX_ACTION_KEY_LENGTH = 60;

    private static final Pattern VALID_PARAM_NAME = Pattern.compile("[A-Za-z0-9_]+");

    private CustomCommandValidator() {
    }

    /**
     * Validates actionKey format rules that require no cross-custom command context:
     * pattern, length, and built-in command collision.
     * Returns an empty list when all format rules pass.
     */
    public static List<String> validateFormat(CustomCommandDefinition candidate) {
        if (candidate == null) {
            return List.of("CustomCommand is missing.");
        }
        List<String> errors = new ArrayList<>();
        String actionKey = candidate.getActionKey();
        if (actionKey == null || actionKey.isBlank()) {
            errors.add("Action key is required.");
        } else {
            appendActionKeyFormatErrors(actionKey, errors);
            if (builtInCommandIds().contains(actionKey.toLowerCase(Locale.ROOT))) {
                errors.add("Action key collides with a built-in command.");
            }
        }
        return List.copyOf(errors);
    }

    /**
     * Full customCommand validation including cross-custom command context.
     * Subsumes all checks from {@link #validateFormat} and additionally checks
     * actionKey uniqueness, phrase collisions, parameter names/types, and step
     * parameter references.
     *
     * @param existingCustomCommands    all currently saved customCommands, used for uniqueness and phrase checks
     * @param originalActionKey the customCommand's {@code actionKey} before editing ({@code null} for
     *                          new customCommands); allows a customCommand to keep its own key during an edit
     *                          without triggering a uniqueness error
     */
    public static List<String> validate(
            CustomCommandDefinition candidate,
            List<CustomCommandDefinition> existingCustomCommands,
            String originalActionKey
    ) {
        List<String> errors = new ArrayList<>();
        if (candidate == null) {
            return List.of("CustomCommand is missing.");
        }
        validateIdentity(candidate, existingCustomCommands, originalActionKey, errors);
        validatePhrases(candidate, existingCustomCommands, originalActionKey, errors);
        validateParameters(candidate, errors);
        validateSteps(candidate, existingCustomCommands, errors);
        return List.copyOf(errors);
    }

    private static void validateIdentity(
            CustomCommandDefinition candidate,
            List<CustomCommandDefinition> existingCustomCommands,
            String originalActionKey,
            List<String> errors
    ) {
        String actionKey = candidate.getActionKey();
        if (actionKey == null || actionKey.isBlank()) {
            errors.add("Action key is required.");
        } else {
            appendActionKeyFormatErrors(actionKey, errors);
            if (builtInCommandIds().contains(normalize(actionKey))) {
                errors.add("Action key collides with a built-in command.");
            }
            for (CustomCommandDefinition customCommand : safeCustomCommands(existingCustomCommands)) {
                if (!sameId(customCommand.getActionKey(), originalActionKey) && sameId(customCommand.getActionKey(), actionKey)) {
                    errors.add("Action key must be unique among customCommands.");
                    break;
                }
            }
        }

        if (candidate.getName() == null || candidate.getName().isBlank()) {
            errors.add("Name is required.");
        }
    }

    /**
     * Appends pattern and length errors for {@code actionKey}.
     * Length is checked only when the pattern passes so both errors never appear together.
     */
    private static void appendActionKeyFormatErrors(String actionKey, List<String> errors) {
        if (!SAFE_ID.matcher(actionKey).matches()) {
            errors.add("Action key must use lowercase letters, numbers, and underscores only.");
        } else if (actionKey.length() < MIN_ACTION_KEY_LENGTH) {
            errors.add("Action key must be at least " + MIN_ACTION_KEY_LENGTH + " characters.");
        } else if (actionKey.length() > MAX_ACTION_KEY_LENGTH) {
            errors.add("Action key must not exceed " + MAX_ACTION_KEY_LENGTH + " characters.");
        }
    }

    private static void validatePhrases(
            CustomCommandDefinition candidate,
            List<CustomCommandDefinition> existingCustomCommands,
            String originalActionKey,
            List<String> errors
    ) {
        List<String> phrases = AiActionLocalizations.splitPhraseGroup(candidate.getPhrases());
        if (phrases.isEmpty()) {
            errors.add("At least one phrase is required.");
            return;
        }

        Set<String> builtInPhrases = builtInPhrases();
        Set<String> seen = new HashSet<>();
        for (String phrase : phrases) {
            String normalized = normalize(phrase);
            if (!seen.add(normalized)) {
                errors.add("Duplicate phrase: " + phrase);
            }
            if (builtInPhrases.contains(normalized)) {
                errors.add("Phrase collides with a built-in action alias: " + phrase);
            }
        }

        for (CustomCommandDefinition customCommand : safeCustomCommands(existingCustomCommands)) {
            if (sameId(customCommand.getActionKey(), originalActionKey)) {
                continue;
            }
            Set<String> otherPhrases = normalizedPhrases(customCommand);
            for (String phrase : phrases) {
                if (otherPhrases.contains(normalize(phrase))) {
                    errors.add("Phrase collides with another custom command: " + phrase);
                }
            }
        }
    }

    private static void validateParameters(CustomCommandDefinition candidate, List<String> errors) {
        List<CustomCommandParameterSpec> params = candidate.getParameters();
        if (params.isEmpty()) return;

        Set<String> seen = new HashSet<>();
        for (CustomCommandParameterSpec param : params) {
            if (param == null || param.getName() == null || param.getName().isBlank()) {
                errors.add("Parameter name is required.");
                continue;
            }
            String normalizedName = param.getName().toLowerCase(Locale.ROOT);
            if (!VALID_PARAM_NAME.matcher(param.getName()).matches()) {
                errors.add("Parameter '" + param.getName() + "': name may only contain letters, digits, or underscore.");
            }
            if (!seen.add(normalizedName)) {
                errors.add("Duplicate parameter name: " + param.getName());
            }
            if (param.getType() == null || !CustomCommandParameterSpec.VALID_TYPES.contains(param.getType())) {
                errors.add("Parameter '" + param.getName() + "': type must be one of "
                        + CustomCommandParameterSpec.VALID_TYPES + ".");
            }
        }
    }

    private static void validateSteps(
            CustomCommandDefinition candidate,
            List<CustomCommandDefinition> existingCustomCommands,
            List<String> errors
    ) {
        List<CustomCommandStep> steps = candidate.getSteps();
        if (steps.isEmpty()) {
            errors.add("At least one step is required.");
            return;
        }

        Set<String> customCommandIds = new HashSet<>();
        for (CustomCommandDefinition customCommand : safeCustomCommands(existingCustomCommands)) {
            customCommandIds.add(normalize(customCommand.getActionKey()));
        }
        customCommandIds.add(normalize(candidate.getActionKey()));

        Set<String> declaredParamNames = new HashSet<>();
        for (CustomCommandParameterSpec spec : candidate.getParameters()) {
            if (spec != null && spec.getName() != null) {
                declaredParamNames.add(spec.getName());
            }
        }

        for (int i = 0; i < steps.size(); i++) {
            CustomCommandStep step = steps.get(i);
            String prefix = "Step " + (i + 1) + ": ";
            if (step == null || step.getType() == null) {
                errors.add(prefix + "type is required.");
                continue;
            }
            switch (step.getType()) {
                case SPEAK -> {
                    requireText(step.getText(), prefix + "text is required.", errors);
                    validateParamRefs(step.getText(), prefix, "text", declaredParamNames, errors);
                }
                case BINDING_TAP -> requireText(step.getBindingId(), prefix + "bindingId is required.", errors);
                case BINDING_HOLD -> {
                    requireText(step.getBindingId(), prefix + "bindingId is required.", errors);
                    requirePositive(step.getDurationMs(), prefix + "durationMs must be positive.", errors);
                }
                case DELAY -> requirePositive(step.getDurationMs(), prefix + "durationMs must be positive.", errors);
                case RAW_KEY -> requireText(step.getRawKey(), prefix + "rawKey is required.", errors);
                case RUN_COMMAND -> {
                    requireText(step.getActionId(), prefix + "commandId is required.", errors);
                    if (customCommandIds.contains(normalize(step.getActionId()))) {
                        errors.add(prefix + "RUN_COMMAND cannot target another custom command.");
                    }
                    step.getStepParams().forEach((key, template) ->
                            validateParamRefs(template, prefix, "stepParams[" + key + "]",
                                    declaredParamNames, errors));
                }
            }
        }
    }

    /** Checks that all {@code ${name}} references in {@code template} are declared custom command parameters. */
    private static void validateParamRefs(
            String template, String stepPrefix, String fieldName,
            Set<String> declaredParamNames, List<String> errors
    ) {
        if (template == null || declaredParamNames.isEmpty()) return;
        Matcher m = CustomCommandExecutionContext.PARAM_REF.matcher(template);
        while (m.find()) {
            String ref = m.group(1);
            if (!declaredParamNames.contains(ref)) {
                errors.add(stepPrefix + fieldName + " references undeclared parameter '" + ref + "'.");
            }
        }
    }

    private static void requireText(String value, String message, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(message);
        }
    }

    private static void requirePositive(int value, String message, List<String> errors) {
        if (value <= 0) {
            errors.add(message);
        }
    }

    static Set<String> builtInCommandIds() {
        Set<String> ids = new HashSet<>();
        for (String id : elite.intel.ai.brain.actions.command.CommandRegistry.getInstance().byId().keySet()) {
            ids.add(normalize(id));
        }
        return ids;
    }

    private static Set<String> builtInPhrases() {
        var full = AiActionsMap.getInstance().actionMap(true);
        Set<String> floating = Set.of(
                QueryIds.GENERAL_CONVERSATION,
                CommandIds.IGNORE_NONSENSICAL_INPUT,
                QueryIds.CONNECTION_CHECK);
        Set<String> customKeys = new HashSet<>();
        for (CustomCommandDefinition def : CustomCommandRegistry.getInstance().getCustomCommands()) {
            customKeys.add(def.getActionKey());
        }
        Set<String> phrases = new HashSet<>();
        full.forEach((group, id) -> {
            if (!floating.contains(id) && !customKeys.contains(id)) {
                AiActionLocalizations.splitPhraseGroup(group).forEach(phrase -> phrases.add(normalize(phrase)));
            }
        });
        return phrases;
    }

    private static Set<String> normalizedPhrases(CustomCommandDefinition customCommand) {
        Set<String> phrases = new HashSet<>();
        AiActionLocalizations.splitPhraseGroup(customCommand.getPhrases()).forEach(phrase -> phrases.add(normalize(phrase)));
        return phrases;
    }

    private static List<CustomCommandDefinition> safeCustomCommands(List<CustomCommandDefinition> customCommands) {
        return customCommands == null ? List.of() : customCommands;
    }

    private static boolean sameId(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
