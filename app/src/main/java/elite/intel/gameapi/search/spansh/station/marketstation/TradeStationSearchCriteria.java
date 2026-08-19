package elite.intel.gameapi.search.spansh.station.marketstation;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class TradeStationSearchCriteria extends BaseJsonDto implements ToJsonConvertible {

    /**
     * Spansh's exact name for the commodity-market service, as served by
     * {@code /api/stations/field_values/services}. There is no boolean "has a market" on the stations
     * endpoint - the market requirement is expressed as a service filter.
     */
    public static final String MARKET_SERVICE = "Market";

    @SerializedName("filters")
    private Filters filters = new Filters();

    @SerializedName("reference_coords")
    private ReferenceCoords referenceCoords;

    /**
     * The system distances are measured from, by name, as the alternative to {@link #referenceCoords}.
     * Only one of the two is ever sent: coordinates when we know where the ship is in the galaxy, a name
     * when all we have is the star we are parked at. Spansh answers a request carrying neither with
     * distances measured from Sol, which is nobody's idea of "nearest".
     */
    @SerializedName("reference_system")
    private String referenceSystem;


    @SerializedName("sort")
    private List<Object> sort = Collections.emptyList();

    @SerializedName("size")
    private int size = 10;

    @SerializedName("page")
    private int page = 0;

    @SerializedName("distance")
    private Distance distance;


    // === Setters for builder-style usage ===


    public Distance getDistance() {
        return distance;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    public void setReferenceCoords(ReferenceCoords referenceCoords) {
        this.referenceCoords = referenceCoords;
    }

    public void setReferenceSystem(String referenceSystem) {
        this.referenceSystem = referenceSystem;
    }

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

    // === Nested classes ===
    public static class SystemName {
        @SerializedName("value")
        private String systemName;

        public String getSystemName() {
            return systemName;
        }

        public void setSystemName(String systemName) {
            this.systemName = systemName;
        }
    }

    /**
     * Station types, in Spansh's own vocabulary from {@code /api/stations/field_values/type}. The names are
     * matched exactly and case-sensitively: a value Spansh does not know simply matches no station, so a
     * misspelling here silently narrows the search instead of failing it.
     */
    public static class StationType {

        /**
         * Orbital types that can hold a commodity market. Construction depots are left out - they are
         * colonisation build sites, not trade stops.
         */
        public static final List<String> ORBITAL_TRADE_TYPES = List.of(
                "Asteroid base", "Coriolis Starport", "Dodec Starport", "Mega ship",
                "Ocellus Starport", "Orbis Starport", "Outpost"
        );

        /**
         * Surface types, offered only when the trade profile allows planetary ports. Odyssey settlements
         * are left out: they are numerous enough to crowd out real ports, and the ones that trade at all
         * deal in on-foot goods rather than the ship cargo a trade route moves.
         */
        public static final List<String> PLANETARY_TRADE_TYPES = List.of(
                "Planetary Outpost", "Planetary Port"
        );

        /**
         * Odyssey surface settlements, which are dockable and do carry a ship commodity market
         * ({@code Dock}, {@code Autodock}, {@code Market}, and pads, in Spansh's own service list).
         * <p>
         * Kept out of {@link #PLANETARY_TRADE_TYPES} - and so out of a trade route - because there are
         * 312,000 of them against 80,000 ports, and most of what they trade is on-foot goods rather than
         * the ship cargo a route moves. They belong in a commodity search all the same: measured live,
         * "Micro-weave Cooling Hoses" is stocked by 2,661 settlements and 231 carriers and by nothing else.
         */
        public static final List<String> SETTLEMENT_TRADE_TYPES = List.of("Settlement");

        /**
         * Player-owned, so offered only when the trade profile allows fleet carriers.
         */
        public static final List<String> CARRIER_TRADE_TYPES = List.of("Drake-Class Carrier");

        /**
         * Every type that can hold a ship commodity market.
         * <p>
         * For the search that answers "where does this good exist", which is a different question from
         * "where would I run a trade route". The profile's surface and carrier rules shape a route the
         * commander will fly repeatedly; a commander who asks where to buy 72 units of mission cargo has
         * not asked about any of that, and would never guess that a trade route setting is why the answer
         * came back empty. Measured live, 140 of the 440 goods in our commodities table are on sale at no
         * starport anywhere in the galaxy, so narrowing that search by profile makes them unobtainable.
         */
        public static final List<String> EVERY_TRADE_TYPE = Stream.of(
                        ORBITAL_TRADE_TYPES, PLANETARY_TRADE_TYPES, SETTLEMENT_TRADE_TYPES, CARRIER_TRADE_TYPES)
                .flatMap(List::stream).toList();

        @SerializedName("value")
        List<String> types = ORBITAL_TRADE_TYPES;

        public void setTypes(List<String> types) {
            this.types = types;
        }
    }

    /**
     * Sorts the page nearest-first. Spansh returns results in index order when {@code sort} is empty, which
     * is not distance order, so without this "the nearest station" is whichever row happened to come back
     * first - hundreds of light years out while a neighbour sits on the same page.
     * <p>
     * WHY no direction setter: descending is never what a "nearest station" search wants.
     */
    public static class DistanceSort {

        @SerializedName("distance")
        private final Ascending distance = new Ascending();

        private static class Ascending {
            @SerializedName("direction")
            private final String direction = "asc";
        }
    }

    /**
     * A required station service, as {@code {"name": ["Market"]}}.
     */
    public static class Service {

        @SerializedName("name")
        private final List<String> name;

        public Service(List<String> name) {
            this.name = name;
        }
    }

    public static class Distance {
        @SerializedName("min")
        private String min; // sure it makes sense to use int, but API wants this as String...
        @SerializedName("max")
        private String max; // sure it makes sense to use int, but API wants this as String...

        public void setMin(int min) {
            //API wants this as String...
            this.min = String.valueOf(min);
        }

        public void setMax(int max) {
            //API wants this as String...
            this.max = String.valueOf(max);
        }
    }

    public static class Filters {

        @SerializedName("updated_at")
        private UpdatedAt updatedAt;

        @SerializedName("distance_to_arrival")
        private RangeFilter distanceToArrival;

        @SerializedName("system_name")
        private SystemName systemName;

        @SerializedName("distance")
        private Distance distance;

        @SerializedName("type")
        private StationType stationType;

        @SerializedName("services")
        private List<Service> services;

        @SerializedName("small_pads")
        private RangeFilter smallPads;

        @SerializedName("medium_pads")
        private RangeFilter mediumPads;

        @SerializedName("large_pads")
        private RangeFilter largePads;

        @SerializedName("marketplace")
        private List<Marketplace> marketplace;


        public UpdatedAt getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(UpdatedAt updatedAt) {
            this.updatedAt = updatedAt;
        }

        public SystemName getSystemName() {
            return systemName;
        }

        public void setSystemName(SystemName systemName) {
            this.systemName = systemName;
        }

        public void setStationType(StationType stationType) {
            this.stationType = stationType;
        }

        /**
         * Services the station must offer; see {@link #MARKET_SERVICE}.
         */
        public void setServices(List<Service> services) {
            this.services = services;
        }

        // Setters
        public void setDistanceToArrival(RangeFilter distanceToArrival) {
            this.distanceToArrival = distanceToArrival;
        }

        public void setSmallPads(RangeFilter smallPads) {
            this.smallPads = smallPads;
        }

        public void setMediumPads(RangeFilter mediumPads) {
            this.mediumPads = mediumPads;
        }

        public void setLargePads(RangeFilter largePads) {
            this.largePads = largePads;
        }

        /**
         * Goods the station's market must actually carry; see {@link Marketplace}.
         */
        public void setMarketplace(List<Marketplace> marketplace) {
            this.marketplace = marketplace;
        }

        public void setDistanceToStarSystem(Distance distanceToStarSystem) {
            this.distance = distanceToStarSystem;
        }
    }


    /**
     * A commodity the station's market must carry, and the terms it must carry it on.
     * <p>
     * The whole filter is one nested question about a SINGLE market entry, so supply and price are read
     * against the same commodity rather than anywhere in the market: "has Gold, with at least a hold's
     * worth in stock, at a price above zero" is one {@code Marketplace}, not three filters.
     * <p>
     * WHY the range fields are {@link RangeFilter}s and never a bare minimum: the endpoint accepts
     * {@code {"comparison": ">=", "value": [500]}} without complaint and then matches NOTHING - measured
     * against the live search, where the same query as a {@code <=>} range returned 550 stations. Every
     * bound here is a range with both ends.
     * <p>
     * WHY there is no market filter without one of these: asked for a commodity alone, Spansh returns
     * every station whose market lists it, stock or no stock - and a market listing Gold at supply 0 is
     * a station the commander would fly to and buy nothing at.
     */
    public static class Marketplace {

        /**
         * Commodity names in Spansh's own vocabulary, from {@code /api/stations/field_values/marketplace}.
         * Matched exactly: a name Spansh does not know matches no station rather than failing the search.
         */
        @SerializedName("commodity")
        private final List<String> commodity;

        @SerializedName("supply")
        private RangeFilter supply;

        @SerializedName("demand")
        private RangeFilter demand;

        @SerializedName("buy_price")
        private RangeFilter buyPrice;

        @SerializedName("sell_price")
        private RangeFilter sellPrice;

        public Marketplace(List<String> commodity) {
            this.commodity = commodity;
        }

        /**
         * Units the station must have in stock - what the commander buys.
         */
        public void setSupply(RangeFilter supply) {
            this.supply = supply;
        }

        /**
         * Units the station must be asking for - what the commander sells into.
         */
        public void setDemand(RangeFilter demand) {
            this.demand = demand;
        }

        /**
         * What the commander PAYS. Spansh names the fields from the station's side of the counter, so
         * {@code buy_price} is the ask and {@code sell_price} is the bid; on the same market entry the
         * ask is always the higher of the two.
         */
        public void setBuyPrice(RangeFilter buyPrice) {
            this.buyPrice = buyPrice;
        }

        /**
         * What the commander RECEIVES; see {@link #setBuyPrice}.
         */
        public void setSellPrice(RangeFilter sellPrice) {
            this.sellPrice = sellPrice;
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


    public static class ReferenceCoords {

        @SerializedName("x")
        private double x;

        @SerializedName("y")
        private double y;

        @SerializedName("z")
        private double z;

        public ReferenceCoords() {
        }

        public ReferenceCoords(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        // Setters
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

    public static class RangeFilter {

        @SerializedName("comparison")
        private String comparison = "<=>";  // Spansh uses "<=>" for range

        @SerializedName("value")
        private int[] value = new int[2];

        public RangeFilter() {
        }

        public RangeFilter(int min, int max) {
            this.value[0] = min;
            this.value[1] = max;
        }

        public void setValue(int[] value) {
            this.value = value != null ? value : new int[]{0, 0};
        }

        public void setComparison(String comparison) {
            this.comparison = comparison;
        }
    }
}