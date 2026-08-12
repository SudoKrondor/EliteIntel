package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.StringUtls;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * A material trader exchange: {@code Paid} leaves the inventory, {@code Received} enters it. Both
 * sides carry their own category, so the two halves are read independently rather than from the
 * event's {@code TraderType}.
 */
public class MaterialTradeEvent extends BaseEvent {

    @SerializedName("MarketID")
    private long marketId;

    @SerializedName("TraderType")
    private String traderType;

    @SerializedName("Paid")
    private TradedMaterial paid;

    @SerializedName("Received")
    private TradedMaterial received;

    public MaterialTradeEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "MaterialTrade");
        MaterialTradeEvent event = GsonFactory.getGson().fromJson(json, MaterialTradeEvent.class);
        this.marketId = event.marketId;
        this.traderType = event.traderType;
        this.paid = event.paid;
        this.received = event.received;
    }

    @Override
    public String getEventType() {
        return "MaterialTrade";
    }

    /**
     * Inventory bookkeeping at a trader; the totals matter, the individual swap does not.
     */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Traded engineering materials at a material trader; carries what was given up and what was received, with amounts.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getMarketId() {
        return marketId;
    }

    public String getTraderType() {
        return traderType;
    }

    public TradedMaterial getPaid() {
        return paid;
    }

    public TradedMaterial getReceived() {
        return received;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp(), useLocalTime);
    }

    /**
     * One side of the exchange.
     */
    public static class TradedMaterial {

        @SerializedName("Material")
        private String material;

        @SerializedName("Material_Localised")
        private String materialLocalised;

        @SerializedName("Category")
        private String category;

        @SerializedName("Quantity")
        private int quantity;

        /**
         * The journal's non-localized material symbol, e.g. {@code imperialshielding}. The only stable key.
         */
        public String getMaterial() {
            return material;
        }

        public String getMaterialLocalised() {
            return materialLocalised;
        }

        public String getCategory() {
            return category;
        }

        public int getQuantity() {
            return quantity;
        }

        /**
         * The name to show: the localised name when the journal supplied one, otherwise the symbol
         * capitalised. Frontier omits {@code Material_Localised} whenever it would equal the raw value.
         */
        public String getDisplayName() {
            return materialLocalised != null ? materialLocalised : StringUtls.capitalizeWords(material);
        }
    }
}
