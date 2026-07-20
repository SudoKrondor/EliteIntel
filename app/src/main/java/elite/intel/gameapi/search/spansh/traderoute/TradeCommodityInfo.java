package elite.intel.gameapi.search.spansh.traderoute;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class TradeCommodityInfo implements ToJsonConvertible {
    @SerializedName("buy_price") public long buyPrice;
    @SerializedName("demand") public long demand;
    @SerializedName("sell_price") public long sellPrice;
    @SerializedName("supply") public long supply;

    public long getBuyPrice() {
        return buyPrice;
    }

    public long getDemand() {
        return demand;
    }

    public long getSellPrice() {
        return sellPrice;
    }

    public long getSupply() {
        return supply;
    }

    @Override public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}
