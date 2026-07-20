package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

public class ParentBody {
    @SerializedName("Star")
    public Integer star;
    @SerializedName("Planet")
    public Integer planet;
    @SerializedName("Null")
    public Integer nullParent;

    public Integer getStar() {
        return star;
    }

    public Integer getPlanet() {
        return planet;
    }

    public Integer getNullParent() {
        return nullParent;
    }
}
