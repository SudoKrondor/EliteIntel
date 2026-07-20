package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class StationBody {
    @SerializedName("id")
    public int id;
    @SerializedName("name")
    public String name;
    @SerializedName("latitude")
    public Double latitude;
    @SerializedName("longitude")
    public Double longitude;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
