package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Boarding a ship or SRV from on foot. Read for one thing: {@code SRV:true} with the vehicle's {@code ID} is how a
 * commander who logged in on foot climbs back into a Nomad that was already out - no launch event follows.
 */
public class EmbarkEvent extends BaseEvent {
    @SerializedName("SRV")
    private boolean srv;

    @SerializedName("Taxi")
    private boolean taxi;

    @SerializedName("Multicrew")
    private boolean multicrew;

    @SerializedName("ID")
    private int id;

    public EmbarkEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Embark");
        EmbarkEvent event = GsonFactory.getGson().fromJson(json, EmbarkEvent.class);
        this.srv = event.srv;
        this.taxi = event.taxi;
        this.multicrew = event.multicrew;
        this.id = event.id;
    }

    @Override
    public String getEventType() {
        return "Embark";
    }

    /**
     * Fires on every hop of an exobiology run; nothing worth remembering.
     */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Boarded the ship or an SRV from on foot.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public boolean isSrv() {
        return srv;
    }

    public boolean isTaxi() {
        return taxi;
    }

    public boolean isMulticrew() {
        return multicrew;
    }

    public int getId() {
        return id;
    }
}
