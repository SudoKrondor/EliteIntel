package elite.intel.companion.input;

import com.google.gson.JsonObject;
import elite.intel.gameapi.SensorDataEvent;

/**
 * Formats legacy sensor-analysis events into the single text representation used as companion EVENT
 * currentInput in both prompts and memory. SensorDataEvent is already subscriber-filtered, calculated,
 * and instruction-bearing; the companion should narrate from this envelope, not from raw journal data.
 */
public final class SensorInputFormatter {

    private static final String EVENT_TYPE = "event_type";
    private static final String TOPIC = "topic";
    private static final String DESCRIPTION = "description";
    private static final String INSTRUCTIONS = "instructions";
    private static final String PAYLOAD = "payload";

    private SensorInputFormatter() {
    }

    /** Returns the structured SensorData EVENT currentInput for prompt and memory use. */
    public static String format(SensorDataEvent event) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty(EVENT_TYPE, "SensorData");
        envelope.addProperty(TOPIC, event.getTopic());
        envelope.addProperty(DESCRIPTION, "Pre-digested event subscriber output for spoken companion narration.");
        envelope.addProperty(INSTRUCTIONS, event.getInstructions());
        envelope.addProperty(PAYLOAD, event.getSensorData());
        return envelope.toString();
    }
}
