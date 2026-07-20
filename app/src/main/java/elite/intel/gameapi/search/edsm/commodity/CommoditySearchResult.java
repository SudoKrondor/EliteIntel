package elite.intel.gameapi.search.edsm.commodity;

import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

public class CommoditySearchResult extends BaseJsonDto implements ToJsonConvertible {

    private String commodity;
    private double price;
    private String starSystem;
    private String stationName;
    private String stationType;

    /// transient
    private double distanceFromPlayer;

    public double getDistanceFromPlayer() {
        return distanceFromPlayer;
    }

    public void setDistanceFromPlayer(double distanceFromPlayer) {
        this.distanceFromPlayer = distanceFromPlayer;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public void setStarSystem(String starSystem) {
        this.starSystem = starSystem;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    @Override public String toString() {
        return toJson();
    }
}
