package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EstimatedScanValuesData {
    @SerializedName("id")
    public int id;
    @SerializedName("id64")
    public long id64;
    @SerializedName("name")
    public String name;
    @SerializedName("url")
    public String url;
    @SerializedName("estimatedValue")
    public int estimatedValue;
    @SerializedName("estimatedValueMapped")
    public int estimatedValueMapped;
    @SerializedName("valuableBodies")
    public List<ValuableBodyData> valuableBodies;

    public int getId() {
        return id;
    }

    public long getId64() {
        return id64;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getEstimatedValue() {
        return estimatedValue;
    }

    public int getEstimatedValueMapped() {
        return estimatedValueMapped;
    }

    public List<ValuableBodyData> getValuableBodies() {
        return valuableBodies;
    }
}
