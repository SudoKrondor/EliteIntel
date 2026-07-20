package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class StationUpdateTime {
    @SerializedName("information")
    public String information;
    @SerializedName("market")
    public String market;
    @SerializedName("shipyard")
    public String shipyard;
    @SerializedName("outfitting")
    public String outfitting;

    public String getInformation() {
        return information;
    }

    public String getMarket() {
        return market;
    }

    public String getShipyard() {
        return shipyard;
    }

    public String getOutfitting() {
        return outfitting;
    }
}
