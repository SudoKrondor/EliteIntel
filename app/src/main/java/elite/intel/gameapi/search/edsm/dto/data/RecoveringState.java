package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class RecoveringState {
    @SerializedName("state")
    public String state;
    @SerializedName("trend")
    public int trend;

    public String getState() {
        return state;
    }

    public int getTrend() {
        return trend;
    }
}
