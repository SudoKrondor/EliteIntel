package elite.intel.gameapi.search.spansh.starsystems;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class StarSystemResult extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("record")
    private SystemRecord record;

    public SystemRecord getRecord() {
        return record;
    }

    public static class SystemRecord {

        private String allegiance;
        private List<Body> bodies;
        @SerializedName("body_count")
        private Integer bodyCount;
        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;
        @SerializedName("controlling_minor_faction_state")
        private String controllingMinorFactionState;
        @SerializedName("controlling_power")
        private String controllingPower;
        @SerializedName("estimated_mapping_value")
        private Integer estimatedMappingValue;
        @SerializedName("estimated_scan_value")
        private Integer estimatedScanValue;
        private String government;
        @SerializedName("id64")
        private Long id64;
        @SerializedName("is_being_colonised")
        private Boolean isBeingColonised;
        @SerializedName("is_colonised")
        private Boolean isColonised;
        @SerializedName("known_permit")
        private Boolean knownPermit;
        @SerializedName("landmark_value")
        private Integer landmarkValue;
        @SerializedName("minor_faction_presences")
        private List<MinorFactionPresence> minorFactionPresences;
        private String name;
        @SerializedName("needs_permit")
        private Boolean needsPermit;
        private Long population;
        private List<String> power;
        @SerializedName("power_state")
        private String powerState;
        @SerializedName("power_state_control_progress")
        private Double powerStateControlProgress;
        @SerializedName("power_state_reinforcement")
        private Integer powerStateReinforcement;
        @SerializedName("power_state_undermining")
        private Integer powerStateUndermining;
        @SerializedName("primary_economy")
        private String primaryEconomy;
        private String region;
        @SerializedName("secondary_economy")
        private String secondaryEconomy;
        private String security;
        private List<Station> stations;
        @SerializedName("thargoid_war_failure_state")
        private String thargoidWarFailureState;
        @SerializedName("thargoid_war_state")
        private String thargoidWarState;
        @SerializedName("thargoid_war_success_state")
        private String thargoidWarSuccessState;
        @SerializedName("updated_at")
        private String updatedAt;
        private Double x;
        private Double y;
        private Double z;

        // getters
        public String getAllegiance() { return allegiance; }
        public List<Body> getBodies() { return bodies; }
        public Integer getBodyCount() { return bodyCount; }
        public String getControllingMinorFaction() { return controllingMinorFaction; }
        public String getControllingMinorFactionState() { return controllingMinorFactionState; }
        public String getControllingPower() { return controllingPower; }
        public Integer getEstimatedMappingValue() { return estimatedMappingValue; }
        public Integer getEstimatedScanValue() { return estimatedScanValue; }
        public String getGovernment() { return government; }
        public Long getId64() { return id64; }
        public Boolean getIsBeingColonised() { return isBeingColonised; }
        public Boolean getIsColonised() { return isColonised; }
        public Boolean getKnownPermit() { return knownPermit; }
        public Integer getLandmarkValue() { return landmarkValue; }
        public List<MinorFactionPresence> getMinorFactionPresences() { return minorFactionPresences; }
        public String getName() { return name; }
        public Boolean getNeedsPermit() { return needsPermit; }
        public Long getPopulation() { return population; }
        public List<String> getPower() { return power; }
        public String getPowerState() { return powerState; }
        public Double getPowerStateControlProgress() { return powerStateControlProgress; }
        public Integer getPowerStateReinforcement() { return powerStateReinforcement; }
        public Integer getPowerStateUndermining() { return powerStateUndermining; }
        public String getPrimaryEconomy() { return primaryEconomy; }
        public String getRegion() { return region; }
        public String getSecondaryEconomy() { return secondaryEconomy; }
        public String getSecurity() { return security; }
        public List<Station> getStations() { return stations; }
        public String getThargoidWarFailureState() { return thargoidWarFailureState; }
        public String getThargoidWarState() { return thargoidWarState; }
        public String getThargoidWarSuccessState() { return thargoidWarSuccessState; }
        public String getUpdatedAt() { return updatedAt; }
        public Double getX() { return x; }
        public Double getY() { return y; }
        public Double getZ() { return z; }
    }

    public static class Body {
        @SerializedName("distance_to_arrival")
        private Double distanceToArrival;
        @SerializedName("estimated_mapping_value")
        private Integer estimatedMappingValue;
        @SerializedName("estimated_scan_value")
        private Integer estimatedScanValue;
        private Long id;
        @SerializedName("id64")
        private Long id64;
        @SerializedName("is_main_star")
        private Boolean isMainStar;
        private String name;
        private String subtype;
        private String type;
        @SerializedName("terraforming_state")
        private String terraformingState;

        // getters
        public Double getDistanceToArrival() { return distanceToArrival; }
        public Integer getEstimatedMappingValue() { return estimatedMappingValue; }
        public Integer getEstimatedScanValue() { return estimatedScanValue; }
        public Long getId() { return id; }
        public Long getId64() { return id64; }
        public Boolean getIsMainStar() { return isMainStar; }
        public String getName() { return name; }
        public String getSubtype() { return subtype; }
        public String getType() { return type; }
        public String getTerraformingState() { return terraformingState; }
    }

    public static class MinorFactionPresence {
        private String allegiance;
        private String government;
        private Double influence;
        private String name;
        private String state;

        // getters
        public String getAllegiance() { return allegiance; }
        public String getGovernment() { return government; }
        public Double getInfluence() { return influence; }
        public String getName() { return name; }
        public String getState() { return state; }
    }

    public static class Station {
        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;
        @SerializedName("controlling_minor_faction_state")
        private String controllingMinorFactionState;
        @SerializedName("distance_to_arrival")
        private Double distanceToArrival;
        @SerializedName("has_large_pad")
        private Boolean hasLargePad;
        @SerializedName("has_market")
        private Boolean hasMarket;
        @SerializedName("has_outfitting")
        private Boolean hasOutfitting;
        @SerializedName("has_shipyard")
        private Boolean hasShipyard;
        @SerializedName("large_pads")
        private Integer largePads;
        @SerializedName("market_id")
        private Long marketId;
        @SerializedName("medium_pads")
        private Integer mediumPads;
        private String name;
        @SerializedName("small_pads")
        private Integer smallPads;
        private String type;

        // getters
        public String getControllingMinorFaction() { return controllingMinorFaction; }
        public String getControllingMinorFactionState() { return controllingMinorFactionState; }
        public Double getDistanceToArrival() { return distanceToArrival; }
        public Boolean getHasLargePad() { return hasLargePad; }
        public Boolean getHasMarket() { return hasMarket; }
        public Boolean getHasOutfitting() { return hasOutfitting; }
        public Boolean getHasShipyard() { return hasShipyard; }
        public Integer getLargePads() { return largePads; }
        public Long getMarketId() { return marketId; }
        public Integer getMediumPads() { return mediumPads; }
        public String getName() { return name; }
        public Integer getSmallPads() { return smallPads; }
        public String getType() { return type; }
    }
}