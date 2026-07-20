package elite.intel.gameapi.search.spansh.starsystems;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;

public class SystemSearchCriteria extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("filters")
    private Filters filters;

    @SerializedName("sort")
    private List<Object> sort = Collections.emptyList();

    @SerializedName("size")
    private int size = 10;

    @SerializedName("page")
    private int page = 0;

    @SerializedName("reference_system")
    private String reference_system;

    public String getReferenceSystem() {
        return reference_system;
    }

    public void setReferenceSystem(String reference_system) {
        this.reference_system = reference_system;
    }

    public static class Distance {
        @SerializedName("min")
        private String min;
        @SerializedName("max")
        private String max;

        public void setMin(int min) {
            this.min = String.valueOf(min);
        }

        public void setMax(int max) {
            this.max = String.valueOf(max);
        }
    }

    public SystemSearchCriteria() {
        // default constructor for Gson
    }

    public Filters getFilters() {
        return filters;
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }

    public List<Object> getSort() {
        return sort;
    }

    public void setSort(List<Object> sort) {
        this.sort = sort != null ? sort : Collections.emptyList();
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public static class SystemNameFilter {
        @SerializedName("value")
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
    public static class Filters {

        @SerializedName("name")
        private SystemNameFilter systemName;

        public SystemNameFilter getSystemName() {
            return systemName;
        }

        public void setSystemName(SystemNameFilter systemName) {
            this.systemName = systemName;
        }

        @SerializedName("distance")
        private Distance distance;

        @SerializedName("stations")
        private List<StationFilter> stations;

        public Distance getDistance() {
            return distance;
        }

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public List<StationFilter> getStations() {
            return stations;
        }

        public void setStations(List<StationFilter> stations) {
            this.stations = stations;
        }
    }

    public static class StationFilter {

        @SerializedName("type")
        private TypeFilter type;

        public TypeFilter getType() {
            return type;
        }

        public void setType(TypeFilter type) {
            this.type = type;
        }
    }

    public static class TypeFilter {

        @SerializedName("value")
        private List<String> value;

        public List<String> getValue() {
            return value;
        }

        public void setValue(List<String> value) {
            this.value = value;
        }
    }
}