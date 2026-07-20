package elite.intel.gameapi.search.spansh.station.marketstation;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class TradeStationSearchResultDto extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("results")
    private List<StationResult> results;

    // Getter / Setter
    public List<StationResult> getResults() {
        return results;
    }

    public void setResults(List<StationResult> results) {
        this.results = results;
    }

    public static class StationResult {

        @SerializedName("allegiance")
        private String allegiance;

        @SerializedName("carrier_docking_access")
        private String carrierDockingAccess;

        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;

        @SerializedName("controlling_minor_faction_influence")
        private Double controllingMinorFactionInfluence;

        @SerializedName("controlling_minor_faction_state")
        private String controllingMinorFactionState;

        @SerializedName("distance")
        private Double distance; // ly from reference coords

        @SerializedName("distance_to_arrival")
        private Double distanceToArrival; // ls from system entry

        @SerializedName("economies")
        private List<Economy> economies;

        @SerializedName("export_commodities")
        private List<Commodity> exportCommodities;

        @SerializedName("government")
        private String government;

        @SerializedName("has_large_pad")
        private Boolean hasLargePad;

        @SerializedName("has_market")
        private Boolean hasMarket;

        @SerializedName("has_outfitting")
        private Boolean hasOutfitting;

        @SerializedName("has_shipyard")
        private Boolean hasShipyard;

        @SerializedName("id")
        private String id;                               // string because Spansh uses 64-bit ints

        @SerializedName("import_commodities")
        private List<Commodity> importCommodities;

        @SerializedName("is_planetary")
        private Boolean isPlanetary;

        @SerializedName("large_pads")
        private Integer largePads;

        @SerializedName("market")
        private List<MarketEntry> market;

        @SerializedName("market_id")
        private String marketId;

        @SerializedName("market_updated_at")
        private String marketUpdatedAt;

        @SerializedName("medium_pads")
        private Integer mediumPads;

        @SerializedName("modules")
        private List<Module> modules;

        @SerializedName("name")
        private String name;

        @SerializedName("outfitting_updated_at")
        private String outfittingUpdatedAt;

        @SerializedName("primary_economy")
        private String primaryEconomy;

        @SerializedName("prohibited_commodities")
        private List<Commodity> prohibitedCommodities;

        @SerializedName("services")
        private List<Service> services;

        @SerializedName("ships")
        private List<Ship> ships;

        @SerializedName("shipyard_updated_at")
        private String shipyardUpdatedAt;

        @SerializedName("small_pads")
        private Integer smallPads;

        @SerializedName("system_controlling_power")
        private String systemControllingPower;

        @SerializedName("system_id64")
        private String systemId64;

        @SerializedName("system_is_being_colonised")
        private Boolean systemIsBeingColonised;

        @SerializedName("system_is_colonised")
        private Boolean systemIsColonised;

        @SerializedName("system_name")
        private String systemName;

        @SerializedName("system_population")
        private Long systemPopulation;

        @SerializedName("system_power")
        private List<String> systemPower;

        @SerializedName("system_power_state")
        private String systemPowerState;

        @SerializedName("system_primary_economy")
        private String systemPrimaryEconomy;

        @SerializedName("system_secondary_economy")
        private String systemSecondaryEconomy;

        @SerializedName("system_x")
        private Double systemX;

        @SerializedName("system_y")
        private Double systemY;

        @SerializedName("system_z")
        private Double systemZ;

        @SerializedName("type")
        private String type; // e.g. "Orbis Starport", "Drake-Class Carrier"

        @SerializedName("updated_at")
        private String updatedAt;

        public String getAllegiance() {
            return allegiance;
        }

        public String getCarrierDockingAccess() {
            return carrierDockingAccess;
        }

        public String getControllingMinorFaction() {
            return controllingMinorFaction;
        }

        public Double getControllingMinorFactionInfluence() {
            return controllingMinorFactionInfluence;
        }

        public String getControllingMinorFactionState() {
            return controllingMinorFactionState;
        }

        public Double getDistance() {
            return distance;
        }

        public Double getDistanceToArrival() {
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

        public Boolean getHasLargePad() {
            return hasLargePad;
        }

        public Boolean getHasMarket() {
            return hasMarket;
        }

        public Boolean getHasOutfitting() {
            return hasOutfitting;
        }

        public Boolean getHasShipyard() {
            return hasShipyard;
        }

        public String getId() {
            return id;
        }

        public List<Commodity> getImportCommodities() {
            return importCommodities;
        }

        public Boolean getPlanetary() {
            return isPlanetary;
        }

        public Integer getLargePads() {
            return largePads;
        }

        public List<MarketEntry> getMarket() {
            return market;
        }

        public String getMarketId() {
            return marketId;
        }

        public String getMarketUpdatedAt() {
            return marketUpdatedAt;
        }

        public Integer getMediumPads() {
            return mediumPads;
        }

        public List<Module> getModules() {
            return modules;
        }

        public String getName() {
            return name;
        }

        public String getOutfittingUpdatedAt() {
            return outfittingUpdatedAt;
        }

        public String getPrimaryEconomy() {
            return primaryEconomy;
        }

        public List<Commodity> getProhibitedCommodities() {
            return prohibitedCommodities;
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

        public Integer getSmallPads() {
            return smallPads;
        }

        public String getSystemControllingPower() {
            return systemControllingPower;
        }

        public String getSystemId64() {
            return systemId64;
        }

        public Boolean getSystemIsBeingColonised() {
            return systemIsBeingColonised;
        }

        public Boolean getSystemIsColonised() {
            return systemIsColonised;
        }

        public String getSystemName() {
            return systemName;
        }

        public Long getSystemPopulation() {
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

        public Double getSystemX() {
            return systemX;
        }

        public Double getSystemY() {
            return systemY;
        }

        public Double getSystemZ() {
            return systemZ;
        }

        public String getType() {
            return type;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public static class Economy {
            @SerializedName("name")
            private String name;
            @SerializedName("share")
            private Double share;

            public String getName() { return name; }
            public Double getShare() { return share; }
        }

        public static class Commodity {
            @SerializedName("name")
            private String name;

            public String getName() { return name; }
        }

        public static class MarketEntry {
            @SerializedName("buy_price")
            private Integer buyPrice;
            @SerializedName("category")
            private String category;
            @SerializedName("commodity")
            private String commodity;
            @SerializedName("demand")
            private Long demand;
            @SerializedName("sell_price")
            private Integer sellPrice;
            @SerializedName("supply")
            private Long supply;

            public Integer getBuyPrice() {
                return buyPrice;
            }

            public String getCategory() {
                return category;
            }

            public String getCommodity() {
                return commodity;
            }

            public Long getDemand() {
                return demand;
            }

            public Integer getSellPrice() {
                return sellPrice;
            }

            public Long getSupply() {
                return supply;
            }
        }

        public static class Module {
            @SerializedName("category")
            private String category;
            @SerializedName("class")
            private Integer moduleClass;
            @SerializedName("ed_symbol")
            private String edSymbol;
            @SerializedName("name")
            private String name;
            @SerializedName("price")
            private Long price;
            @SerializedName("rating")
            private String rating;
            @SerializedName("ship")
            private String ship;
            @SerializedName("weapon_mode")
            private String weaponMode;

            public String getCategory() {
                return category;
            }

            public Integer getModuleClass() {
                return moduleClass;
            }

            public String getEdSymbol() {
                return edSymbol;
            }

            public String getName() {
                return name;
            }

            public Long getPrice() {
                return price;
            }

            public String getRating() {
                return rating;
            }

            public String getShip() {
                return ship;
            }

            public String getWeaponMode() {
                return weaponMode;
            }
        }

        public static class Service {
            @SerializedName("name")
            private String name;

            public String getName() { return name; }
        }

        public static class Ship {
            @SerializedName("name")
            private String name;
            @SerializedName("price")
            private Long price;
            @SerializedName("symbol")
            private String symbol;

            public String getName() { return name; }
            public Long getPrice() { return price; }
        }
    }
}