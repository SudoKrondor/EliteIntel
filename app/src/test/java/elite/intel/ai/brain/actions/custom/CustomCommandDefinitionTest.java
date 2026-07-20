package elite.intel.ai.brain.actions.customcommand;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandDefinitionTest {

    private static final Gson GSON = new Gson();

    // --- validate() ---

    @Test
    void validCustomCommandPassesValidation() {
        CustomCommandDefinition customCommand = deserialize("""
                {
                  "id": "custom_command_test",
                  "name": "Test",
                  "phrases": "test me",
                  "steps": [{"type": "SPEAK", "text": "hi"}]
                }
                """);
        assertDoesNotThrow(customCommand::validate);
    }

    @Test
    void blankIdIsRejected() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id": "", "name": "T", "phrases": "p", "steps": [{"type":"SPEAK","text":"x"}]}
                """);
        assertThrows(IllegalArgumentException.class, customCommand::validate);
    }

    @Test
    void blankNameIsRejected() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id": "custom_command_x", "name": "", "phrases": "p", "steps": [{"type":"SPEAK","text":"x"}]}
                """);
        assertThrows(IllegalArgumentException.class, customCommand::validate);
    }

    @Test
    void blankPhrasesIsRejected() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id": "custom_command_x", "name": "T", "phrases": "", "steps": [{"type":"SPEAK","text":"x"}]}
                """);
        assertThrows(IllegalArgumentException.class, customCommand::validate);
    }

    @Test
    void emptyStepsIsRejected() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id": "custom_command_x", "name": "T", "phrases": "p", "steps": []}
                """);
        assertThrows(IllegalArgumentException.class, customCommand::validate);
    }

    @Test
    void nullStepInsideListIsRejected() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id": "custom_command_x", "name": "T", "phrases": "p", "steps": [null]}
                """);
        assertThrows(IllegalArgumentException.class, customCommand::validate);
    }

    @Test
    void bindingTapStepWithoutBindingIdIsRejected() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id": "custom_command_x", "name": "T", "phrases": "p",
                 "steps": [{"type":"BINDING_TAP"}]}
                """);
        assertThrows(IllegalArgumentException.class, customCommand::validate);
    }

    @Test
    void speakStepWithoutTextIsRejected() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id": "custom_command_x", "name": "T", "phrases": "p",
                 "steps": [{"type":"SPEAK"}]}
                """);
        assertThrows(IllegalArgumentException.class, customCommand::validate);
    }

    @Test
    void descriptionIsOptionalAndDefaultsToEmpty() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id": "custom_command_x", "name": "T", "phrases": "p",
                 "steps": [{"type":"SPEAK","text":"ok"}]}
                """);
        assertDoesNotThrow(customCommand::validate);
        assertEquals("", customCommand.getDescription());
    }

    // --- distinctBindingIds() ---

    @Test
    void distinctBindingIdsForBindingTapAndHold() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id":"m","name":"N","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"A"},
                  {"type":"BINDING_HOLD","bindingId":"B","durationMs":200}
                ]}""");
        assertEquals(List.of("A", "B"), customCommand.distinctBindingIds());
    }

    @Test
    void distinctBindingIdsDeduplicates() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id":"m","name":"N","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"A"},
                  {"type":"BINDING_TAP","bindingId":"A"},
                  {"type":"BINDING_HOLD","bindingId":"B","durationMs":100}
                ]}""");
        assertEquals(List.of("A", "B"), customCommand.distinctBindingIds());
    }

    @Test
    void distinctBindingIdsExcludesDelayAndSpeakAndRunCommand() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id":"m","name":"N","phrases":"p","steps":[
                  {"type":"DELAY","durationMs":500},
                  {"type":"SPEAK","text":"hi"},
                  {"type":"RUN_COMMAND","actionId":"deploy_landing_gear"}
                ]}""");
        assertTrue(customCommand.distinctBindingIds().isEmpty());
    }

    @Test
    void distinctBindingIdsReturnsEmptyWhenNoBindingSteps() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id":"m","name":"N","phrases":"p","steps":[
                  {"type":"SPEAK","text":"hello"}
                ]}""");
        assertTrue(customCommand.distinctBindingIds().isEmpty());
    }

    @Test
    void distinctBindingIdsPreservesFirstOccurrenceOrder() {
        CustomCommandDefinition customCommand = deserialize("""
                {"id":"m","name":"N","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"B"},
                  {"type":"BINDING_TAP","bindingId":"A"},
                  {"type":"BINDING_TAP","bindingId":"B"}
                ]}""");
        assertEquals(List.of("B", "A"), customCommand.distinctBindingIds());
    }

    // --- helpers ---

    private CustomCommandDefinition deserialize(String json) {
        return GSON.fromJson(json, CustomCommandDefinition.class);
    }
}
