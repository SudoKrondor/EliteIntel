package elite.intel.ai.brain.vega.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unwraps a response envelope a model wrapped its own spoken line in.
 *
 * <p>WHY this exists: the companion asks for prose in the {@code speak} tool's {@code text} argument, and the
 * argument is spoken verbatim. A model that has spent its life emitting response objects sometimes hands back
 * the line already packaged - {@code {"texttospeech_response": "Dex, Tubus Compagibus logged."}} - and the
 * commander then hears the wrapper read out. Seen from a cloud provider on a narration turn, but nothing about
 * it is provider-specific, so the guard sits on the shared path rather than in one adapter.
 *
 * <p>The rule is deliberately narrow: a spoken line never legitimately begins with <code>{</code> and ends with
 * <code>}</code>, so only text with that shape is touched at all, and text that fails to yield a payload is
 * returned unchanged rather than guessed at. Both a real JSON object and the loose {@code {key - value}} shape
 * a model improvises are recognised; the key is never trusted by name, because the model invents it.
 */
public final class SpokenTextEnvelope {

    /**
     * A single {@code key <separator> value} pair inside braces, for the improvised shapes that are not valid
     * JSON. The key must look like an identifier - no spaces - which prose inside braces would not.
     */
    private static final Pattern LOOSE_PAIR = Pattern.compile(
            "^\\{\\s*\"?([A-Za-z][A-Za-z0-9_.\\-]*)\"?\\s*[:=\\u2013\\u2014-]\\s*\"?(.*?)\"?\\s*}$",
            Pattern.DOTALL);

    private SpokenTextEnvelope() {
    }

    /**
     * Returns the payload the model wrapped, or {@code text} unchanged when it is not a wrapped line.
     */
    public static String unwrap(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.strip();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '{' || trimmed.charAt(trimmed.length() - 1) != '}') {
            return text;
        }
        // Parseable JSON is answered by the JSON reading alone: the loose pattern must never get a second
        // opinion on an object it would misread, such as a multi-field object whose fields it would run
        // together into one "payload".
        JsonObject object = asJsonObject(trimmed);
        if (object != null) {
            String payload = loneStringMember(object);
            return payload != null ? payload : text;
        }
        Matcher loose = LOOSE_PAIR.matcher(trimmed);
        return loose.matches() ? loose.group(2).strip() : text;
    }

    /**
     * The text read as a JSON object, or null when it does not parse as one.
     */
    private static JsonObject asJsonObject(String candidate) {
        try {
            JsonElement element = JsonParser.parseString(candidate);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException notJson) {
            return null;
        }
    }

    /**
     * The value of the object's single string member, or null when it holds anything else.
     */
    private static String loneStringMember(JsonObject object) {
        if (object.size() != 1) {
            return null;
        }
        Map.Entry<String, JsonElement> only = object.entrySet().iterator().next();
        JsonElement value = only.getValue();
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return null;
        }
        return value.getAsString().strip();
    }
}
