package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class MultiSellExplorationDataEvent extends BaseEvent {
    public static class DiscoveredSystem {
        @SerializedName("SystemName")
        private String systemName;

        @SerializedName("NumBodies")
        private int numBodies;

        public String getSystemName() {
            return systemName;
        }

        public int getNumBodies() {
            return numBodies;
        }
    }

    @SerializedName("Discovered")
    private List<DiscoveredSystem> discovered;

    @SerializedName("BaseValue")
    private long baseValue;

    @SerializedName("Bonus")
    private long bonus;

    @SerializedName("TotalEarnings")
    private long totalEarnings;

    public MultiSellExplorationDataEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "MultiSellExplorationData");
        MultiSellExplorationDataEvent event = GsonFactory.getGson().fromJson(json, MultiSellExplorationDataEvent.class);
        this.discovered = event.discovered;
        this.baseValue = event.baseValue;
        this.bonus = event.bonus;
        this.totalEarnings = event.totalEarnings;
    }

    @Override
    public String getEventType() {
        return "MultiSellExplorationData";
    }

    @Override
    public String llmDescription() {
        return "Sold exploration data for multiple systems at once; carries the systems and the total payout.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public List<DiscoveredSystem> getDiscovered() {
        return discovered;
    }

    public long getBaseValue() {
        return baseValue;
    }

    public long getBonus() {
        return bonus;
    }

    public long getTotalEarnings() {
        return totalEarnings;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}