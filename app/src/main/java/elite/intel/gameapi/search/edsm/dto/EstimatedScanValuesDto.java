package elite.intel.gameapi.search.edsm.dto;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.search.edsm.dto.data.EstimatedScanValuesData;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class EstimatedScanValuesDto implements ToJsonConvertible {
    @SerializedName("data")
    public EstimatedScanValuesData data;
    @SerializedName("timestamp")
    public long timestamp;

    public EstimatedScanValuesData getData() {
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

