package elite.intel.gameapi.search.spansh.traderoute;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class TradeRouteStationInfo implements ToJsonConvertible {
    @SerializedName("distance_to_arrival") public double distanceToArrival;
    @SerializedName("market_id") public long marketId;
    @SerializedName("market_updated_at") public long marketUpdatedAt;
    @SerializedName("station") public String station;
    @SerializedName("system") public String system;
    @SerializedName("system_id64") public long systemId64;   // we ignore it but keep for completeness
    @SerializedName("x") public double x;
    @SerializedName("y") public double y;
    @SerializedName("z") public double z;

    public double getDistanceToArrival() {
        return distanceToArrival;
    }

    public long getMarketId() {
        return marketId;
    }

    public long getMarketUpdatedAt() {
        return marketUpdatedAt;
    }

    public String getStation() {
        return station;
    }

    public String getSystem() {
        return system;
    }

    public long getSystemId64() {
        return systemId64;
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

    @Override public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}
