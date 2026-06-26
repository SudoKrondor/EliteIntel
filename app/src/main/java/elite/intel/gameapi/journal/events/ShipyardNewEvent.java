package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class ShipyardNewEvent extends BaseEvent {

    @SerializedName("ShipType")
    private String shipType;

    @SerializedName("NewShipID")
    private int newShipID;

    public ShipyardNewEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ShipyardNew");
        ShipyardNewEvent event = GsonFactory.getGson().fromJson(json, ShipyardNewEvent.class);
        this.shipType = event.shipType;
        this.newShipID = event.newShipID;
    }

    @Override
    public String getEventType() {
        return "ShipyardNew";
    }

    /** A new ship joined the fleet. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "A newly purchased ship was delivered and made active; carries the ship type and id.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getShipType() {
        return shipType;
    }

    public int getNewShipID() {
        return newShipID;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "ShipyardNew: ", "")
                .add("shipType='" + shipType + "'")
                .add("newShipID=" + newShipID)
                .toString();
    }
}
