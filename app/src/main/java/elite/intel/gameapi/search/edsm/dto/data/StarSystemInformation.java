package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class StarSystemInformation {
    @SerializedName("allegiance")
    public String allegiance;
    @SerializedName("government")
    public String government;
    @SerializedName("faction")
    public String faction;
    @SerializedName("factionState")
    public String factionState;
    @SerializedName("population")
    public long population;
    @SerializedName("security")
    public String security;
    @SerializedName("economy")
    public String economy;
    @SerializedName("secondEconomy")
    public String secondEconomy;
    @SerializedName("reserve")
    public String reserve;

    public String getAllegiance() {
        return allegiance;
    }

    public String getGovernment() {
        return government;
    }

    public String getFaction() {
        return faction;
    }

    public String getFactionState() {
        return factionState;
    }

    public long getPopulation() {
        return population;
    }

    public String getSecurity() {
        return security;
    }

    public String getEconomy() {
        return economy;
    }

    public String getSecondEconomy() {
        return secondEconomy;
    }

    public String getReserve() {
        return reserve;
    }
}
