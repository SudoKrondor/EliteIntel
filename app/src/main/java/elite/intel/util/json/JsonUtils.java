package elite.intel.util.json;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A utility class for handling JSON operations with safety and default values.
 * This class provides static methods to extract values from {@link JsonObject}
 * in a null-safe manner, returning default empty values when necessary.
 * <p>
 * The purpose of this class is to simplify JSON parsing and handling common edge cases,
 * such as missing keys, `null` values, and unexpected types.
 * <p>
 * This class is designed to be used statically and cannot be instantiated.
 */
public final class JsonUtils {
    private static final Logger log = LogManager.getLogger(JsonUtils.class);

    private JsonUtils() {
        // Prevent instantiation
    }

    public static String getAsStringOrEmpty(JsonObject obj, String key) {
        if (obj == null || key == null) return "";
        if (!obj.has(key)) return "";
        var el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) {
            try {
                return el.getAsString();
            } catch (UnsupportedOperationException ignored) {
                // fallthrough
            }
        }
        log.debug("Expected string for key '{}' but got {}", key, el);
        return "";
    }

    /**
     * Repairs common LLM JSON output failures before parsing:
     * 1. Unquoted text_to_speech_response value (model forgot the quotes)
     * 2. Truncated text_to_speech_response string (num_predict limit hit mid-generation)
     */
    public static String repairLlmJson(String raw) {
        if (raw == null || raw.isEmpty()) return raw;

        // Repair 0: model returned plain text or buried JSON in preamble/postamble
        String working = raw.trim();
        if (!working.startsWith("{")) {
            int firstBrace = working.indexOf('{');
            int lastBrace = working.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                // JSON object buried in surrounding text - extract it
                working = working.substring(firstBrace, lastBrace + 1);
            } else {
                // No JSON at all - wrap as text_to_speech_response so the router can speak it
                String escaped = working
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                return "{\"text_to_speech_response\": \"" + escaped + "\"}";
            }
        }

        // Repair 1: truncated string - JSON cut off mid text_to_speech_response value (no closing "})
        if (!working.endsWith("}")) {
            java.util.regex.Pattern truncP = java.util.regex.Pattern.compile(
                    "\"text_to_speech_response\"\\s*:\\s*\"(.*?)$",
                    java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher truncM = truncP.matcher(working);
            if (truncM.find()) {
                String value = truncM.group(1)
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                return working.substring(0, truncM.start()) + "\"text_to_speech_response\": \"" + value + "\"}";
            }
        }

        // Repair 2: unquoted text_to_speech_response value (model output value without surrounding quotes)
        // Note: \\s*+ is possessive - prevents backtracking past whitespace so (?!\") correctly
        // rejects properly-quoted values like: "text_to_speech_response": "some text"
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\"text_to_speech_response\"\\s*:\\s*+(?!\")(.+?)\\s*}\\s*$",
                java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher m = p.matcher(working);
        if (m.find()) {
            String value = m.group(1)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return working.substring(0, m.start()) + "\"text_to_speech_response\": \"" + value + "\"}";
        }

        return working;
    }

    /**
     * Strips markdown and non-TTS-safe characters from the text_to_speech_response field
     * of a parsed LLM response object. Mutates the object in place and returns it.
     * Handles models that ignore formatting instructions and return markdown anyway.
     */
    public static JsonObject sanitizeTtsResponse(JsonObject obj) {
        if (obj == null || !obj.has("text_to_speech_response")) return obj;
        String raw = getAsStringOrEmpty(obj, "text_to_speech_response");
        obj.addProperty("text_to_speech_response", stripMarkdownForTts(raw));
        return obj;
    }

    /**
     * Strips markdown formatting and non-TTS-safe characters, leaving plain spoken text
     * safe for TTS while preserving localized text.
     */
    public static String stripMarkdownForTts(String text) {
        if (text == null || text.isEmpty()) return text;
        String s = text;

        // Em dash, en dash → comma space
        s = s.replace("\u2014", ", ").replace("\u2013", ", ");

        // Markdown links [label](url) → label
        s = s.replaceAll("\\[([^\\]]+)]\\([^)]*\\)", "$1");

        // Table rows (lines that start and end with |) → removed
        s = s.replaceAll("(?m)^\\|.*$", "");

        // ATX headers (# ## ###) → just the text
        s = s.replaceAll("(?m)^#{1,6}\\s*", "");

        // Bold/italic markers ** * __ _
        s = s.replaceAll("[*_]{1,3}([^*_\n]+)[*_]{1,3}", "$1");

        // Blockquote markers
        s = s.replaceAll("(?m)^>\\s*", "");

        // Bullet and numbered list markers at line start
        s = s.replaceAll("(?m)^[-*+•]\\s+", "");
        s = s.replaceAll("(?m)^\\d+[.):]\\s+", "");

        // Backticks (inline code / code fences)
        s = s.replaceAll("`+", "");

        // Horizontal rules
        s = s.replaceAll("(?m)^[-*_]{3,}\\s*$", "");

        // Drop controls, emojis, and standalone symbols while preserving localized text.
        s = s.replaceAll("[\\p{C}\\p{So}\\p{Sk}]+", " ");

        // Collapse blank lines left by removed table rows / headers
        s = s.replaceAll("(?m)^[ \\t]+$", "");
        s = s.replaceAll("\\n{3,}", "\n\n");

        // Collapse multiple spaces
        s = s.replaceAll("[ \\t]{2,}", " ");

        return s.trim();
    }

    public static JsonObject nullSaveJsonObject(JsonObject obj, String key, Logger log) {
        if (obj == null || key == null) return new JsonObject();
        if (!obj.has(key)) return new JsonObject();
        var el = obj.get(key);
        if (el == null || el.isJsonNull()) return new JsonObject();
        if (el.isJsonObject()) return el.getAsJsonObject();
        log.debug("Expected object for key '{}' but got {}", key, el);
        return new JsonObject();
    }

}
