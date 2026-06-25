package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Optional;

public class PromotionEvent extends BaseEvent {
    @SerializedName("Combat")
    private Integer combat;

    @SerializedName("Trade")
    private Integer trade;

    @SerializedName("Explore")
    private Integer explore;

    @SerializedName("CQC")
    private Integer cqc;

    @SerializedName("Federation")
    private Integer federation;

    @SerializedName("Empire")
    private Integer empire;

    @SerializedName("Soldier")
    private Integer soldier;

    @SerializedName("Exobiologist")
    private Integer exobiologist;

    public PromotionEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "Promotion");
        this.combat = json.has("Combat") ? json.get("Combat").getAsInt() : null;
        this.trade = json.has("Trade") ? json.get("Trade").getAsInt() : null;
        this.explore = json.has("Explore") ? json.get("Explore").getAsInt() : null;
        this.cqc = json.has("CQC") ? json.get("CQC").getAsInt() : null;
        this.federation = json.has("Federation") ? json.get("Federation").getAsInt() : null;
        this.empire = json.has("Empire") ? json.get("Empire").getAsInt() : null;
        this.soldier = json.has("Soldier") ? json.get("Soldier").getAsInt() : null;
        this.exobiologist = json.has("Exobiologist") ? json.get("Exobiologist").getAsInt() : null;
    }

    @Override
    public String getEventType() {
        return "Promotion";
    }

    /** Rank-up milestone. */
    @Override
    public Importance importance() {
        return Importance.HIGH;
    }

    @Override
    public String llmDescription() {
        return "Ranked up in a career (combat, trade, exploration, CQC, Federation or Empire); carries which rank and the new level.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public Integer getCombat() {
        return combat;
    }

    public Integer getTrade() {
        return trade;
    }

    public Integer getExplore() {
        return explore;
    }

    public Integer getCqc() {
        return cqc;
    }

    public Integer getFederation() {
        return federation;
    }

    public Integer getEmpire() {
        return empire;
    }

    public Integer getSoldier() {
        return soldier;
    }

    public Integer getExobiologist() {
        return exobiologist;
    }

    /**
     * Gets the promoted rank type and value as an Optional pair.
     * Returns empty if no rank was promoted (edge case).
     */
    public Optional<PromotionDetails> getPromotedRank() {
        if (combat != null) return Optional.of(new PromotionDetails("Combat", combat));
        if (trade != null) return Optional.of(new PromotionDetails("Trade", trade));
        if (explore != null) return Optional.of(new PromotionDetails("Explore", explore));
        if (cqc != null) return Optional.of(new PromotionDetails("CQC", cqc));
        if (federation != null) return Optional.of(new PromotionDetails("Federation", federation));
        if (empire != null) return Optional.of(new PromotionDetails("Empire", empire));
        if (soldier != null) return Optional.of(new PromotionDetails("Soldier", soldier));
        if (exobiologist != null) return Optional.of(new PromotionDetails("Exobiologist", exobiologist));
        return Optional.empty();
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    /**
     * Simple inner record for rank details (Java 14+; use a static class if older).
     */
    public record PromotionDetails(String type, int rank) {}
}