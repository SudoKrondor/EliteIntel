package elite.intel.gameapi.search.spansh.station.traderandbroker;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.gameapi.search.spansh.station.StationSearchHit;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TraderAndBrokerSearchDto extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("count")
    private int count;

    @SerializedName("from")
    private int from;

    @SerializedName("results")
    private List<Result> results;

    @SerializedName("search")
    private Search search;

    @SerializedName("search_reference")
    private String searchReference;

    @SerializedName("size")
    private int size;

    public List<Result> getResults() {
        if (results == null) return Collections.emptyList();
        results.sort(Comparator.comparingDouble(Result::getDistance));
        return results;
    }

    public static class Result implements ToJsonConvertible, StationSearchHit {

        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;

        @SerializedName("controlling_minor_faction_influence")
        private double controllingMinorFactionInfluence;

        @SerializedName("distance_to_arrival")
        private double distanceToArrival;

        @SerializedName("distance")
        private double distance;

        @SerializedName("government")
        private String government;

        @SerializedName("has_large_pad")
        private boolean hasLargePad;

        @SerializedName("has_market")
        private boolean hasMarket;

        @SerializedName("has_outfitting")
        private boolean hasOutfitting;

        @SerializedName("has_shipyard")
        private boolean hasShipyard;

        @SerializedName("id")
        private String id;

        @SerializedName("is_planetary")
        private boolean isPlanetary;

        @SerializedName("large_pads")
        private int largePads;

        @SerializedName("market_id")
        private long marketId;

        @SerializedName("material_trader")
        private String materialTrader;

        @SerializedName("medium_pads")
        private int mediumPads;

        @SerializedName("name")
        private String stationName;

        @SerializedName("primary_economy")
        private String primaryEconomy;

        @SerializedName("secondary_economy")
        private String secondaryEconomy;

        @SerializedName("small_pads")
        private int smallPads;

        @SerializedName("system_controlling_power")
        private String systemControllingPower;

        @SerializedName("system_name")
        private String systemName;

        @SerializedName("system_population")
        private long systemPopulation;

        @SerializedName("system_power")
        private List<String> systemPower;

        @SerializedName("system_power_state")
        private String systemPowerState;

        @SerializedName("system_primary_economy")
        private String systemPrimaryEconomy;

        @SerializedName("system_secondary_economy")
        private String systemSecondaryEconomy;

        @SerializedName("system_x")
        private double systemX;

        @SerializedName("system_y")
        private double systemY;

        @SerializedName("system_z")
        private double systemZ;

        @SerializedName("technology_broker")
        private String technologyBroker;

        @SerializedName("type")
        private String type;

        @SerializedName("shipyard_updated_at")
        private String shipyardUpdatedAt;

        @SerializedName("updated_at")
        private String updatedAt;


        public String getControllingMinorFaction() {
            return controllingMinorFaction;
        }

        public double getControllingMinorFactionInfluence() {
            return controllingMinorFactionInfluence;
        }

        public double getDistanceToArrival() {
            return distanceToArrival;
        }

        public String getGovernment() {
            return government;
        }

        public boolean isHasLargePad() {
            return hasLargePad;
        }

        public boolean isHasMarket() {
            return hasMarket;
        }

        public boolean isHasOutfitting() {
            return hasOutfitting;
        }

        public boolean isHasShipyard() {
            return hasShipyard;
        }

        public String getId() {
            return id;
        }

        public boolean isPlanetary() {
            return isPlanetary;
        }

        public int getLargePads() {
            return largePads;
        }

        public long getMarketId() {
            return marketId;
        }

        public String getMaterialTrader() {
            return materialTrader;
        }

        public int getMediumPads() {
            return mediumPads;
        }

        public String getStationName() {
            return stationName;
        }

        public String getPrimaryEconomy() {
            return primaryEconomy;
        }

        public String getSecondaryEconomy() {
            return secondaryEconomy;
        }

        public int getSmallPads() {
            return smallPads;
        }

        public String getSystemControllingPower() {
            return systemControllingPower;
        }

        public String getSystemName() {
            return systemName;
        }

        public long getSystemPopulation() {
            return systemPopulation;
        }

        public List<String> getSystemPower() {
            return systemPower;
        }

        public String getSystemPowerState() {
            return systemPowerState;
        }

        public String getSystemPrimaryEconomy() {
            return systemPrimaryEconomy;
        }

        public String getSystemSecondaryEconomy() {
            return systemSecondaryEconomy;
        }

        public double getSystemX() {
            return systemX;
        }

        public double getSystemY() {
            return systemY;
        }

        public double getSystemZ() {
            return systemZ;
        }

        public String getTechnologyBroker() {
            return technologyBroker;
        }

        public String getType() {
            return type;
        }

        public String getShipyardUpdatedAt() {
            return shipyardUpdatedAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public double getDistance() {
            return Math.round(distance * 100.0) / 100.0;
        }

        @Override public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }

    public static class Search {

        @SerializedName("filters")
        private Filters filters;

        @SerializedName("page")
        private int page;

        @SerializedName("reference_coords")
        private ReferenceCoords referenceCoords;

        @SerializedName("size")
        private int size;

        @SerializedName("sort")
        private List<Object> sort;
    }

    public static class Filters {

        @SerializedName("distance")
        private Distance distance;

        @SerializedName("material_trader")
        private MaterialTrader materialTrader;
    }

    public static class Distance {

        @SerializedName("max")
        private String max;

        @SerializedName("min")
        private String min;
    }

    public static class MaterialTrader {

        @SerializedName("value")
        private List<String> value;
    }

    public static class ReferenceCoords {

        @SerializedName("x")
        private int x;

        @SerializedName("y")
        private int y;

        @SerializedName("z")
        private int z;
    }
}