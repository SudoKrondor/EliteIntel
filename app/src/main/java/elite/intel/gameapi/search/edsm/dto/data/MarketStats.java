package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MarketStats {
    @SerializedName("id")
    public int id;
    @SerializedName("id64")
    public long id64;
    @SerializedName("name")
    public String name;
    @SerializedName("marketId")
    public long marketId;
    @SerializedName("sId")
    public int sId;
    @SerializedName("sName")
    public String sName;
    @SerializedName("url")
    public String url;
    @SerializedName("commodities")
    public List<Commodity> commodities;

    public int getId() {
        return id;
    }

    public long getId64() {
        return id64;
    }

    public String getName() {
        return name;
    }

    public long getMarketId() {
        return marketId;
    }

    public int getsId() {
        return sId;
    }

    public String getStationName() {
        return sName;
    }

    public String getUrl() {
        return url;
    }

    public List<Commodity> getCommodities() {
        return commodities;
    }
}
