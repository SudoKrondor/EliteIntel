package elite.intel.ai.mouth.edge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/** Strictly maps the Edge voice-list JSON into typed voice records. */
final class EdgeVoiceListParser {
    private EdgeVoiceListParser() {
    }

    static List<EdgeVoice> parse(String json) throws EdgeProtocolException {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) {
                throw new EdgeProtocolException("Edge voice-list response is not an array");
            }
            JsonArray array = root.getAsJsonArray();
            List<EdgeVoice> voices = new ArrayList<>(array.size());
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    throw new EdgeProtocolException("Edge voice-list entry is not an object");
                }
                JsonObject voice = element.getAsJsonObject();
                voices.add(new EdgeVoice(
                        optionalString(voice, "Name"),
                        requiredString(voice, "ShortName"),
                        requiredString(voice, "Gender"),
                        requiredString(voice, "Locale"),
                        optionalString(voice, "SuggestedCodec")));
            }
            if (voices.isEmpty()) {
                throw new EdgeProtocolException("Edge voice-list response is empty");
            }
            return List.copyOf(voices);
        } catch (EdgeProtocolException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EdgeProtocolException("Malformed Edge voice-list response", e);
        }
    }

    private static String requiredString(JsonObject object, String field) throws EdgeProtocolException {
        String value = optionalString(object, field);
        if (value == null || value.isBlank()) {
            throw new EdgeProtocolException("Edge voice-list entry is missing " + field);
        }
        return value;
    }

    private static String optionalString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}
