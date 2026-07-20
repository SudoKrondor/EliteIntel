package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

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

    public int getDemand() {
        return demand;
    }

    public int getStockBracket() {
        return stockBracket;
    }
}
