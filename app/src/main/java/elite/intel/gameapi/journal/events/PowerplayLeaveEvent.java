package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * The commander left the power they were pledged to, and is pledged to none.
 */
public class PowerplayLeaveEvent extends BaseEvent {

    @SerializedName("Power")
    private String power;

    public PowerplayLeaveEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofMinutes(30), "PowerplayLeave");
        PowerplayLeaveEvent e = GsonFactory.getGson().fromJson(json, PowerplayLeaveEvent.class);
        this.power = e.power;
    }

    @Override
    public String getEventType() {
        return "PowerplayLeave";
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
        return "You left your Powerplay power and are no longer pledged; carries the power's name.";
    }

    @Override
    public String memorySummary() {
        return power == null || power.isBlank() ? "" : "left " + power;
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getPower() {
        return power;
    }
}
