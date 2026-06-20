package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.brain.actions.ActionParameterSpec;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime invocation context for a parameterized customCommand.
 * <p>
 * Stores the original LLM {@link JsonObject params} and resolves {@code ${paramName}} templates
 * against them. Bare direct references like {@code "${lat}"} preserve the original JSON type
 * (number, boolean) rather than coercing to string; mixed templates like {@code "prefix_${lat}"}
 * are always resolved to a string.
 */
public final class CustomCommandExecutionContext {

    /** Matches any {@code ${paramName}} occurrence within a template string. Used by validator and resolver. */
    static final Pattern PARAM_REF = Pattern.compile("\\$\\{([^}]+)\\}");
    /**
     * Matches a template that is entirely one bare {@code ${paramName}} reference.
     * Bare refs preserve the original JSON element type (number, boolean) in
     * {@link #resolveStepParams}; mixed templates (e.g. {@code "prefix_${x}"}) are always strings.
     */
    private static final Pattern BARE_REF = Pattern.compile("^\\s*\\$\\{([^}]+)\\}\\s*$");

    private final CustomCommandDefinition customCommand;
    private final JsonObject jsonParams;

    private CustomCommandExecutionContext(CustomCommandDefinition customCommand, JsonObject jsonParams) {
        this.customCommand = customCommand;
        this.jsonParams = jsonParams != null ? jsonParams : new JsonObject();
    }

    /**
     * Builds a context from the custom command definition and the LLM's raw {@code JsonObject params}.
     * An empty or null params object produces a context with no resolved values.
     */
    public static CustomCommandExecutionContext fromJson(CustomCommandDefinition customCommand, JsonObject json) {
        return new CustomCommandExecutionContext(customCommand, json);
    }

    /**
     * Validates that all required custom command parameters are present in the LLM params.
     *
     * @return a list of error messages (empty means all required params are present)
     */
    public List<String> validateRequiredParams() {
        List<String> errors = new ArrayList<>();
        for (ActionParameterSpec spec : customCommand.getParameters()) {
            if (!spec.isRequired()) continue;
            JsonElement element = jsonParams.get(spec.getName());
            boolean missing = element == null || element.isJsonNull()
                    || (element.isJsonPrimitive() && element.getAsString().isBlank());
            if (missing) {
                errors.add("Missing required parameter: " + spec.getName());
            }
        }
        return errors;
    }

    /**
     * Resolves a template string by replacing {@code ${paramName}} tokens with parameter values.
     * The result is always a {@code String}; use {@link #resolveStepParams} when the original
     * JSON type (number, boolean) must be preserved.
     *
     * @param template the template string, or {@code null} (returns {@code null})
     * @return the resolved string
     * @throws UnresolvedCustomCommandParamException if a referenced parameter is absent from the context
     */
    public String resolveString(String template) {
        if (template == null) return null;
        Matcher m = PARAM_REF.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String paramName = m.group(1);
            JsonElement element = jsonParams.get(paramName);
            if (element == null || element.isJsonNull()) {
                throw new UnresolvedCustomCommandParamException(paramName, template);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(element.getAsString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolves a step's param mapping into a {@link JsonObject} for passing to a nested command handler.
     * <p>
     * For a bare {@code "${paramName}"} template, the original {@link JsonElement} type is preserved
     * (e.g. a JSON number stays a number). For mixed templates, the result is a string.
     *
     * @param stepParamMapping mapping of handler-side param names to value templates (may be null)
     * @return resolved {@code JsonObject} (empty if mapping is null or empty)
     * @throws UnresolvedCustomCommandParamException if a referenced parameter is absent from the context
     */
    public JsonObject resolveStepParams(Map<String, String> stepParamMapping) {
        JsonObject result = new JsonObject();
        if (stepParamMapping == null || stepParamMapping.isEmpty()) return result;

        for (Map.Entry<String, String> entry : stepParamMapping.entrySet()) {
            String key = entry.getKey();
            String template = entry.getValue();
            if (template == null) {
                result.add(key, JsonNull.INSTANCE);
                continue;
            }
            Matcher bareMatch = BARE_REF.matcher(template);
            if (bareMatch.matches()) {
                // Preserve original JSON type for bare references.
                String paramName = bareMatch.group(1);
                JsonElement element = jsonParams.get(paramName);
                if (element == null || element.isJsonNull()) {
                    throw new UnresolvedCustomCommandParamException(paramName, template);
                }
                result.add(key, element);
            } else {
                result.addProperty(key, resolveString(template));
            }
        }
        return result;
    }
}
