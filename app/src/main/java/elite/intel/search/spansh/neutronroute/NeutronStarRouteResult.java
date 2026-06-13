package elite.intel.search.spansh.neutronroute;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class NeutronStarRouteResult implements ToJsonConvertible {

    @SerializedName("source_system")
    private String sourceSystem;
    @SerializedName("destination_system")
    private String destinationSystem;
    @SerializedName("distance")
    private double distance;
    @SerializedName("efficiency")
    private String efficiency;
    @SerializedName("range")
    private String range;
    @SerializedName("total_jumps")
    private int totalJumps;
    @SerializedName("system_jumps")
    private List<NeutronStarSystemJump> systemJumps;
    @SerializedName("via")
    private List<String> via;
    @SerializedName("job")
    private String job;

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getDestinationSystem() {
        return destinationSystem;
    }

    public double getDistance() {
        return distance;
    }

    public String getEfficiency() {
        return efficiency;
    }

    public String getRange() {
        return range;
    }

    public int getTotalJumps() {
        return totalJumps;
    }

    public List<NeutronStarSystemJump> getSystemJumps() {
        return systemJumps;
    }

    public List<String> getVia() {
        return via;
    }

    public String getJob() {
        return job;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}
