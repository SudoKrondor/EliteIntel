package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class CarrierDepositFuelEvent extends BaseEvent {

    @SerializedName("CarrierID")
    private long carrierID;

    @SerializedName("Amount")
    private int amount;

    @SerializedName("Total")
    private int total;

    public CarrierDepositFuelEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CarrierDepositFuel");

        CarrierDepositFuelEvent event = GsonFactory.getGson().fromJson(json, CarrierDepositFuelEvent.class);
        this.carrierID = event.carrierID;
        this.amount = event.amount;
        this.total = event.total;
    }

    @Override
    public String getEventType() {
        return "CarrierDepositFuel";
    }

    @Override
    public String llmDescription() {
        return "Deposited tritium fuel into the fleet carrier; carries the amount and the new fuel total.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getCarrierID() {
        return carrierID;
    }

    public int getAmount() {
        return amount;
    }

    public int getTotal() {
        return total;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "CarrierDepositFuelEvent[", "]")
                .add("carrierID=" + carrierID)
                .add("amount=" + amount)
                .add("total=" + total)
                .add("timestamp='" + getFormattedTimestamp(false) + "'")
                .toString();
    }
}