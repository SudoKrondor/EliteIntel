package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class ShipyardData implements ToYamlConvertable {
    @SerializedName("id")
    public int id;
    @SerializedName("id64")
    public long id64;
    @SerializedName("name")
    public String name;
    @SerializedName("marketId")
    public long marketId;
    @SerializedName("sId")
    public int sId;
    @SerializedName("sName")
    public String sName;
    @SerializedName("ships")
    public Object ships; // Null in example; can be refined to List if structure known

    public int getId() {
        return id;
    }

    public long getId64() {
        return id64;
    }

    public String getName() {
        return name;
    }

    public long getMarketId() {
        return marketId;
    }

    public int getsId() {
        return sId;
    }

    public String getsName() {
        return sName;
    }

    public Object getShips() {
        return ships;
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
