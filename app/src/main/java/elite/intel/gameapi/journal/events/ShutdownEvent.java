package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class ShutdownEvent extends BaseEvent {
    public ShutdownEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Shutdown");
    }

    @Override
    public String getEventType() {
        return "Shutdown";
    }

    @Override
    public String llmDescription() {
        return "The game is shutting down and the play session ended.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner("Shutdown detected: ")
                .add("timestamp='" + getTimestamp() + "'")
                .toString();
    }
}