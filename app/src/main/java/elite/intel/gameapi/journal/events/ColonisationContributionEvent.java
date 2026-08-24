package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

/**
 * Cargo handed over at a colonisation construction site - the moment the hold empties into the build.
 * <p>
 * The manifest that follows a second later says what is still needed, so nothing has to be deducted from
 * a stored list by hand: the depot event is always the authority and this is the signal that it is about
 * to change. What this carries that the manifest does not is what WE gave, as opposed to what everyone
 * has given between them.
 */
public class ColonisationContributionEvent extends BaseEvent {

    /**
     * One commodity handed over, in the quantity handed over.
     */
    public static class Contribution {
        @SerializedName("Name")
        private String name;

        @SerializedName("Name_Localised")
        private String nameLocalised;

        @SerializedName("Amount")
        private int amount;

        /**
         * Frontier's decorated symbol. Note the casing differs from the manifest's - the contribution
         * writes {@code $InsulatingMembrane_name;} where the manifest writes {@code $insulatingmembrane_name;}
         * for the same good - so it must be normalised before it is compared with anything.
         */
        public String getName() {
            return name;
        }

        public String getNameLocalised() {
            return nameLocalised;
        }

        public int getAmount() {
            return amount;
        }
    }

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("Contributions")
    private List<Contribution> contributions;

    public ColonisationContributionEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ColonisationContribution");
        ColonisationContributionEvent e = GsonFactory.getGson().fromJson(json, ColonisationContributionEvent.class);
        this.marketID = e.marketID;
        this.contributions = e.contributions;
    }

    @Override
    public String getEventType() {
        return "ColonisationContribution";
    }

    /**
     * The commander finished a haul. Worth remarking on, unlike the manifest that repeats behind it.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Delivered cargo to a colonisation construction site; carries each commodity handed over and "
                + "how many tonnes of it.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getMarketID() {
        return marketID;
    }

    public List<Contribution> getContributions() {
        return contributions == null ? List.of() : contributions;
    }
}
