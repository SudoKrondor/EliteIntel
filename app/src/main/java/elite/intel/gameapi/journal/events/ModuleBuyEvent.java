package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Outfitting purchase. Net cost is {@code BuyPrice} minus any {@code SellPrice}
 * received for the module that was replaced (0 when no module was sold back).
 */
public class ModuleBuyEvent extends BaseEvent {

    @SerializedName("Slot")
    private String slot;
    @SerializedName("BuyItem")
    private String buyItem;
    @SerializedName("BuyItem_Localised")
    private String buyItemLocalised;
    @SerializedName("BuyPrice")
    private long buyPrice;
    @SerializedName("SellItem")
    private String sellItem;
    @SerializedName("SellPrice")
    private long sellPrice;
    @SerializedName("MarketID")
    private long marketID;

    public ModuleBuyEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ModuleBuy");
        ModuleBuyEvent e = GsonFactory.getGson().fromJson(json, ModuleBuyEvent.class);
        this.slot = e.slot;
        this.buyItem = e.buyItem;
        this.buyItemLocalised = e.buyItemLocalised;
        this.buyPrice = e.buyPrice;
        this.sellItem = e.sellItem;
        this.sellPrice = e.sellPrice;
        this.marketID = e.marketID;
    }

    @Override
    public String getEventType() {
        return "ModuleBuy";
    }

    @Override
    public String llmDescription() {
        return "Bought and fitted a module at outfitting; carries the module and the price paid.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getSlot() {
        return slot;
    }

    public String getBuyItem() {
        return buyItem;
    }

    public String getBuyItemLocalised() {
        return buyItemLocalised;
    }

    public long getBuyPrice() {
        return buyPrice;
    }

    public String getSellItem() {
        return sellItem;
    }

    public long getSellPrice() {
        return sellPrice;
    }

    public long getMarketID() {
        return marketID;
    }
}
