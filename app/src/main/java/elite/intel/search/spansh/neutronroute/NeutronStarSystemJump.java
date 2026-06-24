package elite.intel.search.spansh.neutronroute;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class NeutronStarSystemJump implements ToJsonConvertible {

    @SerializedName("system")
    private String system;
    @SerializedName("id64")
    private long systemAddress;
    @SerializedName("distance_jumped")
    private double distanceJumped;
    @SerializedName("distance_left")
    private double distanceLeft;
    @SerializedName("jumps")
    private int jumps;
    @SerializedName("neutron_star")
    private boolean neutronStar;
    @SerializedName("x")
    private double x;
    @SerializedName("y")
    private double y;
    @SerializedName("z")
    private double z;

    public String getSystem() {
        return system;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public double getDistanceJumped() {
        return distanceJumped;
    }

    public double getDistanceLeft() {
        return distanceLeft;
    }

    public int getJumps() {
        return jumps;
    }

    public boolean isNeutronStar() {
        return neutronStar;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}
