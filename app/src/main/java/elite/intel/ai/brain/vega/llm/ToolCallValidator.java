package elite.intel.ai.brain.vega.llm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.ai.brain.vega.model.llm.LlmToolInvocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates parsed tool calls against the exact provider-neutral tool snapshot offered in the same LLM request.
 * The contract is deliberately strict and non-coercing: required properties must exist, JSON primitive types and
 * enum values must match exactly, and undeclared properties reject parameterized calls before any call can reach
 * execution. Arguments hallucinated for a parameterless function are ignored because its empty schema makes every
 * value inapplicable. A provider's explicit {@code null} for an optional property is also removed. Normalization is
 * atomic: it happens only after the complete response validates. Required {@code null} remains invalid.
 */
final class ToolCallValidator {

    private ToolCallValidator() {
    }

    /**
     * Validates the complete invocation set and atomically removes optional null properties on success. No
     * invocation is mutated when any call is invalid.
     */
    static boolean validateAndNormalizeExactSchemas(
            List<LlmToolInvocation> invocations,
            List<LlmToolDefinition> offeredTools
    ) {
        if (invocations == null || invocations.isEmpty() || offeredTools == null) {
            return false;
        }
        Map<String, Map<String, ActionParameterSpec>> schemasByTool = indexSchemas(offeredTools);
        if (schemasByTool == null) {
            return false;
        }
        List<List<String>> propertiesToRemoveByInvocation = new ArrayList<>(invocations.size());
        for (LlmToolInvocation invocation : invocations) {
            if (invocation == null || invocation.name() == null || invocation.arguments() == null) {
                return false;
            }
            Map<String, ActionParameterSpec> schema = schemasByTool.get(invocation.name());
            List<String> propertiesToRemove = normalizableProperties(invocation.arguments(), schema);
            if (propertiesToRemove == null) {
                return false;
            }
            propertiesToRemoveByInvocation.add(propertiesToRemove);
        }
        for (int i = 0; i < invocations.size(); i++) {
            JsonObject arguments = invocations.get(i).arguments();
            propertiesToRemoveByInvocation.get(i).forEach(arguments::remove);
        }
        return true;
    }

    /** Builds a unique name/parameter index; an ambiguous or malformed offered snapshot is never executable. */
    private static Map<String, Map<String, ActionParameterSpec>> indexSchemas(List<LlmToolDefinition> offeredTools) {
        Map<String, Map<String, ActionParameterSpec>> schemasByTool = new HashMap<>();
        for (LlmToolDefinition tool : offeredTools) {
            if (tool == null || tool.name() == null || tool.name().isBlank() || tool.parameters() == null) {
                return null;
            }
            Map<String, ActionParameterSpec> parameters = new HashMap<>();
            for (ActionParameterSpec parameter : tool.parameters()) {
                if (parameter == null || !validSpec(parameter)
                        || parameters.putIfAbsent(parameter.getName(), parameter) != null) {
                    return null;
                }
            }
            if (schemasByTool.putIfAbsent(tool.name(), Map.copyOf(parameters)) != null) {
                return null;
            }
        }
        return schemasByTool;
    }

    /** Returns harmless properties to discard, or {@code null} when the call violates its declared schema. */
    private static List<String> normalizableProperties(
            JsonObject arguments,
            Map<String, ActionParameterSpec> schema
    ) {
        if (schema == null) {
            return null;
        }
        if (schema.isEmpty()) {
            return List.copyOf(arguments.keySet());
        }
        return optionalNullProperties(arguments, schema);
    }

    private static boolean validSpec(ActionParameterSpec parameter) {
        try {
            parameter.validate();
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    /** Returns optional null property names to remove, or {@code null} when the arguments are invalid. */
    private static List<String> optionalNullProperties(
            JsonObject arguments,
            Map<String, ActionParameterSpec> schema
    ) {
        List<String> optionalNulls = new ArrayList<>();
        for (Map.Entry<String, JsonElement> argument : arguments.entrySet()) {
            ActionParameterSpec parameter = schema.get(argument.getKey());
            if (parameter == null) {
                return null;
            }
            JsonElement value = argument.getValue();
            if (value == null || value.isJsonNull()) {
                if (parameter.isRequired()) {
                    return null;
                }
                optionalNulls.add(argument.getKey());
            } else if (!matchesParameter(value, parameter)) {
                return null;
            }
        }
        boolean allRequiredPresent = schema.values().stream()
                .filter(ActionParameterSpec::isRequired)
                .allMatch(parameter -> arguments.has(parameter.getName()));
        return allRequiredPresent ? List.copyOf(optionalNulls) : null;
    }

    private static boolean matchesParameter(JsonElement value, ActionParameterSpec parameter) {
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        boolean typeMatches = switch (parameter.getType()) {
            case "string" -> primitive.isString();
            case "number" -> primitive.isNumber();
            case "boolean" -> primitive.isBoolean();
            default -> false;
        };
        if (!typeMatches) {
            return false;
        }
        List<String> enumValues = parameter.getEnumValues();
        return enumValues.isEmpty() || primitive.isString() && enumValues.contains(primitive.getAsString());
    }
}
