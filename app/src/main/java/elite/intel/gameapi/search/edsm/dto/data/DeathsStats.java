package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.StringJoiner;

public class DeathsStats implements ToYamlConvertable {
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

    public int getToday() {
        return today;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DeathsStats.class.getSimpleName() + "[", "]")
                .add("total=" + total)
                .add("week=" + thisWeek)
                .add("day=" + today)
                .toString();
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
