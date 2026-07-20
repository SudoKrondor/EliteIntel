package elite.intel.gameapi.search.edsm.dto;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.search.edsm.dto.data.StationsData;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class StationsDto implements ToJsonConvertible {
    @SerializedName("data")
    public StationsData data;
    @SerializedName("timestamp")
    public long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public StationsData getData() {
        return data == null ? new StationsData() : data;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}

