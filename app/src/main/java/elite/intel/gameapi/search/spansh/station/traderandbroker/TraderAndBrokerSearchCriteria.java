package elite.intel.gameapi.search.spansh.station.traderandbroker;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The request body for a material trader / technology broker search on {@code /api/stations/search}.
 *
 * <p>Every filter here is written in the exact shape the endpoint accepts, because Spansh ignores a
 * filter it does not recognise and silently matches nothing against a shape it does not expect. That
 * is how these searches went stale: the light-year radius was being sent as a pair of ints where the
 * endpoint wants strings, and no freshness bound was sent at all, so the nearest listing won no
 * matter how many months old it was.
 */
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

        @SerializedName("distance_to_arrival")
        private RangeFilter distanceToArrival;

        @SerializedName("large_pads")
        private RangeFilter largePads;

        @SerializedName("updated_at")
        private UpdatedAt updatedAt;

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public void setMaterialTrader(MaterialTrader materialTrader) {
            this.materialTrader = materialTrader;
        }

        public void setTechnologyBroker(TechnologyBroker technologyBroker) {
            this.technologyBroker = technologyBroker;
        }

        public void setDistanceToArrival(RangeFilter distanceToArrival) {
            this.distanceToArrival = distanceToArrival;
        }

        /**
         * Null leaves the pad size unconstrained, which is the right answer for a small or medium ship.
         */
        public void setLargePads(RangeFilter largePads) {
            this.largePads = largePads;
        }

        public void setUpdatedAt(UpdatedAt updatedAt) {
            this.updatedAt = updatedAt;
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

    /**
     * The light-year radius around the reference coordinates.
     *
     * <p>Unlike every other bound on this endpoint this one is a bare min/max pair of STRINGS, not a
     * {@code "<=>"} range, and it is silently ignored in any other shape.
     */
    public static class Distance {

        @SerializedName("min")
        private String min;

        @SerializedName("max")
        private String max;

        public void setMin(int min) {
            this.min = lightYears(min);
        }

        public void setMax(int max) {
            this.max = lightYears(max);
        }

        // WHY Locale.ROOT: the wire wants "100.00", and a German default locale would write "100,00".
        private static String lightYears(int value) {
            return String.format(Locale.ROOT, "%.2f", (double) value);
        }
    }

    /**
     * A closed numeric range, {@code {"comparison": "<=>", "value": [min, max]}}. Both ends are always
     * sent: the endpoint accepts a one-sided {@code ">="} without complaint and then matches nothing.
     */
    public static class RangeFilter {

        @SerializedName("comparison")
        private final String comparison = "<=>";

        @SerializedName("value")
        private final int[] value;

        public RangeFilter(int min, int max) {
            this.value = new int[]{min, max};
        }
    }

    /**
     * How recently the station's listing must have been updated, as the closed range
     * {@code ["now-7h", "now"]} in Spansh's own relative-time vocabulary.
     */
    public static class UpdatedAt {

        @SerializedName("comparison")
        private final String comparison = "<=>";

        @SerializedName("value")
        private final List<String> value;

        // WHY private: the upper bound is always "now". A range ending in the past would ask for
        // listings that are stale by construction, which is the exact thing this filter exists to stop.
        private UpdatedAt(String since) {
            this.value = List.of(since, "now");
        }

        /**
         * Listings updated between {@code since} and now, where {@code since} is a Spansh relative time
         * such as {@code "now-7h"}.
         */
        public static UpdatedAt since(String since) {
            return new UpdatedAt(since);
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
