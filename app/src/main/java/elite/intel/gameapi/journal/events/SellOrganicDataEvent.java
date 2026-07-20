package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

/**
 * A sale of exobiology scan data at Vista Genomics.
 *
 * <p>WHY this event computes its own totals ({@link #getTotalValue()}, {@link #getTotalBonus()},
 * {@link #getTotalCredits()}, {@link #getSaleByGenus()}) rather than leaving them to the caller:
 * the event is serialized straight into the companion's narration payload, and a per-row
 * {@code Value}/{@code Bonus} table asks the LLM to do the arithmetic. It does that badly. A real
 * ten-row sale worth 221,670,000 credits was announced as 132,500,000 because the model collapsed
 * duplicate variant rows while summing. Every figure the narration needs is precomputed here.
 */
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

    /**
     * Per-genus roll-up of a sale, so the narrator never has to add up rows itself.
     */
    public record GenusSale(String genus, int samples, long credits) {
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

    /** Sold exobiology data; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Sold exobiology (organic) scan data at Vista Genomics; carries the species sold and the total value.";
    }

    @Override
    public String memorySummary() {
        if (bioData == null || bioData.isEmpty()) {
            return "";
        }
        // WHY samples and species are counted separately: one species is sold once per sample
        // collected, so a ten-row sale is routinely five species. Reporting rows as species
        // inflates the count in the commander's remembered history.
        long samples = bioData.size();
        long species = bioData.stream().map(SellOrganicDataEvent::speciesOf).distinct().count();
        return "sold exobiology data: " + samples + (samples == 1 ? " sample" : " samples")
                + " across " + species + " species for " + getTotalCredits() + " credits";
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
     * Base sale value, excluding first-discovery bonuses.
     */
    public long getTotalValue() {
        return sum(BioData::getValue);
    }

    /**
     * First-discovery bonuses only; zero unless the commander was first to log the variant.
     */
    public long getTotalBonus() {
        return sum(BioData::getBonus);
    }

    /**
     * What the commander was actually paid: value plus first-discovery bonus.
     */
    public long getTotalCredits() {
        return getTotalValue() + getTotalBonus();
    }

    /**
     * Credits per genus, bonus included, in the order the genera appear in the sale.
     */
    public List<GenusSale> getSaleByGenus() {
        if (bioData == null) {
            return List.of();
        }
        Map<String, GenusSale> byGenus = new LinkedHashMap<>();
        for (BioData data : bioData) {
            String genus = genusOf(data);
            byGenus.merge(
                    genus,
                    new GenusSale(genus, 1, data.getValue() + data.getBonus()),
                    (a, b) -> new GenusSale(a.genus(), a.samples() + b.samples(), a.credits() + b.credits()));
        }
        return List.copyOf(byGenus.values());
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

    /**
     * Genus label for the breakdown, falling back to the species name when the genus fields are absent.
     */
    private static String genusOf(BioData data) {
        return firstPresent(data.getGenusLocalised(), data.getGenus(), speciesOf(data));
    }

    /**
     * Species identity for counting, falling back through the narrower names the journal may carry instead.
     */
    private static String speciesOf(BioData data) {
        return firstPresent(data.getSpeciesLocalised(), data.getSpecies(), data.getVariantLocalised(), data.getVariant());
    }

    /**
     * First populated name, or {@code "unknown"} when the journal names the organism in no field at all.
     * <p>WHY a placeholder rather than null: an unlabelled entry would be dropped from the serialized
     * payload, leaving a breakdown whose rows no longer add up to {@link #getTotalCredits()}.
     */
    private static String firstPresent(String... names) {
        for (String name : names) {
            if (name != null) return name;
        }
        return "unknown";
    }

    private long sum(ToLongFunction<BioData> field) {
        // WHY: a SellOrganicData with no BioData legitimately credits nothing - an empty sale, not an error.
        if (bioData == null) {
            return 0;
        }
        return bioData.stream().mapToLong(field).sum();
    }
}