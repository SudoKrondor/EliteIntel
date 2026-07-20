package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.brain.actions.ActionParameterSpec;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandExecutionContextTest {

    // --- resolveString ---

    @Test
    void resolveStringReplacesKnownParam() {
        CustomCommandExecutionContext ctx = contextWithJson("{\"speed\": \"50\"}");
        assertEquals("set speed 50", ctx.resolveString("set speed ${speed}"));
    }

    @Test
    void resolveStringMultipleParams() {
        CustomCommandExecutionContext ctx = contextWithJson("{\"lat\": \"-10.5\", \"lon\": \"45.2\"}");
        assertEquals("navigate to -10.5, 45.2", ctx.resolveString("navigate to ${lat}, ${lon}"));
    }

    @Test
    void resolveStringNullTemplateReturnsNull() {
        CustomCommandExecutionContext ctx = contextWithJson("{}");
        assertNull(ctx.resolveString(null));
    }

    @Test
    void resolveStringNoRefsReturnedUnchanged() {
        CustomCommandExecutionContext ctx = contextWithJson("{}");
        assertEquals("hello pilot", ctx.resolveString("hello pilot"));
    }

    @Test
    void resolveStringThrowsForUnresolvedRef() {
        CustomCommandExecutionContext ctx = contextWithJson("{}");
        assertThrows(UnresolvedCustomCommandParamException.class,
                () -> ctx.resolveString("navigate to ${lat}"));
    }

    // --- resolveStepParams ---

    @Test
    void resolveStepParamsPreservesNumberTypeForBareRef() {
        CustomCommandExecutionContext ctx = contextWithJson("{\"lat\": -10.5, \"lon\": 45.2}");
        JsonObject result = ctx.resolveStepParams(Map.of("lat", "${lat}", "lon", "${lon}"));
        // Bare refs must preserve JSON number type, not coerce to string.
        assertFalse(result.get("lat").isJsonPrimitive() && result.get("lat").getAsJsonPrimitive().isString());
        assertEquals(-10.5, result.get("lat").getAsDouble(), 0.001);
        assertEquals(45.2, result.get("lon").getAsDouble(), 0.001);
    }

    @Test
    void resolveStepParamsPreservesBooleanTypeForBareRef() {
        CustomCommandExecutionContext ctx = contextWithJson("{\"enabled\": true}");
        JsonObject result = ctx.resolveStepParams(Map.of("state", "${enabled}"));
        assertTrue(result.get("state").getAsBoolean());
        assertTrue(result.get("state").getAsJsonPrimitive().isBoolean());
    }

    @Test
    void resolveStepParamsMixedTemplateBecomesString() {
        CustomCommandExecutionContext ctx = contextWithJson("{\"name\": \"gold\"}");
        JsonObject result = ctx.resolveStepParams(Map.of("key", "find_${name}"));
        assertEquals("find_gold", result.get("key").getAsString());
    }

    @Test
    void resolveStepParamsEmptyMappingReturnsEmptyObject() {
        CustomCommandExecutionContext ctx = contextWithJson("{\"lat\": 1.0}");
        JsonObject result = ctx.resolveStepParams(Map.of());
        assertTrue(result.entrySet().isEmpty());
    }

    @Test
    void resolveStepParamsNullMappingReturnsEmptyObject() {
        CustomCommandExecutionContext ctx = contextWithJson("{}");
        JsonObject result = ctx.resolveStepParams(null);
        assertTrue(result.entrySet().isEmpty());
    }

    @Test
    void resolveStepParamsThrowsForUnresolvedBareRef() {
        CustomCommandExecutionContext ctx = contextWithJson("{}");
        assertThrows(UnresolvedCustomCommandParamException.class,
                () -> ctx.resolveStepParams(Map.of("lat", "${lat}")));
    }

    // --- validateRequiredParams ---

    @Test
    void validateRequiredParamsNoErrorsWhenAllPresent() {
        CustomCommandDefinition customCommand = customCommandWithParams(List.of(
                new ActionParameterSpec("lat", "number", true, "", null, null),
                new ActionParameterSpec("lon", "number", true, "", null, null)
        ));
        CustomCommandExecutionContext ctx = CustomCommandExecutionContext.fromJson(customCommand,
                JsonParser.parseString("{\"lat\": -10.5, \"lon\": 45.2}").getAsJsonObject());
        assertTrue(ctx.validateRequiredParams().isEmpty());
    }

    @Test
    void validateRequiredParamsReportsEachMissingRequired() {
        CustomCommandDefinition customCommand = customCommandWithParams(List.of(
                new ActionParameterSpec("lat", "number", true, "", null, null),
                new ActionParameterSpec("lon", "number", true, "", null, null),
                new ActionParameterSpec("comment", "string", false, "", null, null)
        ));
        CustomCommandExecutionContext ctx = CustomCommandExecutionContext.fromJson(customCommand, new JsonObject());
        List<String> errors = ctx.validateRequiredParams();
        assertEquals(2, errors.size(), "Only required params should be reported missing");
        assertTrue(errors.stream().anyMatch(e -> e.contains("lat")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("lon")));
    }

    @Test
    void validateRequiredParamsIgnoresOptional() {
        CustomCommandDefinition customCommand = customCommandWithParams(List.of(
                new ActionParameterSpec("hint", "string", false, "", null, null)
        ));
        CustomCommandExecutionContext ctx = CustomCommandExecutionContext.fromJson(customCommand, new JsonObject());
        assertTrue(ctx.validateRequiredParams().isEmpty());
    }

    @Test
    void validateRequiredParamsNoErrorsForParamlessCustomCommand() {
        CustomCommandDefinition customCommand = customCommandWithParams(List.of());
        CustomCommandExecutionContext ctx = CustomCommandExecutionContext.fromJson(customCommand, new JsonObject());
        assertTrue(ctx.validateRequiredParams().isEmpty());
    }

    // --- helpers ---

    private static CustomCommandExecutionContext contextWithJson(String json) {
        CustomCommandDefinition customCommand = customCommandWithParams(List.of());
        JsonObject params = JsonParser.parseString(json).getAsJsonObject();
        return CustomCommandExecutionContext.fromJson(customCommand, params);
    }

    private static CustomCommandDefinition customCommandWithParams(List<ActionParameterSpec> params) {
        return new CustomCommandDefinition("test", "Test", "", "test phrase", params,
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hello", null)));
    }
}
