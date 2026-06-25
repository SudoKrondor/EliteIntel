package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

public class SquadronStartupEvent extends BaseEvent {

    @SerializedName("SquadronID")
    private int squadronID;

    @SerializedName("SquadronName")
    private String squadronName;

    @SerializedName("CurrentRank")
    private int currentRank;

    @SerializedName("CurrentRankName")
    private String currentRankName;

    @SerializedName("CurrentRankName_Localised")
    private String currentRankNameLocalised;

    public SquadronStartupEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "SquadronStartup");
        SquadronStartupEvent event = GsonFactory.getGson().fromJson(json, SquadronStartupEvent.class);
        this.squadronID = event.squadronID;
        this.squadronName = event.squadronName;
        this.currentRank = event.currentRank;
        this.currentRankName = event.currentRankName;
        this.currentRankNameLocalised = event.currentRankNameLocalised;
    }

    @Override
    public String getEventType() {
        return "SquadronStartup";
    }

    @Override
    public String llmDescription() {
        return "Reports your squadron membership at game load; carries the squadron name and your rank. Background.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public int getSquadronID() {
        return squadronID;
    }

    public void setSquadronID(int squadronID) {
        this.squadronID = squadronID;
    }

    public String getSquadronName() {
        return squadronName;
    }

    public void setSquadronName(String squadronName) {
        this.squadronName = squadronName;
    }

    public int getCurrentRank() {
        return currentRank;
    }

    public void setCurrentRank(int currentRank) {
        this.currentRank = currentRank;
    }

    public String getCurrentRankName() {
        return currentRankName;
    }

    public void setCurrentRankName(String currentRankName) {
        this.currentRankName = currentRankName;
    }

    public String getCurrentRankNameLocalised() {
        return currentRankNameLocalised;
    }

    public void setCurrentRankNameLocalised(String currentRankNameLocalised) {
        this.currentRankNameLocalised = currentRankNameLocalised;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SquadronStartupEvent that = (SquadronStartupEvent) o;
        return squadronID == that.squadronID &&
                currentRank == that.currentRank &&
                Objects.equals(squadronName, that.squadronName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(squadronID, squadronName, currentRank);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SquadronStartupEvent.class.getSimpleName() + "[", "]")
                .add("SquadronID=" + squadronID)
                .add("SquadronName='" + squadronName + "'")
                .add("CurrentRank=" + currentRank)
                .add("CurrentRankName='" + currentRankName + "'")
                .add("CurrentRankNameLocalised='" + currentRankNameLocalised + "'")
                .toString();
    }
}
