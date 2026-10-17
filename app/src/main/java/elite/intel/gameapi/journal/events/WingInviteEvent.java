package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Another commander invited us to join their wing. The journal carries only who sent it.
 */
public class WingInviteEvent extends BaseEvent {

    @SerializedName("Name")
    private String name;

    public WingInviteEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "WingInvite");
        WingInviteEvent event = GsonFactory.getGson().fromJson(json, WingInviteEvent.class);
        this.name = event.name;
    }

    @Override
    public String getEventType() {
        return "WingInvite";
    }

    /**
     * Another player is waiting on an answer.
     */
    @Override
    public Importance importance() {
        return Importance.HIGH;
    }

    @Override
    public String llmDescription() {
        return "Another commander invited us to join their wing; carries the inviting commander's name.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getName() {
        return name;
    }
}
