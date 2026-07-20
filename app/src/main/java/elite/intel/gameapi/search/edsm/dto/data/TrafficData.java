package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class TrafficData implements ToYamlConvertable {
    @SerializedName("discovery")
    public DiscoveryData discovery;

    @SerializedName("traffic")
    public TrafficStats traffic;

    public DiscoveryData getDiscovery() {
        return discovery;
    }

    public TrafficStats getTraffic() {
        return traffic == null ? new TrafficStats() : traffic;
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
