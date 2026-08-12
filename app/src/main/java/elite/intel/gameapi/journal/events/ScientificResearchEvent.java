package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.StringUtls;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Material donated to a research station. It leaves the inventory and nothing comes back.
 */
public class ScientificResearchEvent extends BaseEvent {

    @SerializedName("MarketID")
    private long marketId;

    @SerializedName("Name")
    private String name;

    @SerializedName("Name_Localised")
    private String nameLocalised;

    @SerializedName("Category")
    private String category;

    @SerializedName("Count")
    private int count;

    public ScientificResearchEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ScientificResearch");
        ScientificResearchEvent event = GsonFactory.getGson().fromJson(json, ScientificResearchEvent.class);
        this.marketId = event.marketId;
        this.name = event.name;
        this.nameLocalised = event.nameLocalised;
        this.category = event.category;
        this.count = event.count;
    }

    @Override
    public String getEventType() {
        return "ScientificResearch";
    }

    /**
     * Inventory bookkeeping.
     */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Donated engineering material to a research station; carries the material name, category, and amount handed over.";
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

    /**
     * The name to show: the localised name when present, otherwise the symbol capitalised.
     */
    public String getDisplayName() {
        return nameLocalised != null ? nameLocalised : StringUtls.capitalizeWords(name);
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp(), useLocalTime);
    }
}
