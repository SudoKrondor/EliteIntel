package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;

public class Station implements ToYamlConvertable {
    @SerializedName("marketId")
    public long marketId;
    @SerializedName("type")
    public String type;
    @SerializedName("name")
    public String name;
    @SerializedName("body")
    public StationBody body;
    @SerializedName("distanceToArrival")
    public double distanceToArrivalInLightSeconds;
    @SerializedName("allegiance")
    public String allegiance;
    @SerializedName("government")
    public String government;
    @SerializedName("economy")
    public String economy;
    @SerializedName("secondEconomy")
    public String secondEconomy;
    @SerializedName("haveMarket")
    public boolean haveMarket;
    @SerializedName("haveShipyard")
    public boolean haveShipyard;
    @SerializedName("haveOutfitting")
    public boolean haveOutfitting;
    @SerializedName("otherServices")
    public List<String> availableServices;
    @SerializedName("controllingFaction")
    public ControllingFaction controllingFaction;
    @SerializedName("updateTime")
    public StationUpdateTime updateTime;

    /// transient - not part of json!
    private double transientDistance;

    public double getTransientDistance() {
        return transientDistance;
    }

    public void setTransientDistance(double transientDistance) {
        this.transientDistance = transientDistance;
    }

    //transient
    List<Commodity> commodities;

    public List<Commodity> getCommodities() {
        return commodities;
    }

    public void setCommodities(List<Commodity> commodities) {
        this.commodities = commodities;
    }

    //transient
    String starSystemName;
    public String getStarSystemName() {
        return this.starSystemName;
    }

    public void setStarSystemName(String starSystemName) {
        this.starSystemName = starSystemName;
    }

    public long getMarketId() {
        return marketId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public StationBody getBody() {
        return body;
    }

    public double getDistanceToArrivalInLightSeconds() {
        return distanceToArrivalInLightSeconds;
    }

    public String getAllegiance() {
        return allegiance;
    }

    public String getGovernment() {
        return government;
    }

    public String getEconomy() {
        return economy;
    }

    public String getSecondEconomy() {
        return secondEconomy;
    }

    public boolean isHaveMarket() {
        return haveMarket;
    }

    public boolean isHaveShipyard() {
        return haveShipyard;
    }

    public boolean isHaveOutfitting() {
        return haveOutfitting;
    }

    public List<String> getAvailableServices() {
        return availableServices;
    }

    public ControllingFaction getControllingFaction() {
        return controllingFaction;
    }

    public StationUpdateTime getUpdateTime() {
        return updateTime;
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
