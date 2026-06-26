package elite.intel.gameapi;

/**
 * Subscriber-prepared narration payload for LLM speech paths. Publishers provide already filtered data,
 * narration instructions, and a neutral topic id so consumers can route memory/prompt context without
 * depending on companion-specific model types.
 */
public class SensorDataEvent {

    public static final String TOPIC_NAVIGATION = "navigation";
    public static final String TOPIC_TRADE = "trade";
    public static final String TOPIC_SYSTEM = "system";

    private final String sensorData;
    private final String instructions;
    private final String topic;

    /** Creates sensor narration with the default system topic for legacy publishers. */
    public SensorDataEvent(String sensorData, String instructions) {
        this(sensorData, instructions, TOPIC_SYSTEM);
    }

    /** Creates sensor narration with an explicit neutral topic id such as {@link #TOPIC_NAVIGATION}. */
    public SensorDataEvent(String sensorData, String instructions, String topic) {
        this.instructions = instructions;
        this.sensorData = sensorData;
        this.topic = topic == null || topic.isBlank() ? TOPIC_SYSTEM : topic.trim();
    }

    public String getSensorData() {
        return "sensorData: " + sensorData;
    }

    public String getInstructions() {
        return this.instructions;
    }

    /** Returns the neutral topic id supplied by the subscriber layer. */
    public String getTopic() {
        return topic;
    }
}
