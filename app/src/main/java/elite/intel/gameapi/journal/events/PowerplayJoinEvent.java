package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * The commander pledged to a power.
 */
public class PowerplayJoinEvent extends BaseEvent {

    @SerializedName("Power")
    private String power;

    public PowerplayJoinEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofMinutes(30), "PowerplayJoin");
        PowerplayJoinEvent e = GsonFactory.getGson().fromJson(json, PowerplayJoinEvent.class);
        this.power = e.power;
    }

    @Override
    public String getEventType() {
        return "PowerplayJoin";
    }

    /**
     * A change of allegiance; memory context.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "You pledged to a Powerplay power; carries the power's name.";
    }

    @Override
    public String memorySummary() {
        return power == null || power.isBlank() ? "" : "pledged to " + power;
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getPower() {
        return power;
    }
}
