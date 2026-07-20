package elite.intel.gameapi.search.spansh.findcarrier;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FleetCarrierSearchResultsDto implements ToJsonConvertible {

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

    public int getCount() {
        return count;
    }

    public int getFrom() {
        return from;
    }

    public List<Result> getResults() {
        if (results == null) return Collections.emptyList();
        results.sort(Comparator.comparingDouble(Result::getDistance));
        return results;
    }

    public Search getSearch() {
        return search;
    }

    public String getSearchReference() {
        return searchReference;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    public static class Result {

        @SerializedName("carrier_docking_access")
        private String carrierDockingAccess;

        @SerializedName("carrier_name")
        private String carrierName;                 

        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;

        @SerializedName("distance")
        private double distance;

        @SerializedName("distance_to_arrival")
        private double distanceToArrival;

        @SerializedName("economies")
        private List<Economy> economies;

        @SerializedName("export_commodities")
        private List<Commodity> exportCommodities;   

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

        @SerializedName("import_commodities")
        private List<Commodity> importCommodities;   

        @SerializedName("is_planetary")
        private boolean isPlanetary;

        @SerializedName("large_pads")
        private int largePads;

        @SerializedName("market")
        private List<MarketEntry> market;           

        @SerializedName("market_id")
        private long marketId;

        @SerializedName("market_updated_at")
        private String marketUpdatedAt;             

        @SerializedName("medium_pads")
        private int mediumPads;

        @SerializedName("name")
        private String callSign;

        @SerializedName("primary_economy")
        private String primaryEconomy;

        @SerializedName("services")
        private List<Service> services;

        @SerializedName("ships")
        private List<Ship> ships;                   

        @SerializedName("shipyard_updated_at")
        private String shipyardUpdatedAt;           

        @SerializedName("small_pads")
        private int smallPads;

        @SerializedName("system_id64")
        private long systemId64;

        @SerializedName("system_name")
        private String systemName;

        @SerializedName("system_power")
        private List<String> systemPower;

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

        @SerializedName("type")
        private String type;

        @SerializedName("updated_at")
        private String updatedAt;

        public String getCarrierDockingAccess() {
            return carrierDockingAccess;
        }

        public String getCarrierName() {
            return carrierName;
        }

        public String getControllingMinorFaction() {
            return controllingMinorFaction;
        }

        public double getDistance() {
            return distance;
        }

        public double getDistanceToArrival() {
            return distanceToArrival;
        }

        public List<Economy> getEconomies() {
            return economies;
        }

        public List<Commodity> getExportCommodities() {
            return exportCommodities;
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

        public List<Commodity> getImportCommodities() {
            return importCommodities;
        }

        public boolean isPlanetary() {
            return isPlanetary;
        }

        public int getLargePads() {
            return largePads;
        }

        public List<MarketEntry> getMarket() {
            return market;
        }

        public long getMarketId() {
            return marketId;
        }

        public String getMarketUpdatedAt() {
            return marketUpdatedAt;
        }

        public int getMediumPads() {
            return mediumPads;
        }

        public String getCallSign() {
            return callSign;
        }

        public String getPrimaryEconomy() {
            return primaryEconomy;
        }

        public List<Service> getServices() {
            return services;
        }

        public List<Ship> getShips() {
            return ships;
        }

        public String getShipyardUpdatedAt() {
            return shipyardUpdatedAt;
        }

        public int getSmallPads() {
            return smallPads;
        }

        public long getSystemId64() {
            return systemId64;
        }

        public String getSystemName() {
            return systemName;
        }

        public List<String> getSystemPower() {
            return systemPower;
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

        public String getType() {
            return type;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }
    }

    public static class Economy {
        @SerializedName("name")  private String name;
        @SerializedName("share") private double share;

        public String getName() {
            return name;
        }

        public double getShare() {
            return share;
        }
    }

    public static class Commodity {
        @SerializedName("name") private String name;

        public String getName() {
            return name;
        }
    }

    public static class MarketEntry {
        @SerializedName("buy_price")   private long buyPrice;
        @SerializedName("category")    private String category;
        @SerializedName("commodity")   private String commodity;
        @SerializedName("demand")      private int demand;
        @SerializedName("sell_price")  private long sellPrice;
        @SerializedName("supply")      private int supply;

        public long getBuyPrice() {
            return buyPrice;
        }

        public String getCategory() {
            return category;
        }

        public String getCommodity() {
            return commodity;
        }

        public int getDemand() {
            return demand;
        }

        public long getSellPrice() {
            return sellPrice;
        }

        public int getSupply() {
            return supply;
        }
    }

    public static class Service {
        @SerializedName("name") private String name;

        public String getName() {
            return name;
        }
    }

    public static class Ship {
        @SerializedName("name")   private String name;
        @SerializedName("price")  private long price;
        @SerializedName("symbol") private String symbol;

        public String getName() {
            return name;
        }

        public long getPrice() {
            return price;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public static class Search {
        @SerializedName("filters")         private Filters filters;
        @SerializedName("page")            private int page;
        @SerializedName("reference_coords")private ReferenceCoords referenceCoords;
        @SerializedName("size")            private int size;
        @SerializedName("sort")            private List<Object> sort;

        public Filters getFilters() {
            return filters;
        }

        public int getPage() {
            return page;
        }

        public ReferenceCoords getReferenceCoords() {
            return referenceCoords;
        }

        public int getSize() {
            return size;
        }

        public List<Object> getSort() {
            return sort;
        }
    }

    public static class Filters {
        @SerializedName("carrier_docking_access") private CarrierDockingAccess carrierDockingAccess;
        @SerializedName("distance")               private Distance distance;

        public CarrierDockingAccess getCarrierDockingAccess() {
            return carrierDockingAccess;
        }

        public Distance getDistance() {
            return distance;
        }
    }

    public static class CarrierDockingAccess {
        @SerializedName("value") private List<String> value;

        public List<String> getValue() {
            return value;
        }
    }

    public static class Distance {
        @SerializedName("min") private String min;
        @SerializedName("max") private String max;

        public String getMin() {
            return min;
        }

        public String getMax() {
            return max;
        }
    }

    public static class ReferenceCoords {
        @SerializedName("x") private double x;
        @SerializedName("y") private double y;
        @SerializedName("z") private double z;

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }
}