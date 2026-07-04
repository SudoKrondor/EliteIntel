package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.text.DecimalFormat;
import java.time.Duration;

public class ReputationEvent extends BaseEvent {
    @SerializedName("Empire")
    private double empire;

    @SerializedName("Federation")
    private double federation;

    @SerializedName("Independent")
    private double independent;

    @SerializedName("Alliance")
    private double alliance;

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00%");

    public ReputationEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "Reputation");
        ReputationEvent event = GsonFactory.getGson().fromJson(json, ReputationEvent.class);
        this.empire = event.empire;
        this.federation = event.federation;
        this.independent = event.independent;
        this.alliance = event.alliance;
    }

    @Override
    public String getEventType() {
        return "Reputation";
    }

    /** Reputation snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "A snapshot of your reputation with the major factions (Federation, Empire, Alliance, Independent). Background.";
    }

    @Override
    public String memorySummary() {
        return "reputation - federation " + getFederationPercent() + ", empire " + getEmpirePercent()
                + ", alliance " + getAlliancePercent();
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public double getEmpire() {
        return empire;
    }

    public double getFederation() {
        return federation;
    }

    public double getIndependent() {
        return independent;
    }

    public double getAlliance() {
        return alliance;
    }

    public String getEmpirePercent() {
        return PERCENT_FORMAT.format(empire / 100.0);
    }

    public String getFederationPercent() {
        return PERCENT_FORMAT.format(federation / 100.0);
    }

    public String getIndependentPercent() {
        return PERCENT_FORMAT.format(independent / 100.0);
    }

    public String getAlliancePercent() {
        return PERCENT_FORMAT.format(alliance / 100.0);
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}