package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class ValuableBodyData {
    @SerializedName("bodyId")
    public int bodyId;
    @SerializedName("bodyName")
    public String bodyName;
    @SerializedName("distance")
    public double distance;
    @SerializedName("valueMax")
    public int valueMax;

    public int getBodyId() {
        return bodyId;
    }

    public String getBodyName() {
        return bodyName;
    }

    public double getDistance() {
        return distance;
    }

    public int getValueMax() {
        return valueMax;
    }
}
