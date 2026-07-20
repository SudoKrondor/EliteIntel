package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.StringJoiner;

public class TrafficStats implements ToYamlConvertable {
    @SerializedName("total")
    public int total;
    @SerializedName("week")
    public int thisWeek;
    @SerializedName("day")
    public int today;

    public int getTotal() {
        return total;
    }

    public int getThisWeek() {
        return thisWeek;
    }

    public int getDay() {
        return today;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TrafficStats.class.getSimpleName() + "[", "]")
                .add("total=" + total)
                .add("thiwWeek=" + thisWeek)
                .add("today=" + today)
                .toString();
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
