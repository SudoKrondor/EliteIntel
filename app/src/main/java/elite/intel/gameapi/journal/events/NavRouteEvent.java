package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class NavRouteEvent extends BaseEvent {
    public NavRouteEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofHours(12), "NavRoute");
    }

    @Override
    public String getEventType() {
        return "NavRoute";
    }

    @Override
    public String llmDescription() {
        return "Plotted a multi-jump route; carries the ordered list of systems in the route.";
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
        return new StringJoiner("NavRoute detected: ")
                .add("timestamp='" + getTimestamp() + "'")
                .toString();
    }
}