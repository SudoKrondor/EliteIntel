package elite.intel.gameapi.search.spansh.station.vista;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;

public class VistaSearchCriteria extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("filters")
    private Filters filters;

    @SerializedName("reference_coords")
    private ReferenceCoords referenceCoords;

    public void setReferenceCoords(ReferenceCoords referenceCoords) {
        this.referenceCoords = referenceCoords;
    }

    @SerializedName("sort")
    private List<Object> sort = Collections.emptyList();

    @SerializedName("size")
    private int size = 5;

    @SerializedName("page")
    private int page = 0;

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

    public static class Filters {

        @SerializedName("services")
        private List<Service> services;

        public void setServices(List<Service> services) {
            this.services = services;
        }

        @SerializedName("distance")
        private VistaSearchCriteria.Distance distance;

        public void setDistance(Distance distance) {
            this.distance = distance;
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

    public static class Service {

        @SerializedName("name")
        private List<String> name;

        public void setName(List<String> name) {
            this.name = name;
        }
    }
}