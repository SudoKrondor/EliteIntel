package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class NavRouteClearEvent extends BaseEvent {
    public NavRouteClearEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "NavRouteClear");
    }

    @Override
    public String getEventType() {
        return "NavRouteClear";
    }

    @Override
    public String llmDescription() {
        return "Cleared the plotted navigation route.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }
}