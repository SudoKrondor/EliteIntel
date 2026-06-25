package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class MiningRefinedEvent extends BaseEvent {
    @SerializedName("Type")
    private String type;

    @SerializedName("Type_Localised")
    private String typeLocalised;

    public MiningRefinedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "MiningRefined");
        MiningRefinedEvent event = GsonFactory.getGson().fromJson(json, MiningRefinedEvent.class);
        this.type = event.type;
        this.typeLocalised = event.typeLocalised;
    }

    @Override
    public String getEventType() {
        return "MiningRefined";
    }

    @Override
    public String llmDescription() {
        return "Refined a unit of mined material into the cargo hold; carries the commodity. Fires often while mining.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getType() {
        return type;
    }

    public String getTypeLocalised() {
        return typeLocalised;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MiningRefinedEvent.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("typeLocalised='" + typeLocalised + "'")
                .add("timestamp='" + timestamp + "'")
                .add("eventName='" + event + "'")
                .add("endOfLife=" + endOfLife)
                .toString();
    }
}