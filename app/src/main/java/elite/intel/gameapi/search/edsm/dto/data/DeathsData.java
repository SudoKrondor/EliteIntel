package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class DeathsData implements ToYamlConvertable {
    @SerializedName("deaths")
    public DeathsStats deaths;


    public DeathsStats getDeaths() {
        return deaths == null ? new DeathsStats() : deaths;
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
