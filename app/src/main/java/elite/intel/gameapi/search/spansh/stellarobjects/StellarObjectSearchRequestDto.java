package elite.intel.gameapi.search.spansh.stellarobjects;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;

public class StellarObjectSearchRequestDto implements ToJsonConvertible {

    @SerializedName("filters")
    private Filters filters;

    @SerializedName("sort")
    private List<Object> sort = Collections.emptyList();

    @SerializedName("size")
    private int size = 5; //default result size

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
        @SerializedName("reserve_level")
        private ReserveLevel reserveLevel;
        @SerializedName("ring_signals")
        private List<RingSignal> ringSignals;

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public void setReserveLevel(ReserveLevel reserveLevel) {
            this.reserveLevel = reserveLevel;
        }

        public void setRingSignals(List<RingSignal> ringSignals) {
            this.ringSignals = ringSignals;
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

    public static class ReserveLevel {

        @SerializedName("value")
        private List<String> value;

        public void setValue(List<String> value) {
            this.value = value;
        }
    }

    public static class RingSignal {

        @SerializedName("comparison")
        private String comparison;

        @SerializedName("count")
        private List<Integer> count;

        @SerializedName("name")
        private String name;

        public void setComparison(String comparison) {
            this.comparison = comparison;
        }

        public void setCount(List<Integer> count) {
            this.count = count;
        }

        public void setName(String name) {
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