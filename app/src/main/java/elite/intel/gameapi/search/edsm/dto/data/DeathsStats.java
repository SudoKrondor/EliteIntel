package elite.intel.gameapi.search.edsm.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.StringJoiner;

/**
 * EDSM's death counts for a system. Named for speech for the same reason as
 * {@link TrafficStats} — this goes to the companion as YAML and the property names are spoken.
 */
public class DeathsStats implements ToYamlConvertable {
    @SerializedName("total")
    @JsonProperty("total")
    public int total;
    @SerializedName("week")
    @JsonProperty("this week")
    public int thisWeek;
    @SerializedName("day")
    @JsonProperty("today")
    public int today;

    @JsonProperty("total")
    public int getTotal() {
        return total;
    }

    @JsonProperty("this week")
    public int getThisWeek() {
        return thisWeek;
    }

    @JsonProperty("today")
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
