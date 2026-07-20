package elite.intel.gameapi.search.spansh.station;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class DestinationDto implements ToJsonConvertible {

    @SerializedName("json")
    private String json;


    @SuppressWarnings("unused")
    public DestinationDto() {
        // required zero-arg constructor for Gson
    }

    public DestinationDto(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}