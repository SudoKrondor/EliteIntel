package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class StarInRadius {
    @SerializedName("distance")
    public double distance;
    @SerializedName("name")
    public String name;

    public double getDistance() {
        return distance;
    }

    public String getName() {
        return name;
    }
}
