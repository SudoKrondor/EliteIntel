package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class MaterialsEvent extends BaseEvent {
    @SerializedName("Raw")
    private List<Material> raw;

    @SerializedName("Manufactured")
    private List<Material> manufactured;

    @SerializedName("Encoded")
    private List<Material> encoded;

    public MaterialsEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Materials");
        MaterialsEvent event = GsonFactory.getGson().fromJson(json, MaterialsEvent.class);
        this.raw = event.raw;
        this.manufactured = event.manufactured;
        this.encoded = event.encoded;
    }

    @Override
    public String getEventType() {
        return "Materials";
    }

    /** Materials snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "A snapshot of all engineering materials held (raw, manufactured, encoded); written at game load. Background list.";
    }

    @Override
    public String memorySummary() {
        int rawCount = raw == null ? 0 : raw.size();
        int manufacturedCount = manufactured == null ? 0 : manufactured.size();
        int encodedCount = encoded == null ? 0 : encoded.size();
        if (rawCount == 0 && manufacturedCount == 0 && encodedCount == 0) {
            return ""; // nothing held: no materials snapshot worth remembering
        }
        return "materials on hand: " + rawCount + " raw, " + manufacturedCount + " manufactured, " + encodedCount + " encoded";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public List<Material> getRaw() {
        return raw;
    }

    public List<Material> getManufactured() {
        return manufactured;
    }

    public List<Material> getEncoded() {
        return encoded;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    public static class Material {
        @SerializedName("Name")
        private String name;

        @SerializedName("Name_Localised")
        private String localisedName;

        @SerializedName("Count")
        private int count;

        public String getName() {
            return name;
        }

        public String getLocalisedName() {
            return localisedName;
        }

        public int getCount() {
            return count;
        }
    }
}