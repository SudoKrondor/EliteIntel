package elite.intel.gameapi.search.spansh.station.interstellarfactors;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;

public class InterstellarFactorsSearchCriteria extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("filters")
    private Filters filters;

    @SerializedName("sort")
    private List<Object> sort = Collections.emptyList();

    @SerializedName("size")
    private int size = 5;

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

        @SerializedName("distance")
        private Distance distance;

        @SerializedName("distance_to_arrival")
        private DistanceToArrival distanceToArrival;

        @SerializedName("services")
        private List<Service> services;

        @SerializedName("large_pads")
        private LargePads largePads;

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public void setDistanceToArrival(DistanceToArrival distanceToArrival) {
            this.distanceToArrival = distanceToArrival;
        }

        public void setServices(List<Service> services) {
            this.services = services;
        }

        /**
         * Null leaves the pad size unconstrained, which is the right answer for a small or medium ship.
         */
        public void setLargePads(LargePads largePads) {
            this.largePads = largePads;
        }
    }

    /**
     * Range filter over a station's large-pad count. Spansh has no boolean "large pad required" on the
     * stations endpoint, so "at least one" is expressed as the range {@code [1, MANY]}.
     */
    public static class LargePads {

        /**
         * Comfortably above the largest pad count in the game; the range is really "one or more".
         */
        private static final int MANY = 100;

        @SerializedName("comparison")
        private final String comparison = "<=>";

        @SerializedName("value")
        private final int[] value;

        // WHY: no setters and no other constructor. The only state this filter has a use for is "one or
        // more", and a mutable range would let a caller build [0, 0] - the exact inverse of the intent.
        private LargePads(int min, int max) {
            this.value = new int[]{min, max};
        }

        public static LargePads atLeastOne() {
            return new LargePads(1, MANY);
        }
    }

    public static class Distance {

        @SerializedName("min")
        private String min;

        @SerializedName("max")
        private String max;

        public Distance(String min, String max) {
            this.min = min;
            this.max = max;
        }

        public void setMin(String min) {
            this.min = min;
        }

        public void setMax(String max) {
            this.max = max;
        }
    }

    /**
     * Models a range filter: {"comparison": "<=>", "value": [minLs, maxLs]}
     */
    public static class DistanceToArrival {

        @SerializedName("comparison")
        private String comparison;

        @SerializedName("value")
        private int[] value;

        public DistanceToArrival(String comparison, int minLs, int maxLs) {
            this.comparison = comparison;
            this.value = new int[]{minLs, maxLs};
        }

        public void setComparison(String comparison) {
            this.comparison = comparison;
        }

        public void setValue(int minLs, int maxLs) {
            this.value = new int[]{minLs, maxLs};
        }
    }

    public static class Service {

        @SerializedName("name")
        private List<String> name;

        public Service(List<String> name) {
            this.name = name;
        }

        public void setName(List<String> name) {
            this.name = name;
        }
    }

    public static class ReferenceCoords {

        @SerializedName("x")
        private double x;

        @SerializedName("y")
        private double y;

        @SerializedName("z")
        private double z;

        public ReferenceCoords(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

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