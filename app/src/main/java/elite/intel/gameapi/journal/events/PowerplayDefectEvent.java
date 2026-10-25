package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * The commander defected from one power to another.
 */
public class PowerplayDefectEvent extends BaseEvent {

    @SerializedName("FromPower")
    private String fromPower;

    @SerializedName("ToPower")
    private String toPower;

    public PowerplayDefectEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofMinutes(30), "PowerplayDefect");
        PowerplayDefectEvent e = GsonFactory.getGson().fromJson(json, PowerplayDefectEvent.class);
        this.fromPower = e.fromPower;
        this.toPower = e.toPower;
    }

    @Override
    public String getEventType() {
        return "PowerplayDefect";
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
        return "You defected from one Powerplay power to another; carries both powers' names.";
    }

    @Override
    public String memorySummary() {
        return toPower == null || toPower.isBlank() ? "" : "defected from " + fromPower + " to " + toPower;
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getFromPower() {
        return fromPower;
    }

    public String getToPower() {
        return toPower;
    }
}
