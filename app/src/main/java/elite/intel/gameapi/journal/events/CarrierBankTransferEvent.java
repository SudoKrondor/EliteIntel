package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Moving credits between the commander and a fleet carrier. {@code Deposit} moves
 * money from the commander to the carrier (outflow); {@code Withdraw} moves it back
 * (inflow). {@code PlayerBalance} is the resulting personal balance.
 */
public class CarrierBankTransferEvent extends BaseEvent {

    @SerializedName("CarrierID")
    private long carrierID;
    @SerializedName("Deposit")
    private long deposit;
    @SerializedName("Withdraw")
    private long withdraw;
    @SerializedName("PlayerBalance")
    private long playerBalance;
    @SerializedName("CarrierBalance")
    private long carrierBalance;

    public CarrierBankTransferEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CarrierBankTransfer");
        CarrierBankTransferEvent e = GsonFactory.getGson().fromJson(json, CarrierBankTransferEvent.class);
        this.carrierID = e.carrierID;
        this.deposit = e.deposit;
        this.withdraw = e.withdraw;
        this.playerBalance = e.playerBalance;
        this.carrierBalance = e.carrierBalance;
    }

    @Override
    public String getEventType() {
        return "CarrierBankTransfer";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getCarrierID() {
        return carrierID;
    }

    public long getDeposit() {
        return deposit;
    }

    public long getWithdraw() {
        return withdraw;
    }

    public long getPlayerBalance() {
        return playerBalance;
    }

    public long getCarrierBalance() {
        return carrierBalance;
    }
}
