package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FactionStats {
    @SerializedName("id")
    public int id;
    @SerializedName("name")
    public String name;
    @SerializedName("allegiance")
    public String allegiance;
    @SerializedName("government")
    public String government;
    @SerializedName("influence")
    public double influence;
    @SerializedName("state")
    public String state;
    @SerializedName("activeStates")
    public List<Object> activeStates;
    @SerializedName("recoveringStates")
    public List<RecoveringState> recoveringStates;
    @SerializedName("pendingStates")
    public List<Object> pendingStates;
    @SerializedName("happiness")
    public String happiness;
    @SerializedName("isPlayer")
    public boolean isPlayer;
    @SerializedName("lastUpdate")
    public long lastUpdate;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAllegiance() {
        return allegiance;
    }

    public String getGovernment() {
        return government;
    }

    public double getInfluence() {
        return influence;
    }

    public String getState() {
        return state;
    }

    public List<Object> getActiveStates() {
        return activeStates;
    }

    public List<RecoveringState> getRecoveringStates() {
        return recoveringStates;
    }

    public List<Object> getPendingStates() {
        return pendingStates;
    }

    public String getHappiness() {
        return happiness;
    }

    public boolean isPlayer() {
        return isPlayer;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }
}
