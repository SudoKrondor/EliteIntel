package elite.intel.gameapi.search.spansh.station.vista;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class VistaGenomicsLocationDto extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("results")
    private List<Result> results;

    public List<Result> getResults() {
        return results;
    }

    public static class Result implements ToJsonConvertible{

        @SerializedName("body_gravity")
        private double bodyGravity;

        @SerializedName("body_has_atmosphere")
        private boolean bodyHasAtmosphere;

        @SerializedName("body_name")
        private String bodyName;

        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;

        @SerializedName("controlling_minor_faction_influence")
        private double controllingMinorFactionInfluence;

        @SerializedName("controlling_minor_faction_state")
        private String controllingMinorFactionState;

        @SerializedName("distance")
        private double distance;

        @SerializedName("distance_to_arrival")
        private double distanceToArrival;

        @SerializedName("government")
        private String government;

        @SerializedName("has_large_pad")
        private boolean hasLargePad;

        @SerializedName("has_market")
        private boolean hasMarket;

        @SerializedName("id")
        private String id;

        @SerializedName("is_planetary")
        private boolean isPlanetary;

        @SerializedName("large_pads")
        private int largePads;

        @SerializedName("latitude")
        private double latitude;

        @SerializedName("longitude")
        private double longitude;

        @SerializedName("market_id")
        private long marketId;

        @SerializedName("market_updated_at")
        private String marketUpdatedAt;

        @SerializedName("medium_pads")
        private int mediumPads;

        @SerializedName("name")
        private String stationName;

        @SerializedName("primary_economy")
        private String primaryEconomy;

        @SerializedName("services")
        private List<Service> services;

        @SerializedName("small_pads")
        private int smallPads;

        @SerializedName("system_id64")
        private long systemId64;

        @SerializedName("system_is_being_colonised")
        private boolean systemIsBeingColonised;

        @SerializedName("system_is_colonised")
        private boolean systemIsColonised;

        @SerializedName("system_name")
        private String systemName;

        @SerializedName("system_population")
        private long systemPopulation;

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


        public double getBodyGravity() {
            return bodyGravity;
        }

        public boolean isBodyHasAtmosphere() {
            return bodyHasAtmosphere;
        }

        public String getBodyName() {
            return bodyName;
        }

        public String getControllingMinorFaction() {
            return controllingMinorFaction;
        }

        public double getControllingMinorFactionInfluence() {
            return controllingMinorFactionInfluence;
        }

        public String getControllingMinorFactionState() {
            return controllingMinorFactionState;
        }

        public double getDistance() {
            return distance;
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

        public String getId() {
            return id;
        }

        public boolean isPlanetary() {
            return isPlanetary;
        }

        public int getLargePads() {
            return largePads;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
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

        public String getStationName() {
            return stationName;
        }

        public String getPrimaryEconomy() {
            return primaryEconomy;
        }

        public List<Service> getServices() {
            return services;
        }

        public int getSmallPads() {
            return smallPads;
        }

        public long getSystemId64() {
            return systemId64;
        }

        public boolean isSystemIsBeingColonised() {
            return systemIsBeingColonised;
        }

        public boolean isSystemIsColonised() {
            return systemIsColonised;
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

        @Override public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }

    public static class Service {

        @SerializedName("name")
        private String name;

        public String getName() {
            return name;
        }
    }
}