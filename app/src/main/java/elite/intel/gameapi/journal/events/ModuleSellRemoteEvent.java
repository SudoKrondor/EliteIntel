package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Selling a stored (remote) module. {@code SellPrice} is an inflow.
 */
public class ModuleSellRemoteEvent extends BaseEvent {

    @SerializedName("StorageSlot")
    private int storageSlot;
    @SerializedName("SellItem")
    private String sellItem;
    @SerializedName("SellItem_Localised")
    private String sellItemLocalised;
    @SerializedName("SellPrice")
    private long sellPrice;
    @SerializedName("ServerId")
    private long serverId;

    public ModuleSellRemoteEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ModuleSellRemote");
        ModuleSellRemoteEvent e = GsonFactory.getGson().fromJson(json, ModuleSellRemoteEvent.class);
        this.storageSlot = e.storageSlot;
        this.sellItem = e.sellItem;
        this.sellItemLocalised = e.sellItemLocalised;
        this.sellPrice = e.sellPrice;
        this.serverId = e.serverId;
    }

    @Override
    public String getEventType() {
        return "ModuleSellRemote";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public int getStorageSlot() {
        return storageSlot;
    }

    public String getSellItem() {
        return sellItem;
    }

    public String getSellItemLocalised() {
        return sellItemLocalised;
    }

    public long getSellPrice() {
        return sellPrice;
    }

    public long getServerId() {
        return serverId;
    }
}
