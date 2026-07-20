package elite.intel.gameapi.search.spansh.stellarobjects;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StellarObjectSearchResultDto extends BaseJsonDto implements ToJsonConvertible {

    @SerializedName("count")
    private int count;

    @SerializedName("from")
    private int from;

    @SerializedName("reference")
    private Reference reference;

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

    public Reference getReference() {
        return reference;
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

    public static class Reference {

        @SerializedName("id64")
        private long id64;

        @SerializedName("name")
        private String materialName;

        @SerializedName("x")
        private double x;

        @SerializedName("y")
        private double y;

        @SerializedName("z")
        private double z;

        public long getId64() {
            return id64;
        }

        public String getMaterialName() {
            return materialName;
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

    public static class Genus {

        @SerializedName("name")
        String values;

        public String getValues() {
            return values;
        }
    }

    public static class Result implements ToJsonConvertible {

        @SerializedName("genuses")
        private List<Genus> genuses;

        @SerializedName("materials")
        private List<Material> materials;

        @SerializedName("signals")
        private List<Signal> signals;

        @SerializedName("signal_count")
        private Integer signalCount;

        @SerializedName("synthesis_recipes")
        private List<SynthesisRecipe> synthesisRecipes;

        // Existing fields (kept as-is, only showing the ones you already had + new ones)
        @SerializedName("arg_of_periapsis")
        private double argOfPeriapsis;

        @SerializedName("ascending_node")
        private Double ascendingNode;

        @SerializedName("atmosphere")
        private String atmosphere;

        @SerializedName("atmosphere_composition")
        private List<Composition> atmosphereComposition;

        @SerializedName("axis_tilt")
        private double axisTilt;

        @SerializedName("body_id")
        private int bodyId;

        @SerializedName("distance")
        private double distance;

        @SerializedName("distance_to_arrival")
        private double distanceToArrival;

        @SerializedName("earth_masses")
        private double eMasses;

        @SerializedName("estimated_mapping_value")
        private int estimatedMappingValue;

        @SerializedName("estimated_scan_value")
        private int estimatedScanValue;

        @SerializedName("gravity")
        private double gravity;

        @SerializedName("id")
        private String id;

        @SerializedName("id64")
        private long id64;

        @SerializedName("is_landable")
        private boolean isLandable;

        @SerializedName("is_main_star")
        private Boolean isMainStar;

        @SerializedName("is_rotational_period_tidally_locked")
        private boolean isRotationalPeriodTidallyLocked;

        @SerializedName("mean_anomaly")
        private Double meanAnomaly;

        @SerializedName("name")
        private String bodyName;

        @SerializedName("orbital_eccentricity")
        private double orbitalEccentricity;

        @SerializedName("orbital_inclination")
        private double orbitalInclination;

        @SerializedName("orbital_period")
        private double orbitalPeriod;

        @SerializedName("orbital_synchronicity")
        private Double orbitalSynchronicity;

        @SerializedName("parents")
        private List<Parent> parents;

        @SerializedName("radius")
        private double radius;

        @SerializedName("reserve_level")
        private String reserveLevel;

        @SerializedName("rings")
        private List<Ring> rings;

        @SerializedName("rotational_period")
        private double rotationalPeriod;

        @SerializedName("semi_major_axis")
        private double semiMajorAxis;

        @SerializedName("solid_composition")
        private List<Composition> solidComposition;

        @SerializedName("subtype")
        private String subtype;

        @SerializedName("surface_pressure")
        private double surfacePressure;

        @SerializedName("surface_temperature")
        private double surfaceTemperature;

        @SerializedName("system_id64")
        private long systemId64;

        @SerializedName("system_name")
        private String systemName;

        @SerializedName("system_region")
        private String systemRegion;

        @SerializedName("system_x")
        private double x;

        @SerializedName("system_y")
        private double y;

        @SerializedName("system_z")
        private double z;

        @SerializedName("terraforming_state")
        private String terraformingState;

        @SerializedName("type")
        private String type;

        @SerializedName("updated_at")
        private String updatedAt;

        @SerializedName("volcanism_type")
        private String volcanismType;

        public List<Genus> getGenus() {
            return genuses;
        }

        public double getArgOfPeriapsis() {
            return argOfPeriapsis;
        }

        public Double getAscendingNode() {
            return ascendingNode;
        }

        public String getAtmosphere() {
            return atmosphere;
        }

        public List<Composition> getAtmosphereComposition() {
            return atmosphereComposition;
        }

        public double getAxisTilt() {
            return axisTilt;
        }

        public int getBodyId() {
            return bodyId;
        }

        public double getDistance() {
            return Math.round(distance * 100.0) / 100.0;
        }

        public double getDistanceToArrival() {
            return distanceToArrival;
        }

        public double geteMasses() {
            return eMasses;
        }

        public int getEstimatedMappingValue() {
            return estimatedMappingValue;
        }

        public int getEstimatedScanValue() {
            return estimatedScanValue;
        }

        public double getGravity() {
            return gravity;
        }

        public String getId() {
            return id;
        }

        public long getId64() {
            return id64;
        }

        public boolean isLandable() {
            return isLandable;
        }

        public Boolean getIsMainStar() {
            return isMainStar;
        }

        public boolean isRotationalPeriodTidallyLocked() {
            return isRotationalPeriodTidallyLocked;
        }

        public Double getMeanAnomaly() {
            return meanAnomaly;
        }

        public String getBodyName() {
            return bodyName;
        }

        public double getOrbitalEccentricity() {
            return orbitalEccentricity;
        }

        public double getOrbitalInclination() {
            return orbitalInclination;
        }

        public double getOrbitalPeriod() {
            return orbitalPeriod;
        }

        public Double getOrbitalSynchronicity() {
            return orbitalSynchronicity;
        }

        public List<Parent> getParents() {
            return parents;
        }

        public double getRadius() {
            return radius;
        }

        public String getReserveLevel() {
            return reserveLevel;
        }

        public List<Ring> getRings() {
            return rings;
        }

        public double getRotationalPeriod() {
            return rotationalPeriod;
        }

        public double getSemiMajorAxis() {
            return semiMajorAxis;
        }

        public List<Composition> getSolidComposition() {
            return solidComposition;
        }

        public String getSubtype() {
            return subtype;
        }

        public double getSurfacePressure() {
            return surfacePressure;
        }

        public double getSurfaceTemperature() {
            return surfaceTemperature;
        }

        public String getSystemName() {
            return systemName;
        }

        public String getSystemRegion() {
            return systemRegion;
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

        public String getTerraformingState() {
            return terraformingState;
        }

        public String getType() {
            return type;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public String getVolcanismType() {
            return volcanismType;
        }

        @Override public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }

    public static class Material {
        @SerializedName("name") private String name;
        @SerializedName("share") private double share;
        // getters
        public String getName() { return name; }
        public double getShare() { return share; }
    }

    public static class SynthesisRecipe {
        @SerializedName("level") private String level;
        @SerializedName("name") private String name;
        // getters
        public String getLevel() { return level; }
        public String getName() { return name; }
    }



    public static class Composition {

        @SerializedName("name")
        private String name;

        @SerializedName("share")
        private double share;

        public String getName() {
            return name;
        }

        public double getShare() {
            return share;
        }
    }

    public static class Parent {

        @SerializedName("id64")
        private long id64;

        @SerializedName("subtype")
        private String subtype;

        @SerializedName("type")
        private String type;

        public long getId64() {
            return id64;
        }

        public String getSubtype() {
            return subtype;
        }

        public String getType() {
            return type;
        }
    }

    public static class Ring {

        @SerializedName("inner_radius")
        private double innerRadius;

        @SerializedName("mass")
        private double mass;

        @SerializedName("name")
        private String name;

        @SerializedName("outer_radius")
        private double outerRadius;

        @SerializedName("signal_count")
        private Integer signalCount;

        @SerializedName("signals")
        private List<Signal> signals;

        @SerializedName("signals_updated_at")
        private String signalsUpdatedAt;

        @SerializedName("type")
        private String type;

        public double getInnerRadius() {
            return innerRadius;
        }

        public double getMass() {
            return mass;
        }

        public String getName() {
            return name;
        }

        public double getOuterRadius() {
            return outerRadius;
        }

        public Integer getSignalCount() {
            return signalCount;
        }

        public List<Signal> getSignals() {
            return signals;
        }

        public String getSignalsUpdatedAt() {
            return signalsUpdatedAt;
        }

        public String getType() {
            return type;
        }
    }

    public static class Signal {

        @SerializedName("count")
        private int count;

        @SerializedName("name")
        private String name;

        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }
    }

    public static class Search {

        @SerializedName("filters")
        private Filters filters;

        @SerializedName("page")
        private int page;

        @SerializedName("size")
        private int size;

        @SerializedName("sort")
        private List<Object> sort;

        public Filters getFilters() {
            return filters;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public List<Object> getSort() {
            return sort;
        }
    }

    public static class Filters {

        @SerializedName("reserve_level")
        private ReserveLevel reserveLevel;

        @SerializedName("ring_signals")
        private List<RingSignal> ringSignals;

        public ReserveLevel getReserveLevel() {
            return reserveLevel;
        }

        public List<RingSignal> getRingSignals() {
            return ringSignals;
        }
    }

    public static class ReserveLevel {

        @SerializedName("value")
        private List<String> value;

        public List<String> getValue() {
            return value;
        }
    }

    public static class RingSignal {

        @SerializedName("comparison")
        private String comparison;

        @SerializedName("count")
        private List<Integer> count;

        @SerializedName("name")
        private String name;

        public String getComparison() {
            return comparison;
        }

        public List<Integer> getCount() {
            return count;
        }

        public String getName() {
            return name;
        }
    }
}