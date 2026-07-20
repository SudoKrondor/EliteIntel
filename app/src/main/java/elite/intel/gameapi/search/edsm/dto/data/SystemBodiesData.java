package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SystemBodiesData {
    @SerializedName("id")
    public int id;
    @SerializedName("id64")
    public long id64;
    @SerializedName("name")
    public String name;
    @SerializedName("url")
    public String url;
    @SerializedName("bodyCount")
    public int bodyCount;
    @SerializedName("bodies")
    public List<BodyData> bodies;

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

    public int getBodyCount() {
        return bodyCount;
    }

    public List<BodyData> getBodies() {
        return bodies;
    }
}
