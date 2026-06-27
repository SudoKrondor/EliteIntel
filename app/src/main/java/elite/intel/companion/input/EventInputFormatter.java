package elite.intel.companion.input;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import elite.intel.gameapi.journal.events.BaseEvent;

/**
 * Formats a journal event into the single text representation used as EVENT currentInput in both
 * prompts and companion memory. The envelope keeps the curated LLM-facing event meaning beside the
 * raw journal payload so later consumers see the same source text the thought saw.
 */
public final class EventInputFormatter {

    private static final String EVENT_TYPE = "event_type";
    private static final String DESCRIPTION = "description";
    private static final String PAYLOAD = "payload";
    private static final String PAYLOAD_RAW = "payload_raw";

    private EventInputFormatter() {
    }

    /** Returns the structured EVENT currentInput for prompt and memory use. */
    public static String format(BaseEvent event) {
        JsonObject envelope = new JsonObject();
        String eventType = event.getEventType();
        envelope.addProperty(EVENT_TYPE, eventType);
        envelope.addProperty(DESCRIPTION, description(event, eventType));

        String payload = event.toJson();
        if (payload == null || payload.isBlank()) {
            envelope.add(PAYLOAD, new JsonObject());
        } else {
            try {
                JsonElement parsed = JsonParser.parseString(payload);
                envelope.add(PAYLOAD, parsed);
            } catch (JsonSyntaxException invalidPayload) {
                envelope.addProperty(PAYLOAD_RAW, payload);
            }
        }
        return envelope.toString();
    }

    private static String description(BaseEvent event, String eventType) {
        String description = event.llmDescription();
        return description == null || description.isBlank() ? eventType : description;
    }
}
