package elite.intel.ai.brain.actions.handlers.commands.custom;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomCommandValidatorTest {

    @BeforeAll
    static void loadRegistries() {
        // builtInCommandIds()/builtInPhrases() read the singleton registries; load them here so the
        // built-in collision checks do not depend on another test class having initialized them first.
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        CustomCommandRegistry.getInstance().load();
    }

    @Test
    void validCustomCommandHasNoErrors() {
        CustomCommandDefinition customCommand = customCommand("custom_command_valid", "Valid", "valid phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        assertTrue(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void rejectsUnsafeIdAndBuiltInCommandId() {
        CustomCommandDefinition unsafe = customCommand("bad id", "Bad", "unique phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));
        CustomCommandDefinition builtIn = customCommand("deploy_landing_gear", "Bad", "another unique phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        assertFalse(CustomCommandValidator.validate(unsafe, List.of(), null).isEmpty());
        assertFalse(CustomCommandValidator.validate(builtIn, List.of(), null).isEmpty());
    }

    @Test
    void rejectsDuplicateCustomCommandIdExceptOriginalId() {
        CustomCommandDefinition existing = customCommand("custom_command_same", "Existing", "existing phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));
        CustomCommandDefinition candidate = customCommand("custom_command_same", "Candidate", "candidate phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        assertFalse(CustomCommandValidator.validate(candidate, List.of(existing), null).isEmpty());
        assertTrue(CustomCommandValidator.validate(candidate, List.of(existing), "custom_command_same").isEmpty());
    }

    @Test
    void rejectsDuplicateCustomCommandPhrase() {
        CustomCommandDefinition existing = customCommand("custom_command_existing", "Existing", "duplicate phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));
        CustomCommandDefinition candidate = customCommand("custom_command_candidate", "Candidate", "duplicate phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        assertFalse(CustomCommandValidator.validate(candidate, List.of(existing), null).isEmpty());
    }

    @Test
    void rejectsInvalidSteps() {
        CustomCommandDefinition candidate = customCommand("custom_command_candidate", "Candidate", "candidate phrase", List.of(
                new CustomCommandStep(CustomCommandStep.Type.DELAY, null, 0, null),
                new CustomCommandStep(CustomCommandStep.Type.BINDING_HOLD, "Binding", 0, null)
        ));

        assertFalse(CustomCommandValidator.validate(candidate, List.of(), null).isEmpty());
    }

    // --- actionKey format and length validation ---

    @Test
    void rejectsActionKeyWithUppercaseLetters() {
        CustomCommandDefinition customCommand = customCommand("Apply_Combat_Preset", "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
    }

    @Test
    void rejectsActionKeyWithHyphen() {
        CustomCommandDefinition customCommand = customCommand("apply-combat-preset", "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
    }

    @Test
    void rejectsActionKeyWithDotsAndColons() {
        CustomCommandDefinition customCommand = customCommand("apply.combat:preset", "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
    }

    @Test
    void rejectsActionKeyTooShort() {
        CustomCommandDefinition customCommand = customCommand("go", "Go", "go phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("at least")));
    }

    @Test
    void rejectsActionKeyTooLong() {
        String longKey = "a".repeat(CustomCommandValidator.MAX_ACTION_KEY_LENGTH + 1);
        CustomCommandDefinition customCommand = customCommand(longKey, "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("must not exceed")));
    }

    @Test
    void acceptsCyrillicActionKey() {
        // The key is derived from Cyrillic phrases and stays in-script for routing overlap; it must validate.
        CustomCommandDefinition customCommand = customCommand("лететь_к_миссии", "Mission", "лететь к миссии",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        assertTrue(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void acceptsActionKeyAtMinimumLength() {
        // "custom_command_test" is exactly MIN_ACTION_KEY_LENGTH (10) characters.
        CustomCommandDefinition customCommand = customCommand("custom_command_test", "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        assertTrue(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void patternErrorSuppressesLengthError() {
        // "bad key!" is 8 chars (below minimum) AND contains invalid characters.
        // Only the pattern error must be reported — not the length error.
        CustomCommandDefinition customCommand = customCommand("bad key!", "Bad", "bad phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello")));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
        assertFalse(errors.stream().anyMatch(e -> e.contains("at least")));
    }

    // --- helpers ---

    private static CustomCommandDefinition customCommand(String id, String name, String phrases, List<CustomCommandStep> steps) {
        return new CustomCommandDefinition(id, name, "", phrases, steps);
    }
}
