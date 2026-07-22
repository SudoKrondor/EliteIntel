package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.StringUtls;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class MaterialCollectedEvent extends BaseEvent {
    @SerializedName("Category")
    private String category;

    @SerializedName("Name")
    private String name;

    @SerializedName("Count")
    private int count;

    @SerializedName("Name_Localised")
    private String nameLocalised;

    public MaterialCollectedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "MaterialCollected");
        MaterialCollectedEvent event = GsonFactory.getGson().fromJson(json, MaterialCollectedEvent.class);
        this.category = event.category;
        this.name = event.name;
        this.count = event.count;
        this.nameLocalised = event.nameLocalised;
    }


    public String getNameLocalised() {
        return nameLocalised;
    }

    /**
     * The name to speak: the localised name when the journal supplied one, otherwise the raw name.
     * Frontier omits {@code Name_Localised} whenever it would equal the raw value, which is the common case
     * for raw elements (e.g. {@code carbon}), so callers must not assume it is present.
     */
    public String getDisplayName() {
        return nameLocalised != null ? nameLocalised : StringUtls.capitalizeWords(name);
    }

    @Override
    public String getEventType() {
        return "MaterialCollected";
    }

    /** High-frequency pickup telemetry. Ignore. */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Picked up engineering materials; carries the material category, name, and amount. Fires often.";
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

    public int getCount() {
        return count;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}