package elite.intel.gameapi.search.edsm.dto;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.search.edsm.dto.data.StarSystemData;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class StarSystemDto implements ToJsonConvertible {
    @SerializedName("data")
    public StarSystemData data;
    @SerializedName("timestamp")
    public long timestamp;

    @SerializedName("coords")
    private Coords coords;

    public Coords getCoords() {
        return coords;
    }

    public static class Coords{
        double x,y,z;

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }

    public StarSystemData getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }
}

