package elite.intel.gameapi.search.spansh.traderoute;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class TradeRouteResponse implements ToJsonConvertible {

    @SerializedName("result") public List<TradeRouteTransaction> result;
    @SerializedName("state") public String state;
    @SerializedName("status") public String status;

    public List<TradeRouteTransaction> getResult() {
        return result;
    }

    public void setResult(List<TradeRouteTransaction> result) {
        this.result = result;
    }

    public String getState() {
        return state;
    }

    public String getStatus() {
        return status;
    }

    @Override public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}