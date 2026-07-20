package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class StarSystemData {
    @SerializedName("name")
    public String name;
    @SerializedName("information")
    public StarSystemInformation information;

    public String getName() {
        return name;
    }

    public StarSystemInformation getInformation() {
        return information;
    }
}
