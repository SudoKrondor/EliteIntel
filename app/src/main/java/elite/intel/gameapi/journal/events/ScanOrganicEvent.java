package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class ScanOrganicEvent extends BaseEvent {
    @SerializedName("ScanType")
    private String scanType;

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

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("Body")
    private Long body;

    public ScanOrganicEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ScanOrganic");
        ScanOrganicEvent event = GsonFactory.getGson().fromJson(json, ScanOrganicEvent.class);
        this.scanType = event.scanType;
        this.genus = event.genus;
        this.genusLocalised = event.genusLocalised;
        this.species = event.species;
        this.speciesLocalised = event.speciesLocalised;
        this.variant = event.variant;
        this.variantLocalised = event.variantLocalised;
        this.systemAddress = event.systemAddress;
        this.body = event.body;
    }

    @Override
    public String getEventType() {
        return "ScanOrganic";
    }

    @Override
    public String llmDescription() {
        return "Scanned a biological organism on foot; carries the species and genus and the scan stage (Log, Sample, or Analyse).";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getScanType() {
        return scanType;
    }

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

    public long getSystemAddress() {
        return systemAddress;
    }

    public Long getBody() {
        return body;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}