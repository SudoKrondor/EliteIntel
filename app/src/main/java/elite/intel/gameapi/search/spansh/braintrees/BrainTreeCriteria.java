package elite.intel.gameapi.search.spansh.braintrees;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;

public class BrainTreeCriteria extends BaseJsonDto implements ToJsonConvertible {


    @SerializedName("filters")
    private BrainTreeCriteria.Filters filters;

    @SerializedName("sort")
    private List<Object> sort = Collections.emptyList();

    @SerializedName("size")
    private int size = 5; //default result size

    @SerializedName("page")
    private int page = 0;

    @SerializedName("reference_coords")
    private BrainTreeCriteria.ReferenceCoords referenceCoords;

    public void setFilters(BrainTreeCriteria.Filters filters) {
        this.filters = filters;
    }

    public void setSort(List<Object> sort) {
        this.sort = sort != null ? sort : Collections.emptyList();
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setReferenceCoords(BrainTreeCriteria.ReferenceCoords referenceCoords) {
        this.referenceCoords = referenceCoords;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    public static class Filters {

        @SerializedName("distance")
        private BrainTreeCriteria.Distance distance;

        @SerializedName("genuses")
        private BrainTreeCriteria.Genuses genuses;

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public void setGenuses(Genuses genuses) {
            this.genuses = genuses;
        }
    }

    public static class Genuses {
        @SerializedName("value")
        private List<String> genuses;

        public void setGenuses(List<String> genuses) {
            this.genuses = genuses;
        }
    }

    public static class Distance {
        @SerializedName("min")
        private int min;
        @SerializedName("max")
        private int max;

        public void setMin(int min) {
            this.min = min;
        }

        public void setMax(int max) {
            this.max = max;
        }
    }

    public static class ReferenceCoords {

        @SerializedName("x")
        private double x;

        @SerializedName("y")
        private double y;

        @SerializedName("z")
        private double z;

        public void setX(double x) {
            this.x = x;
        }

        public void setY(double y) {
            this.y = y;
        }

        public void setZ(double z) {
            this.z = z;
        }
    }
}
