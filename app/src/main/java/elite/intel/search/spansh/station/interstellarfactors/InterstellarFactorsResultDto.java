package elite.intel.search.spansh.station.interstellarfactors;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InterstellarFactorsResultDto extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("count")
    private int count;

    @SerializedName("from")
    private int from;

    @SerializedName("reference")
    private Reference reference;

    @SerializedName("results")
    private List<Result> results;

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

    public Reference getReference() {
        return reference;
    }

    public String getSearchReference() {
        return searchReference;
    }

    public int getSize() {
        return size;
    }

    public List<Result> getResults() {
        if (results == null) return Collections.emptyList();
        results.sort(Comparator.comparingDouble(Result::getDistance));
        return results;
    }

    public static class Reference {

        @SerializedName("id64")
        private long id64;

        @SerializedName("name")
        private String name;

        @SerializedName("x")
        private double x;

        @SerializedName("y")
        private double y;

        @SerializedName("z")
        private double z;

        public long getId64() {
            return id64;
        }

        public String getName() {
            return name;
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

    public static class Result implements ToJsonConvertible {

        @SerializedName("allegiance")
        private String allegiance;

        @SerializedName("body_gravity")
        private double bodyGravity;

        @SerializedName("body_has_atmosphere")
        private boolean bodyHasAtmosphere;

        @SerializedName("body_has_rings")
        private boolean bodyHasRings;

        @SerializedName("body_id64")
        private long bodyId64;

        @SerializedName("body_name")
        private String bodyName;

        @SerializedName("body_subtype")
        private String bodySubtype;

        @SerializedName("body_type")
        private String bodyType;

        @SerializedName("controlling_minor_faction")
        private String controllingMinorFaction;

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

        @SerializedName("system_controlling_power")
        private String systemControllingPower;

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

        @SerializedName("type")
        private String type;

        @SerializedName("updated_at")
        private String updatedAt;

        public String getAllegiance() {
            return allegiance;
        }

        public double getBodyGravity() {
            return bodyGravity;
        }

        public boolean isBodyHasAtmosphere() {
            return bodyHasAtmosphere;
        }

        public boolean isBodyHasRings() {
            return bodyHasRings;
        }

        public long getBodyId64() {
            return bodyId64;
        }

        public String getBodyName() {
            return bodyName;
        }

        public String getBodySubtype() {
            return bodySubtype;
        }

        public String getBodyType() {
            return bodyType;
        }

        public String getControllingMinorFaction() {
            return controllingMinorFaction;
        }

        public double getDistance() {
            return Math.round(distance * 100.0) / 100.0;
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

        public String getSystemControllingPower() {
            return systemControllingPower;
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

        public String getType() {
            return type;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        @Override
        public String toJson() {
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
