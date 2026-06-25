package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

public class CargoTransferEvent extends BaseEvent {
    @SerializedName("Transfers")
    private List<Transfer> transfers;

    public CargoTransferEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CargoTransfer");
        CargoTransferEvent event = GsonFactory.getGson().fromJson(json, CargoTransferEvent.class);
        this.transfers = event.transfers;
    }

    @Override
    public String getEventType() {
        return "CargoTransfer";
    }

    @Override
    public String llmDescription() {
        return "Transferred cargo between the ship and an SRV or fleet carrier; carries the commodities and the direction.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner("Cargo transfer detected: ")
                .add("transfers=" + transfers)
                .toString();
    }

    public static class Transfer {
        @SerializedName("Type")
        private String type;

        @SerializedName("Type_Localised")
        private String typeLocalised;

        @SerializedName("Count")
        private int count;

        @SerializedName("Direction")
        private String direction;

        public String getType() {
            return type;
        }

        public String getTypeLocalised() {
            return typeLocalised;
        }

        public int getCount() {
            return count;
        }

        public String getDirection() {
            return direction;
        }
    }
}