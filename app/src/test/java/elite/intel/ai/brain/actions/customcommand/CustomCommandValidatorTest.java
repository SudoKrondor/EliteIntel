package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.brain.actions.ActionParameterSpec;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomCommandValidatorTest {

    @Test
    void validCustomCommandHasNoErrors() {
        CustomCommandDefinition customCommand = customCommand("custom_command_valid", "Valid", "valid phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertTrue(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void rejectsUnsafeIdAndBuiltInCommandId() {
        CustomCommandDefinition unsafe = customCommand("bad id", "Bad", "unique phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));
        CustomCommandDefinition builtIn = customCommand("deploy_landing_gear", "Bad", "another unique phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertFalse(CustomCommandValidator.validate(unsafe, List.of(), null).isEmpty());
        assertFalse(CustomCommandValidator.validate(builtIn, List.of(), null).isEmpty());
    }

    @Test
    void rejectsDuplicateCustomCommandIdExceptOriginalId() {
        CustomCommandDefinition existing = customCommand("custom_command_same", "Existing", "existing phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));
        CustomCommandDefinition candidate = customCommand("custom_command_same", "Candidate", "candidate phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertFalse(CustomCommandValidator.validate(candidate, List.of(existing), null).isEmpty());
        assertTrue(CustomCommandValidator.validate(candidate, List.of(existing), "custom_command_same").isEmpty());
    }

    @Test
    void rejectsDuplicateCustomCommandPhrase() {
        CustomCommandDefinition existing = customCommand("custom_command_existing", "Existing", "duplicate phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));
        CustomCommandDefinition candidate = customCommand("custom_command_candidate", "Candidate", "duplicate phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertFalse(CustomCommandValidator.validate(candidate, List.of(existing), null).isEmpty());
    }

    @Test
    void rejectsInvalidStepsAndCustomCommandRunCommandTarget() {
        CustomCommandDefinition existing = customCommand("custom_command_other", "Other", "other phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));
        CustomCommandDefinition candidate = customCommand("custom_command_candidate", "Candidate", "candidate phrase", List.of(
                new CustomCommandStep(CustomCommandStep.Type.DELAY, null, 0, null, null),
                new CustomCommandStep(CustomCommandStep.Type.BINDING_HOLD, "Binding", 0, null, null),
                new CustomCommandStep(CustomCommandStep.Type.RUN_COMMAND, null, 0, null, "custom_command_other")
        ));

        assertFalse(CustomCommandValidator.validate(candidate, List.of(existing), null).isEmpty());
    }

    // --- parameter validation ---

    @Test
    void validCustomCommandWithParametersHasNoErrors() {
        CustomCommandDefinition customCommand = customCommandWithParams("custom_command_paramtest", "Param Test", "param test phrase",
                List.of(new ActionParameterSpec("lat", "number", true, "latitude", null, null),
                        new ActionParameterSpec("lon", "number", true, "longitude", null, null)),
                List.of(CustomCommandStep.runCommandWithParams("navigate_to_coordinates",
                        Map.of("lat", "${lat}", "lon", "${lon}"))));

        assertTrue(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void rejectsDuplicateParameterName() {
        CustomCommandDefinition customCommand = customCommandWithParams("custom_command_dup", "Dup", "dup phrase",
                List.of(new ActionParameterSpec("speed", "string", true, "", null, null),
                        new ActionParameterSpec("speed", "number", false, "", null, null)),
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertFalse(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void rejectsInvalidParameterType() {
        CustomCommandDefinition customCommand = customCommandWithParams("custom_command_badtype", "Bad Type", "bad type phrase",
                List.of(new ActionParameterSpec("val", "integer", true, "", null, null)),
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertFalse(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void rejectsInvalidParameterName() {
        CustomCommandDefinition customCommand = customCommandWithParams("custom_command_badname", "Bad Name", "bad name phrase",
                List.of(new ActionParameterSpec("bad name!", "string", true, "", null, null)),
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertFalse(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void rejectsUndeclaredParamRefInStepParams() {
        // Step references ${lat} but no parameter named "lat" is declared.
        CustomCommandDefinition customCommand = customCommandWithParams("custom_command_undeclared", "Undeclared", "undeclared phrase",
                List.of(new ActionParameterSpec("lon", "number", true, "", null, null)),
                List.of(CustomCommandStep.runCommandWithParams("navigate_to_coordinates",
                        Map.of("lat", "${lat}", "lon", "${lon}"))));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lat")));
    }

    @Test
    void allowsDeclaredButUnusedParam() {
        // "future_param" is declared but not referenced in any step.
        // This must be allowed (reserved for future conditions/repeat).
        CustomCommandDefinition customCommand = customCommandWithParams("custom_command_unused", "Unused Param", "unused param phrase",
                List.of(new ActionParameterSpec("future_param", "boolean", false, "", null, null)),
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertTrue(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void rejectsUndeclaredParamRefInSpeakText() {
        CustomCommandDefinition customCommand = customCommandWithParams("custom_command_speakref", "Speak Ref", "speak ref phrase",
                List.of(new ActionParameterSpec("name", "string", true, "", null, null)),
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "Hello ${name}, going to ${dest}", null)));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("dest")));
        // "name" is declared, so no error for it
        assertFalse(errors.stream().anyMatch(e -> e.contains("'name'")));
    }

    // --- actionKey format and length validation ---

    @Test
    void rejectsActionKeyWithUppercaseLetters() {
        CustomCommandDefinition customCommand = customCommand("Apply_Combat_Preset", "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
    }

    @Test
    void rejectsActionKeyWithHyphen() {
        CustomCommandDefinition customCommand = customCommand("apply-combat-preset", "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
    }

    @Test
    void rejectsActionKeyWithDotsAndColons() {
        CustomCommandDefinition customCommand = customCommand("apply.combat:preset", "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
    }

    @Test
    void rejectsActionKeyTooShort() {
        CustomCommandDefinition customCommand = customCommand("go", "Go", "go phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("at least")));
    }

    @Test
    void rejectsActionKeyTooLong() {
        String longKey = "a".repeat(CustomCommandValidator.MAX_ACTION_KEY_LENGTH + 1);
        CustomCommandDefinition customCommand = customCommand(longKey, "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("must not exceed")));
    }

    @Test
    void acceptsActionKeyAtMinimumLength() {
        // "custom_command_test" is exactly MIN_ACTION_KEY_LENGTH (10) characters.
        CustomCommandDefinition customCommand = customCommand("custom_command_test", "Test", "test phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        assertTrue(CustomCommandValidator.validate(customCommand, List.of(), null).isEmpty());
    }

    @Test
    void patternErrorSuppressesLengthError() {
        // "bad key!" is 8 chars (below minimum) AND contains invalid characters.
        // Only the pattern error must be reported — not the length error.
        CustomCommandDefinition customCommand = customCommand("bad key!", "Bad", "bad phrase",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));

        List<String> errors = CustomCommandValidator.validate(customCommand, List.of(), null);
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
        assertFalse(errors.stream().anyMatch(e -> e.contains("at least")));
    }

    // --- helpers ---

    private static CustomCommandDefinition customCommand(String id, String name, String phrases, List<CustomCommandStep> steps) {
        return new CustomCommandDefinition(id, name, "", phrases, steps);
    }

    private static CustomCommandDefinition customCommandWithParams(String id, String name, String phrases,
                                                   List<ActionParameterSpec> params, List<CustomCommandStep> steps) {
        return new CustomCommandDefinition(id, name, "", phrases, params, steps);
    }
}
