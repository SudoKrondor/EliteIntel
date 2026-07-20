package elite.intel.gameapi.search.spansh.traderoute;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class TradeRouteTransaction implements ToJsonConvertible {
    @SerializedName("commodities") public List<TradeCommodity> commodities;
    @SerializedName("cumulative_profit") public long cumulativeProfit;
    @SerializedName("destination") public TradeRouteStationInfo destination;
    @SerializedName("distance") public double distance;
    @SerializedName("source") public TradeRouteStationInfo source;
    @SerializedName("total_profit") public long totalProfit;

    public List<TradeCommodity> getCommodities() {
        return commodities;
    }

    public long getCumulativeProfit() {
        return cumulativeProfit;
    }

    public TradeRouteStationInfo getDestination() {
        return destination;
    }

    public double getDistance() {
        return distance;
    }

    public TradeRouteStationInfo getSource() {
        return source;
    }

    public long getTotalProfit() {
        return totalProfit;
    }

    @Override public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}

