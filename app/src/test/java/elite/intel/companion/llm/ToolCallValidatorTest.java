package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallValidatorTest {

    private static final LlmToolDefinition TOOL = new LlmToolDefinition(
            "navigate",
            "Navigate",
            "",
            List.of(
                    new ActionParameterSpec("target", "string", true, "Target", List.of(), null,
                            List.of("station", "system")),
                    new ActionParameterSpec("distance", "number", false, "Distance", List.of(), null),
                    new ActionParameterSpec("silent", "boolean", false, "Silent", List.of(), null)
            ));

    @Test
    void acceptsExactTypesEnumAndOmittedOptionalProperties() {
        JsonObject requiredOnly = new JsonObject();
        requiredOnly.addProperty("target", "station");
        assertTrue(matches(requiredOnly));

        JsonObject complete = requiredOnly.deepCopy();
        complete.addProperty("distance", 12.5);
        complete.addProperty("silent", true);
        assertTrue(matches(complete));
    }

    @Test
    void rejectsMissingRequiredNullAndUndeclaredProperties() {
        assertFalse(matches(new JsonObject()), "a required property must be present");

        JsonObject explicitNull = new JsonObject();
        explicitNull.add("target", null);
        assertFalse(matches(explicitNull), "null never satisfies a primitive parameter type");

        JsonObject extra = validArguments();
        extra.addProperty("unexpected", "value");
        assertFalse(matches(extra), "additional properties are not part of the offered contract");
    }

    @Test
    void normalizesOptionalNullPropertiesToOmission() {
        JsonObject arguments = validArguments();
        arguments.add("distance", null);
        arguments.add("silent", null);

        assertTrue(matches(arguments));
        assertFalse(arguments.has("distance"));
        assertFalse(arguments.has("silent"));
    }

    @Test
    void rejectsTypeCoercionAndUnknownEnumValues() {
        JsonObject numericString = validArguments();
        numericString.addProperty("distance", "12.5");
        assertFalse(matches(numericString), "a numeric string is not a JSON number");

        JsonObject booleanString = validArguments();
        booleanString.addProperty("silent", "true");
        assertFalse(matches(booleanString), "a boolean string is not a JSON boolean");

        JsonObject wrongEnum = validArguments();
        wrongEnum.addProperty("target", "Station");
        assertFalse(matches(wrongEnum), "enum matching is exact and case-sensitive");
    }

    @Test
    void rejectsUnofferedCallAndTheWholeBatchWhenOneCallIsInvalid() {
        LlmToolInvocation unknown = new LlmToolInvocation("call-1", "jump", new JsonObject());
        assertFalse(ToolCallValidator.validateAndNormalizeExactSchemas(List.of(unknown), List.of(TOOL)));

        JsonObject optionalNull = validArguments();
        optionalNull.add("distance", null);
        LlmToolInvocation valid = new LlmToolInvocation("call-2", TOOL.name(), optionalNull);
        LlmToolInvocation invalid = new LlmToolInvocation("call-3", TOOL.name(), new JsonObject());
        assertFalse(ToolCallValidator.validateAndNormalizeExactSchemas(List.of(valid, invalid), List.of(TOOL)),
                "one invalid call rejects the complete provider response");
        assertTrue(optionalNull.has("distance"), "a rejected response must not be partially normalized");
    }

    @Test
    void requestAndDefinitionFreezeTheOfferedSchemaSnapshot() {
        List<ActionParameterSpec> mutableParameters = new ArrayList<>(TOOL.parameters());
        LlmToolDefinition definition = new LlmToolDefinition("navigate", "Navigate", "", mutableParameters);
        List<LlmToolDefinition> mutableTools = new ArrayList<>(List.of(definition));
        LlmRequest request = new LlmRequest("request-1", List.of(), mutableTools, PromptCacheProfile.COMMANDER);

        mutableParameters.clear();
        mutableTools.clear();

        assertEquals(3, request.tools().get(0).parameters().size());
        assertTrue(ToolCallValidator.validateAndNormalizeExactSchemas(
                List.of(new LlmToolInvocation("call-1", "navigate", validArguments())), request.tools()));
    }

    private static boolean matches(JsonObject arguments) {
        return ToolCallValidator.validateAndNormalizeExactSchemas(
                List.of(new LlmToolInvocation("call-1", TOOL.name(), arguments)), List.of(TOOL));
    }

    private static JsonObject validArguments() {
        JsonObject arguments = new JsonObject();
        arguments.addProperty("target", "station");
        return arguments;
    }
}
