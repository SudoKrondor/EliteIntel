package elite.intel.gameapi.search.spansh.traderoute;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class TradeCommodity implements ToJsonConvertible {
    @SerializedName("amount") public int amount;
    @SerializedName("destination_commodity") public TradeCommodityInfo destinationCommodity;
    @SerializedName("name") public String name;
    @SerializedName("profit") public long profit;
    @SerializedName("source_commodity") public TradeCommodityInfo sourceCommodity;
    @SerializedName("total_profit") public long totalProfit;

    public int getAmount() {
        return amount;
    }

    public TradeCommodityInfo getDestinationCommodity() {
        return destinationCommodity;
    }

    public String getName() {
        return name;
    }

    public long getProfit() {
        return profit;
    }

    public TradeCommodityInfo getSourceCommodity() {
        return sourceCommodity;
    }

    public long getTotalProfit() {
        return totalProfit;
    }

    @Override public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}
