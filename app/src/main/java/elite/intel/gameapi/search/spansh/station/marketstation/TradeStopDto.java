package elite.intel.gameapi.search.spansh.station.marketstation;

import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeCommodity;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class TradeStopDto extends BaseJsonDto implements ToJsonConvertible {

    private int stopNumber;
    private List<TradeCommodity> commodities;
    private String sourceSystem;
    private String sourceStation;
    private String destinationSystem;
    private String destinationStation;
    private Long sourceMarketId;
    private Long destinationMarketId;

    public TradeStopDto() {
        //Require to serialize
    }

    public TradeStopDto(int stopNumber, List<TradeCommodity> commodities, String sourceSystem, String sourceDestination, String destinationSystem, String destinationStation, Long sourceMarketId, Long destinationMarketId) {
        this.stopNumber = stopNumber;
        this.commodities = commodities;
        this.sourceSystem = sourceSystem;
        this.sourceStation = sourceDestination;
        this.destinationSystem = destinationSystem;
        this.destinationStation = destinationStation;
        this.sourceMarketId = sourceMarketId;
        this.destinationMarketId = destinationMarketId;
    }


    public List<TradeCommodity> getCommodities() {
        return commodities;
    }

    public void setCommodities(List<TradeCommodity> commodities) {
        this.commodities = commodities;
    }

    public int getStopNumber() {
        return stopNumber;
    }

    public void setStopNumber(int stopNumber) {
        this.stopNumber = stopNumber;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceStation() {
        return sourceStation;
    }

    public void setSourceStation(String sourceStation) {
        this.sourceStation = sourceStation;
    }

    public String getDestinationSystem() {
        return destinationSystem;
    }

    public void setDestinationSystem(String destinationSystem) {
        this.destinationSystem = destinationSystem;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
    }

    public Long getSourceMarketId() {
        return sourceMarketId;
    }

    public void setSourceMarketId(Long sourceMarketId) {
        this.sourceMarketId = sourceMarketId;
    }

    public Long getDestinationMarketId() {
        return destinationMarketId;
    }

    public void setDestinationMarketId(Long destinationMarketId) {
        this.destinationMarketId = destinationMarketId;
    }
}
