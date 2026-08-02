package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class StarSystemData {
    @SerializedName("name")
    public String name;
    @SerializedName("information")
    public StarSystemInformation information;
    @SerializedName("coords")
    public StarSystemCoordinates coords;

    public String getName() {
        return name;
    }

    public StarSystemInformation getInformation() {
        return information;
    }

    /**
     * Present only when the request asked for coordinates, and only for a system EDSM knows.
     */
    public StarSystemCoordinates getCoords() {
        return coords;
    }
}
