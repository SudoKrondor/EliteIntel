package elite.intel.gameapi.search.spansh.findcarrier;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;

public class FleetCarrierSearchCriteriaDto implements ToJsonConvertible {

    @SerializedName("filters")
    private Filters filters;

    @SerializedName("sort")
    private List<Object> sort = Collections.emptyList();

    @SerializedName("size")
    private int size = 10;

    @SerializedName("page")
    private int page = 0;

    @SerializedName("reference_coords")
    private ReferenceCoords referenceCoords;

    public void setFilters(Filters filters) {
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

    public void setReferenceCoords(ReferenceCoords referenceCoords) {
        this.referenceCoords = referenceCoords;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    public static class Filters {

        @SerializedName("carrier_docking_access")
        private CarrierDockingAccess carrierDockingAccess;

        @SerializedName("distance")
        private Distance distance;

        public void setCarrierDockingAccess(CarrierDockingAccess carrierDockingAccess) {
            this.carrierDockingAccess = carrierDockingAccess;
        }

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        @SerializedName("updated_at")
        private UpdatedAt updatedAt;

        public void setUpdatedAt(UpdatedAt updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class CarrierDockingAccess {

        @SerializedName("value")
        private List<String> value;

        public void setValue(List<String> value) {
            this.value = value;
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

    public static class UpdatedAt {
        @SerializedName("comparison")
        private String comparison;           // e.g. "<=>"

        @SerializedName("value")
        private List<String> value;          // ISO-8601 strings

        public void setComparison(String comparison) { this.comparison = comparison; }
        public void setValue(List<String> value) { this.value = value; }
    }
}