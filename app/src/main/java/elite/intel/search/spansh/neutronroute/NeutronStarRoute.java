package elite.intel.search.spansh.neutronroute;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class NeutronStarRoute implements ToJsonConvertible {

    @SerializedName("job")
    private String job;
    @SerializedName("result")
    private NeutronStarRouteResult result;
    @SerializedName("state")
    private String state;
    @SerializedName("status")
    private String status;

    public String getJob() {
        return job;
    }

    public NeutronStarRouteResult getResult() {
        return result;
    }

    public String getState() {
        return state;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}
