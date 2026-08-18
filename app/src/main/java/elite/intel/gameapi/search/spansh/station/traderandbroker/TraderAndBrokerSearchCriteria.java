package elite.intel.gameapi.search.spansh.station.traderandbroker;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;

public class TraderAndBrokerSearchCriteria extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("filters")
    private Filters filters;

    @SerializedName("sort")
    private List<Object> sort = Collections.emptyList();

    @SerializedName("size")
    private int size;

    @SerializedName("page")
    private int page;

    @SerializedName("reference_coords")
    private ReferenceCoords referenceCoords;

    public static class Filters {

        @SerializedName("material_trader")
        private MaterialTrader materialTrader;

        @SerializedName("technology_broker")
        private TechnologyBroker technologyBroker;

        @SerializedName("distance")
        private Distance distance;

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public void setMaterialTrader(MaterialTrader materialTrader) {
            this.materialTrader = materialTrader;
        }

        public void setTechnologyBroker(TechnologyBroker technologyBroker) {
            this.technologyBroker = technologyBroker;
        }
    }

    public static class MaterialTrader {

        @SerializedName("value")
        private List<String> value;

        public void setValue(List<String> value) {
            this.value = value;
        }
    }

    public static class TechnologyBroker {
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

    /**
     * Sorts Spansh station search results nearest-first before pagination.
     */
    public static class DistanceSort {

        @SerializedName("distance")
        private final Ascending distance = new Ascending();

        private static class Ascending {

            @SerializedName("direction")
            private final String direction = "asc";
        }
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }

    public void setSort(List<Object> sort) {
        this.sort = sort;
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
}