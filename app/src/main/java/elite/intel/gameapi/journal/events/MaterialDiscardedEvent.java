package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.StringUtls;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Material thrown away from the inventory panel. The mirror image of MaterialCollected.
 */
public class MaterialDiscardedEvent extends BaseEvent {

    @SerializedName("Category")
    private String category;

    @SerializedName("Name")
    private String name;

    @SerializedName("Name_Localised")
    private String nameLocalised;

    @SerializedName("Count")
    private int count;

    public MaterialDiscardedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "MaterialDiscarded");
        MaterialDiscardedEvent event = GsonFactory.getGson().fromJson(json, MaterialDiscardedEvent.class);
        this.category = event.category;
        this.name = event.name;
        this.nameLocalised = event.nameLocalised;
        this.count = event.count;
    }

    @Override
    public String getEventType() {
        return "MaterialDiscarded";
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
        return "Threw away engineering material; carries the material category, name, and amount discarded.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getNameLocalised() {
        return nameLocalised;
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
