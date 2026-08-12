package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

/**
 * Synthesis at the inventory panel — refuelling, repairing, or refilling ammo from raw stock.
 * {@code Materials} lists what it cost; the journal gives no category for those entries, which is
 * fine because a spend only has to match an existing row.
 */
public class SynthesisEvent extends BaseEvent {

    @SerializedName("Name")
    private String name;

    @SerializedName("Materials")
    private List<Material> materials;

    public SynthesisEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Synthesis");
        SynthesisEvent event = GsonFactory.getGson().fromJson(json, SynthesisEvent.class);
        this.name = event.name;
        this.materials = event.materials;
    }

    @Override
    public String getEventType() {
        return "Synthesis";
    }

    /**
     * Routine resupply from stock.
     */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Synthesised ammunition, fuel, or repairs from engineering materials; carries the recipe used and the materials it consumed.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    /**
     * The recipe, e.g. {@code "Sub-surface Displacement Ammo"}.
     */
    public String getName() {
        return name;
    }

    public List<Material> getMaterials() {
        return materials;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp(), useLocalTime);
    }

    public static class Material {

        @SerializedName("Name")
        private String name;

        @SerializedName("Name_Localised")
        private String nameLocalised;

        @SerializedName("Count")
        private int count;

        public String getName() {
            return name;
        }

        public String getNameLocalised() {
            return nameLocalised;
        }

        public int getCount() {
            return count;
        }
    }
}
