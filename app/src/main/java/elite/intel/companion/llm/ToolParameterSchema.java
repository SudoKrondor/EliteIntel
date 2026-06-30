package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;

import java.util.List;

/**
 * Renders a companion tool's {@link ActionParameterSpec} list into a parameter schema, shared by every native
 * tool-calling adapter. OpenAI-compatible {@code function.parameters} and Anthropic {@code input_schema} both
 * use the same standard JSON-Schema object (lowercase types) via {@link #jsonSchemaObject}; Gemini's
 * {@code functionDeclarations[].parameters} uses the same shape but uppercase OpenAPI type names, so its
 * adapter builds the object itself and only reuses {@link #describeParameter}.
 * <p>
 * Examples and the extraction hint fold into each property's {@code description}, because none of these
 * schemas has a dedicated field for them (mirroring the legacy {@code CommandParamRules} format); otherwise
 * the companion model never sees the "'target drive' -> drive" style hints the legacy prompt relied on.
 */
final class ToolParameterSchema {

    private ToolParameterSchema() {
    }

    /**
     * Standard JSON-Schema object ({@code {type:object, properties, required}}) for OpenAI-compatible and Anthropic.
     */
    static JsonObject jsonSchemaObject(List<ActionParameterSpec> parameters) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        for (ActionParameterSpec p : parameters) {
            JsonObject prop = new JsonObject();
            prop.addProperty("type", p.getType());
            prop.addProperty("description", describeParameter(p));
            // A closed value set becomes a JSON-Schema enum so the model is constrained to a valid value.
            List<String> enumValues = p.getEnumValues();
            if (!enumValues.isEmpty()) {
                JsonArray enumArray = new JsonArray();
                enumValues.forEach(enumArray::add);
                prop.add("enum", enumArray);
            }
            properties.add(p.getName(), prop);
            if (p.isRequired()) {
                required.add(p.getName());
            }
        }
        schema.add("properties", properties);
        if (!required.isEmpty()) {
            schema.add("required", required);
        }
        return schema;
    }

    /**
     * Folds a parameter's examples and extraction hint into its schema {@code description}. OpenAI-style
     * function-calling schemas have no dedicated fields for these, so they are appended to the description.
     */
    static String describeParameter(ActionParameterSpec p) {
        StringBuilder description = new StringBuilder(p.getDescription());
        List<String> examples = p.getExamples();
        if (!examples.isEmpty()) {
            description.append(" E.g.: ").append(String.join(", ", examples));
        }
        if (p.getExtractionHint() != null && !p.getExtractionHint().isBlank()) {
            description.append(" Hint: ").append(p.getExtractionHint());
        }
        return description.toString().strip();
    }
}
