package elite.intel.gameapi.search.spansh.market;

import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

public class StationMarketDto extends BaseJsonDto implements ToJsonConvertible {
    private  String stationName;
    private  String systemName;
    private  double distance;
    private  boolean hasLargePad;
    private  boolean isPlanetary;
    private  String marketUpdatedAt;
    private  int sellPrice;
    private  int buyPrice;
    private  int commoditySupply;
    private long marketId;

    public StationMarketDto() {
    }

    public String stationName() {
        return stationName;
    }

    public String systemName() {
        return systemName;
    }

    public double distance() {
        return distance;
    }

    public boolean hasLargePad() {
        return hasLargePad;
    }

    public boolean isPlanetary() {
        return isPlanetary;
    }

    public String marketUpdatedAt() {
        return marketUpdatedAt;
    }

    public int getSellPrice() {
        return sellPrice;
    }

    public int commoditySupply() {
        return commoditySupply;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setHasLargePad(boolean hasLargePad) {
        this.hasLargePad = hasLargePad;
    }

    public void setPlanetary(boolean planetary) {
        isPlanetary = planetary;
    }

    public void setMarketUpdatedAt(String marketUpdatedAt) {
        this.marketUpdatedAt = marketUpdatedAt;
    }

    public void setSellPrice(int sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void setCommoditySupply(int commoditySupply) {
        this.commoditySupply = commoditySupply;
    }

    public int getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(int buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setMarketId(long marketId) {
        this.marketId= marketId;
    }

    public long getMarketId() {
        return marketId;
    }
}
