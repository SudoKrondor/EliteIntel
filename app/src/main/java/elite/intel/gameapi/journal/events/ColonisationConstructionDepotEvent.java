package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

/**
 * The full manifest of a colonisation construction site: what it still needs, what it has been given,
 * and how far along it is.
 * <p>
 * The game republishes this in full every 15-30 seconds for as long as the ship is on the pad, and again
 * within a second or two of every {@code Docked} at the depot, so there is never a need to ask for it -
 * arriving IS the refresh. Anything reading it has to be idempotent for the same reason: fifty identical
 * copies of the manifest arrive during an ordinary visit.
 * <p>
 * The site is not necessarily the commander's own. Any commander can haul to any depot, so nothing here
 * may be treated as belonging to us: {@code ProvidedAmount} counts everyone's deliveries, and it moves
 * between visits without us lifting a finger.
 * <p>
 * {@code ConstructionProgress} is provided tonnes over required tonnes across the whole manifest - 70 of
 * 6721 tonnes reads as {@code 0.010415} - so it is a flat tonnage ratio, not weighted by what the goods
 * are worth.
 */
public class ColonisationConstructionDepotEvent extends BaseEvent {

    /**
     * One line of the manifest: a commodity, how much the site wants, and how much it already has.
     */
    public static class ResourceRequired {
        @SerializedName("Name")
        private String name;

        @SerializedName("Name_Localised")
        private String nameLocalised;

        @SerializedName("RequiredAmount")
        private int requiredAmount;

        @SerializedName("ProvidedAmount")
        private int providedAmount;

        @SerializedName("Payment")
        private long payment;

        /**
         * Frontier's decorated symbol, e.g. {@code $insulatingmembrane_name;}. Normalise it with
         * {@code JournalSymbol} before joining it with the hold or the commodities table.
         */
        public String getName() {
            return name;
        }

        /**
         * The commodity in the language the GAME is running in, which is not necessarily the language
         * the app speaks. A fallback for display, never a key.
         */
        public String getNameLocalised() {
            return nameLocalised;
        }

        public int getRequiredAmount() {
            return requiredAmount;
        }

        public int getProvidedAmount() {
            return providedAmount;
        }

        /**
         * Credits per tonne the depot pays on delivery.
         */
        public long getPayment() {
            return payment;
        }
    }

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("ConstructionProgress")
    private double constructionProgress;

    @SerializedName("ConstructionComplete")
    private boolean constructionComplete;

    @SerializedName("ConstructionFailed")
    private boolean constructionFailed;

    @SerializedName("ResourcesRequired")
    private List<ResourceRequired> resourcesRequired;

    public ColonisationConstructionDepotEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ColonisationConstructionDepot");
        ColonisationConstructionDepotEvent e = GsonFactory.getGson().fromJson(json, ColonisationConstructionDepotEvent.class);
        this.marketID = e.marketID;
        this.constructionProgress = e.constructionProgress;
        this.constructionComplete = e.constructionComplete;
        this.constructionFailed = e.constructionFailed;
        this.resourcesRequired = e.resourcesRequired;
    }

    @Override
    public String getEventType() {
        return "ColonisationConstructionDepot";
    }

    /**
     * Repeats every few seconds while docked; background context, never something to wake the companion.
     */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "The manifest of a colonisation construction site the ship is docked at: every commodity it "
                + "needs, how much has been delivered so far by anyone, what it pays per tonne, and overall "
                + "build progress.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getMarketID() {
        return marketID;
    }

    /**
     * Completion in {@code [0,1]}: delivered tonnes over required tonnes across the whole manifest.
     */
    public double getConstructionProgress() {
        return constructionProgress;
    }

    public boolean isConstructionComplete() {
        return constructionComplete;
    }

    public boolean isConstructionFailed() {
        return constructionFailed;
    }

    public List<ResourceRequired> getResourcesRequired() {
        return resourcesRequired == null ? List.of() : resourcesRequired;
    }
}
