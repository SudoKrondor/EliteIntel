package elite.intel.gameapi.search.edsm.dto;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.search.edsm.dto.data.TrafficData;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class TrafficDto implements ToJsonConvertible, ToYamlConvertable {
    @SerializedName("data")
    public TrafficData data;
    @SerializedName("timestamp")
    public long timestamp;

    public TrafficData getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}

