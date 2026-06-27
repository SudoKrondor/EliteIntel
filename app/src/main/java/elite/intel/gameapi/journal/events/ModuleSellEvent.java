package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Outfitting sale. {@code SellPrice} is an inflow.
 */
public class ModuleSellEvent extends BaseEvent {

    @SerializedName("Slot")
    private String slot;
    @SerializedName("SellItem")
    private String sellItem;
    @SerializedName("SellItem_Localised")
    private String sellItemLocalised;
    @SerializedName("SellPrice")
    private long sellPrice;
    @SerializedName("MarketID")
    private long marketID;

    public ModuleSellEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ModuleSell");
        ModuleSellEvent e = GsonFactory.getGson().fromJson(json, ModuleSellEvent.class);
        this.slot = e.slot;
        this.sellItem = e.sellItem;
        this.sellItemLocalised = e.sellItemLocalised;
        this.sellPrice = e.sellPrice;
        this.marketID = e.marketID;
    }

    @Override
    public String getEventType() {
        return "ModuleSell";
    }

    /** Routine outfitting sale; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Sold a module at outfitting; carries the module and the sale value.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getSlot() {
        return slot;
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

    public long getMarketID() {
        return marketID;
    }
}
