package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FactionData {
    @SerializedName("id")
    public int id;
    @SerializedName("id64")
    public long id64;
    @SerializedName("name")
    public String name;
    @SerializedName("url")
    public String url;
    @SerializedName("controllingFaction")
    public ControllingFaction controllingFaction;
    @SerializedName("factions")
    public List<FactionStats> factions;

    public int getId() {
        return id;
    }

    public long getId64() {
        return id64;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public ControllingFaction getControllingFaction() {
        return controllingFaction;
    }

    public List<FactionStats> getFactions() {
        return factions;
    }
}
