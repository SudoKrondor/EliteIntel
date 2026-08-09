package elite.intel.gameapi.search.edsm.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.StringJoiner;

/**
 * EDSM's traffic counts for a system.
 *
 * <p>The two sets of annotations serve opposite directions and are both load-bearing: Gson's
 * {@link SerializedName} reads EDSM's JSON coming in, while Jackson's {@link JsonProperty} names the
 * fields going out as YAML. That YAML is handed to the companion as narration source, so a property
 * name here is read out loud — {@code thisWeek} was being spoken as one word. Every name in this
 * class has to be something a person would say.
 *
 * <p>Both the field and its getter carry the same {@code JsonProperty}: Jackson otherwise treats them
 * as two properties and emits the number twice, which is what {@code today}/{@code getDay} used to do.
 */
public class TrafficStats implements ToYamlConvertable {
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
    public int getDay() {
        return today;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TrafficStats.class.getSimpleName() + "[", "]")
                .add("total=" + total)
                .add("this Week=" + thisWeek)
                .add("today=" + today)
                .toString();
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
