package elite.intel.gameapi.search.edsm.dto;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.search.edsm.dto.data.StarSystemCoordinates;
import elite.intel.gameapi.search.edsm.dto.data.StarSystemData;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class StarSystemDto implements ToJsonConvertible {
    @SerializedName("data")
    public StarSystemData data;
    @SerializedName("timestamp")
    public long timestamp;

    /**
     * The system's position, or null when the response carried none.
     *
     * <p>WHY delegated: this wrapper is always built by hand around a parsed response, never
     * deserialized itself, so a {@code coords} field of its own could only ever read null. The
     * coordinates arrive inside the response element.
     */
    public StarSystemCoordinates getCoords() {
        return data == null ? null : data.getCoords();
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

