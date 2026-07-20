package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class DiscoveryData implements ToYamlConvertable {
    @SerializedName("commander")
    public String commander;
    @SerializedName("date")
    public String date;

    public String getCommander() {
        return commander;
    }

    public String getDate() {
        return date;
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
