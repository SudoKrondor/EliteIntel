package elite.intel.gameapi.search.edsm.dto;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.search.edsm.dto.data.FactionStats;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class FactionDto implements ToJsonConvertible {
    @SerializedName("data")
    public FactionStats data;
    @SerializedName("timestamp")
    public long timestamp;

    public FactionStats getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}

