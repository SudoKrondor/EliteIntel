package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class ProgressEvent extends BaseEvent implements PlayerProgressStats {
    @SerializedName("Combat")
    private int combat;

    @SerializedName("Trade")
    private int trade;

    @SerializedName("Explore")
    private int explore;

    @SerializedName("Soldier")
    private int soldier;

    @SerializedName("Exobiologist")
    private int exobiologist;

    @SerializedName("Empire")
    private int empire;

    @SerializedName("Federation")
    private int federation;

    @SerializedName("CQC")
    private int cqc;

    public ProgressEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "Progress");
        ProgressEvent event = GsonFactory.getGson().fromJson(json, ProgressEvent.class);
        this.combat = event.combat;
        this.trade = event.trade;
        this.explore = event.explore;
        this.soldier = event.soldier;
        this.exobiologist = event.exobiologist;
        this.empire = event.empire;
        this.federation = event.federation;
        this.cqc = event.cqc;
    }

    @Override
    public String getEventType() {
        return "Progress";
    }

    /** Rank progress snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "A snapshot of rank progress percentages (combat, trade, exploration, and so on). Background.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public int getCombat() {
        return combat;
    }

    public int getTrade() {
        return trade;
    }

    public int getExplore() {
        return explore;
    }

    public int getSoldier() {
        return soldier;
    }

    public int getExobiologist() {
        return exobiologist;
    }

    public int getEmpire() {
        return empire;
    }

    public int getFederation() {
        return federation;
    }

    public int getCQC() {
        return cqc;
    }
}