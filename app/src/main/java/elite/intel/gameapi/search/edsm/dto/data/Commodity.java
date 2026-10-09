package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.ai.brain.vega.SpokenAmounts;

public class Commodity {
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("buyPrice")
    public int buyPrice;
    @SerializedName("stock")
    public int stock;
    @SerializedName("sellPrice")
    public int sellPrice;
    @SerializedName("demand")
    public int demand;
    @SerializedName("stockBracket")
    public int stockBracket;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getBuyPrice() {
        return buyPrice;
    }

    public int getStock() {
        return stock;
    }

    public int getSellPrice() {
        return sellPrice;
    }

    /**
     * Read by the YAML mapper only, for the LLM. See {@link SpokenAmounts#nestedSibling}.
     */
    public String getBuyPriceSpoken() {
        return SpokenAmounts.nestedSibling(buyPrice);
    }

    /**
     * Read by the YAML mapper only, for the LLM. See {@link SpokenAmounts#nestedSibling}.
     */
    public String getSellPriceSpoken() {
        return SpokenAmounts.nestedSibling(sellPrice);
    }

    public int getDemand() {
        return demand;
    }

    public int getStockBracket() {
        return stockBracket;
    }
}
