package elite.intel.gameapi.search.spansh.starsystems;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class StationSearchResult extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("results")
    private List<SystemResult> results;


    public List<SystemResult> getResults() {
        return results;
    }

    public static class SystemResult {
        @SerializedName("allegiance") 
        private String allegiance;

        @SerializedName("bodies")
        private List<Body> bodies;

        @SerializedName("body_count")
        private int bodyCount;

        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;

        @SerializedName("controlling_minor_faction_state")
        private String controllingMinorFactionState;

        @SerializedName("controlling_power")
        private String controllingPower;

        @SerializedName("system_power_state")
        private String systemPowerState;

        @SerializedName("distance")
        private double distance;

        @SerializedName("estimated_mapping_value")
        private long estimatedMappingValue;

        @SerializedName("estimated_scan_value")
        private long estimatedScanValue;

        @SerializedName("government")
        private String government;

        @SerializedName("id")
        private String id;

        @SerializedName("id64")
        private long id64;

        @SerializedName("is_being_colonised")
        private boolean isBeingColonised;

        @SerializedName("is_colonised")
        private boolean isColonised;

        @SerializedName("known_permit")
        private boolean knownPermit;

        @SerializedName("landmark_value")
        private int landmarkValue;

        @SerializedName("minor_faction_presences")
        private List<MinorFactionPresence> minorFactionPresences;

        @SerializedName("name")
        private String name;

        @SerializedName("needs_permit")
        private boolean needsPermit;

        @SerializedName("population")
        private long population;

        @SerializedName("power")
        private List<String> power;

        @SerializedName("power_state")
        private String powerState;

        @SerializedName("power_state_control_progress")
        private double powerStateControlProgress;

        @SerializedName("power_state_reinforcement")
        private int powerStateReinforcement;

        @SerializedName("power_state_undermining")
        private int powerStateUndermining;

        @SerializedName("primary_economy")
        private String primaryEconomy;

        @SerializedName("region")
        private String region;

        @SerializedName("secondary_economy")
        private String secondaryEconomy;

        @SerializedName("security")
        private String security;

        @SerializedName("stations")
        private List<Station> stations;

        @SerializedName("thargoid_war_failure_state")
        private String thargoidWarFailureState;

        @SerializedName("thargoid_war_state")
        private String thargoidWarState;

        @SerializedName("thargoid_war_success_state")
        private String thargoidWarSuccessState;

        @SerializedName("updated_at")
        private String updatedAt;

        @SerializedName("x")
        private double x;

        @SerializedName("y")
        private double y;

        @SerializedName("z")
        private double z;


        public String getSystemPowerState() {
            return systemPowerState;
        }

        public String getAllegiance() {
            return allegiance;
        }

        public List<Body> getBodies() {
            return bodies;
        }

        public int getBodyCount() {
            return bodyCount;
        }

        public String getControllingMinorFaction() {
            return controllingMinorFaction;
        }

        public String getControllingMinorFactionState() {
            return controllingMinorFactionState;
        }

        public String getControllingPower() {
            return controllingPower;
        }

        public double getDistance() {
            return distance;
        }

        public long getEstimatedMappingValue() {
            return estimatedMappingValue;
        }

        public long getEstimatedScanValue() {
            return estimatedScanValue;
        }

        public String getGovernment() {
            return government;
        }

        public String getId() {
            return id;
        }

        public long getId64() {
            return id64;
        }

        public boolean isBeingColonised() {
            return isBeingColonised;
        }

        public boolean isColonised() {
            return isColonised;
        }

        public boolean isKnownPermit() {
            return knownPermit;
        }

        public int getLandmarkValue() {
            return landmarkValue;
        }

        public List<MinorFactionPresence> getMinorFactionPresences() {
            return minorFactionPresences;
        }

        public String getName() {
            return name;
        }

        public boolean isNeedsPermit() {
            return needsPermit;
        }

        public long getPopulation() {
            return population;
        }

        public List<String> getPower() {
            return power;
        }

        public String getPowerState() {
            return powerState;
        }

        public double getPowerStateControlProgress() {
            return powerStateControlProgress;
        }

        public int getPowerStateReinforcement() {
            return powerStateReinforcement;
        }

        public int getPowerStateUndermining() {
            return powerStateUndermining;
        }

        public String getPrimaryEconomy() {
            return primaryEconomy;
        }

        public String getRegion() {
            return region;
        }

        public String getSecondaryEconomy() {
            return secondaryEconomy;
        }

        public String getSecurity() {
            return security;
        }

        public List<Station> getStations() {
            return stations;
        }

        public String getThargoidWarFailureState() {
            return thargoidWarFailureState;
        }

        public String getThargoidWarState() {
            return thargoidWarState;
        }

        public String getThargoidWarSuccessState() {
            return thargoidWarSuccessState;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

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

    public static class Body {
        @SerializedName("distance_to_arrival")
        private double distanceToArrival;

        @SerializedName("estimated_mapping_value")
        private long estimatedMappingValue;

        @SerializedName("estimated_scan_value")
        private long estimatedScanValue;

        @SerializedName("id")
        private long id;

        @SerializedName("id64")
        private long id64;

        @SerializedName("is_main_star")
        private Boolean isMainStar;

        @SerializedName("name")
        private String name;

        @SerializedName("subtype")
        private String subtype;

        @SerializedName("terraforming_state")
        private String terraformingState;

        @SerializedName("type")
        private String type;

        public double getDistanceToArrival() {
            return distanceToArrival;
        }

        public long getEstimatedMappingValue() {
            return estimatedMappingValue;
        }

        public long getEstimatedScanValue() {
            return estimatedScanValue;
        }

        public long getId() {
            return id;
        }

        public long getId64() {
            return id64;
        }

        public Boolean getMainStar() {
            return isMainStar;
        }

        public String getName() {
            return name;
        }

        public String getSubtype() {
            return subtype;
        }

        public String getTerraformingState() {
            return terraformingState;
        }

        public String getType() {
            return type;
        }
    }

    public static class MinorFactionPresence {
        @SerializedName("allegiance")
        private String allegiance;

        @SerializedName("government")
        private String government;

        @SerializedName("influence")
        private double influence;

        @SerializedName("name")
        private String name;

        @SerializedName("state")
        private String state;

        public String getAllegiance() {
            return allegiance;
        }

        public String getGovernment() {
            return government;
        }

        public double getInfluence() {
            return influence;
        }

        public String getName() {
            return name;
        }

        public String getState() {
            return state;
        }
    }

    public static class Station {
        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;

        @SerializedName("controlling_minor_faction_state")
        private String controllingMinorFactionState;

        @SerializedName("distance_to_arrival")
        private double distanceToArrival;

        @SerializedName("has_large_pad")
        private boolean hasLargePad;

        @SerializedName("has_market")
        private boolean hasMarket;

        @SerializedName("has_outfitting")
        private boolean hasOutfitting;

        @SerializedName("has_shipyard")
        private boolean hasShipyard;

        @SerializedName("large_pads")
        private Integer largePads;

        @SerializedName("market_id")
        private long marketId;

        @SerializedName("medium_pads")
        private Integer mediumPads;

        @SerializedName("name")
        private String name;

        @SerializedName("small_pads")
        private Integer smallPads;

        @SerializedName("type")
        private String type;

        public String getControllingMinorFaction() {
            return controllingMinorFaction;
        }

        public String getControllingMinorFactionState() {
            return controllingMinorFactionState;
        }

        public double getDistanceToArrival() {
            return distanceToArrival;
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

        public Integer getLargePads() {
            return largePads;
        }

        public long getMarketId() {
            return marketId;
        }

        public Integer getMediumPads() {
            return mediumPads;
        }

        public String getName() {
            return name;
        }

        public Integer getSmallPads() {
            return smallPads;
        }

        public String getType() {
            return type;
        }
    }
}
