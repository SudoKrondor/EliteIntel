package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class RingData {
    @SerializedName("name")
    public String name;
    @SerializedName("type")
    public String type;
    @SerializedName("mass")
    public double mass;
    @SerializedName("innerRadius")
    public double innerRadius;
    @SerializedName("outerRadius")
    public double outerRadius;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getMass() {
        return mass;
    }

    public double getInnerRadius() {
        return innerRadius;
    }

    public double getOuterRadius() {
        return outerRadius;
    }
}
