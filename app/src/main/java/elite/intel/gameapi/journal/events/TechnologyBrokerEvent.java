package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

/**
 * A tech-broker unlock. {@code Materials} is the engineering-material half of the price and is the
 * only half this event's inventory handling touches — {@code Commodities} is cargo, tracked
 * elsewhere, and {@code ItemsUnlocked} is what was bought rather than what was spent.
 */
public class TechnologyBrokerEvent extends BaseEvent {

    @SerializedName("BrokerType")
    private String brokerType;

    @SerializedName("MarketID")
    private long marketId;

    @SerializedName("ItemsUnlocked")
    private List<UnlockedItem> itemsUnlocked;

    @SerializedName("Materials")
    private List<Material> materials;

    public TechnologyBrokerEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "TechnologyBroker");
        TechnologyBrokerEvent event = GsonFactory.getGson().fromJson(json, TechnologyBrokerEvent.class);
        this.brokerType = event.brokerType;
        this.marketId = event.marketId;
        this.itemsUnlocked = event.itemsUnlocked;
        this.materials = event.materials;
    }

    @Override
    public String getEventType() {
        return "TechnologyBroker";
    }

    /**
     * An unlock is a milestone worth a remark.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Unlocked a module or weapon at a technology broker; carries what was unlocked and the materials and commodities it cost.";
    }

    @Override
    public String memorySummary() {
        if (itemsUnlocked == null || itemsUnlocked.isEmpty()) return "";
        UnlockedItem first = itemsUnlocked.getFirst();
        String item = first.getDisplayName();
        if (item == null || item.isBlank()) return "";
        return itemsUnlocked.size() == 1
                ? "unlocked " + item + " at a technology broker"
                : "unlocked " + item + " and " + (itemsUnlocked.size() - 1) + " more at a technology broker";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getBrokerType() {
        return brokerType;
    }

    public long getMarketId() {
        return marketId;
    }

    public List<UnlockedItem> getItemsUnlocked() {
        return itemsUnlocked;
    }

    /**
     * The engineering materials the unlock cost.
     */
    public List<Material> getMaterials() {
        return materials;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp(), useLocalTime);
    }

    public static class UnlockedItem {

        @SerializedName("Name")
        private String name;

        @SerializedName("Name_Localised")
        private String nameLocalised;

        public String getName() {
            return name;
        }

        public String getNameLocalised() {
            return nameLocalised;
        }

        public String getDisplayName() {
            return nameLocalised != null ? nameLocalised : name;
        }
    }

    public static class Material {

        @SerializedName("Name")
        private String name;

        @SerializedName("Name_Localised")
        private String nameLocalised;

        @SerializedName("Category")
        private String category;

        @SerializedName("Count")
        private int count;

        public String getName() {
            return name;
        }

        public String getNameLocalised() {
            return nameLocalised;
        }

        public String getCategory() {
            return category;
        }

        public int getCount() {
            return count;
        }
    }
}
