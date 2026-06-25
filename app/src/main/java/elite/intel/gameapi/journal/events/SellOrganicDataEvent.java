package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.TimestampFormatter;
import java.time.Duration;
import java.util.List;

public class SellOrganicDataEvent extends BaseEvent {
    public static class BioData {
        @SerializedName("Genus")
        private String genus;

        @SerializedName("Genus_Localised")
        private String genusLocalised;

        @SerializedName("Species")
        private String species;

        @SerializedName("Species_Localised")
        private String speciesLocalised;

        @SerializedName("Variant")
        private String variant;

        @SerializedName("Variant_Localised")
        private String variantLocalised;

        @SerializedName("Value")
        private long value;

        @SerializedName("Bonus")
        private long bonus;

        public String getGenus() {
            return genus;
        }

        public String getGenusLocalised() {
            return genusLocalised;
        }

        public String getSpecies() {
            return species;
        }

        public String getSpeciesLocalised() {
            return speciesLocalised;
        }

        public String getVariant() {
            return variant;
        }

        public String getVariantLocalised() {
            return variantLocalised;
        }

        public long getValue() {
            return value;
        }

        public long getBonus() {
            return bonus;
        }
    }

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("BioData")
    private List<BioData> bioData;

    public SellOrganicDataEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "SellOrganicData");
        SellOrganicDataEvent event = GsonFactory.getGson().fromJson(json, SellOrganicDataEvent.class);
        this.marketID = event.marketID;
        this.bioData = event.bioData;
    }

    @Override
    public String getEventType() {
        return "SellOrganicData";
    }

    @Override
    public String llmDescription() {
        return "Sold exobiology (organic) scan data at Vista Genomics; carries the species sold and the total value.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getMarketID() {
        return marketID;
    }

    public List<BioData> getBioData() {
        return bioData;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}