package elite.intel.ai.brain.actions.handlers.commands.custom;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.commands.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.queries.ConnectionCheckQuery;
import elite.intel.ai.brain.actions.handlers.queries.GeneralConversationQuery;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates custom-command identity, aliases, and steps.
 * <p>
 * Custom commands sit alongside the built-in actions rather than replacing them, so an action key or
 * trigger phrase that collides with a built-in is rejected outright - the classifier must never have to
 * choose between a built-in and a user-authored twin of it.
 */
public final class CustomCommandValidator {

    /**
     * Routing-safe key: lowercase letters and decimal digits of <em>any</em> script, plus underscore.
     * {@code \p{Ll}} keeps cased lowercase letters (Latin {@code a-z}, Cyrillic {@code а-я}, ...),
     * {@code \p{Lo}} keeps caseless scripts (CJK, Arabic, ...), {@code \p{Nd}} keeps decimal digits.
     * Uppercase letters, whitespace, colons, dots and hyphens are all excluded, so a key can never
     * break the {@code "  <key>:\n"} prompt block or carry a script the routing LLM can't echo.
     * Keys are machine-derived from phrases via {@link CustomCommandKeyDeriver}, never hand-typed.
     */
    static final Pattern SAFE_ID = Pattern.compile("[\\p{Ll}\\p{Lo}\\p{Nd}_]+");
    static final int MIN_ACTION_KEY_LENGTH = 3;
    static final int MAX_ACTION_KEY_LENGTH = 60;

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
     * actionKey uniqueness, phrase collisions, and step fields.
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
        validateSteps(candidate, errors);
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

    private static void validateSteps(CustomCommandDefinition candidate, List<String> errors) {
        List<CustomCommandStep> steps = candidate.getSteps();
        if (steps.isEmpty()) {
            errors.add("At least one step is required.");
            return;
        }

        for (int i = 0; i < steps.size(); i++) {
            CustomCommandStep step = steps.get(i);
            String prefix = "Step " + (i + 1) + ": ";
            if (step == null || step.getType() == null) {
                errors.add(prefix + "type is required.");
                continue;
            }
            switch (step.getType()) {
                case SPEAK -> requireText(step.getText(), prefix + "text is required.", errors);
                case BINDING_TAP -> requireText(step.getBindingId(), prefix + "bindingId is required.", errors);
                case BINDING_HOLD -> {
                    requireText(step.getBindingId(), prefix + "bindingId is required.", errors);
                    requirePositive(step.getDurationMs(), prefix + "durationMs must be positive.", errors);
                }
                case DELAY -> requirePositive(step.getDurationMs(), prefix + "durationMs must be positive.", errors);
                case RAW_KEY -> requireText(step.getRawKey(), prefix + "rawKey is required.", errors);
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
        for (String id : CommandRegistry.getInstance().byId().keySet()) {
            ids.add(normalize(id));
        }
        return ids;
    }

    private static Set<String> builtInPhrases() {
        Set<String> floating = Set.of(
                GeneralConversationQuery.ID,
                IgnoreNonsensicalInputCommand.ID,
                ConnectionCheckQuery.ID);
        Language language = SystemSession.getInstance().getLanguage();
        Set<String> phrases = new HashSet<>();
        List<IntelAction> builtIns = new ArrayList<>();
        builtIns.addAll(CommandRegistry.getInstance().byId().values());
        builtIns.addAll(QueryRegistry.getInstance().byId().values());
        for (IntelAction action : builtIns) {
            if (!floating.contains(action.id()) && AiActionAliasTextProvider.hasKey(language, action.id())) {
                AiActionLocalizations.splitPhraseGroup(
                                AiActionAliasTextProvider.getText(language, action.id()))
                        .forEach(phrase -> phrases.add(normalize(phrase)));
            }
        }
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
