package elite.intel.gameapi.search.edsm.dto.data;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;
import java.util.Map;

public class BodyData extends BaseJsonDto implements ToJsonConvertible {
    @SerializedName("id")
    public int id;
    @SerializedName("id64")
    public long id64;
    @SerializedName("bodyId")
    public long bodyId;
    @SerializedName("name")
    public String name;
    @SerializedName("discovery")
    public DiscoveryData discovery;
    @SerializedName("type")
    public String type;
    @SerializedName("subType")
    public String subType;
    @SerializedName("parents")
    public List<ParentBody> parents;
    @SerializedName("distanceToArrival")
    public double distanceToArrival;
    @SerializedName("isMainStar")
    public boolean isMainStar;
    @SerializedName("isScoopable")
    public boolean isScoopable;
    @SerializedName("age")
    public int age;
    @SerializedName("spectralClass")
    public String spectralClass;
    @SerializedName("luminosity")
    public String luminosity;
    @SerializedName("absoluteMagnitude")
    public double absoluteMagnitude;
    @SerializedName("solarMasses")
    public double solarMasses;
    @SerializedName("solarRadius")
    public double solarRadius;
    @SerializedName("surfaceTemperature")
    public double surfaceTemperature;
    @SerializedName("orbitalPeriod")
    public double orbitalPeriod;
    @SerializedName("semiMajorAxis")
    public double semiMajorAxis;
    @SerializedName("orbitalEccentricity")
    public double orbitalEccentricity;
    @SerializedName("orbitalInclination")
    public double orbitalInclination;
    @SerializedName("argOfPeriapsis")
    public double argOfPeriapsis;
    @SerializedName("rotationalPeriod")
    public double rotationalPeriod;
    @SerializedName("rotationalPeriodTidallyLocked")
    public boolean rotationalPeriodTidallyLocked;
    @SerializedName("axialTilt")
    public double axialTilt;
    @SerializedName("belts")
    public List<BeltData> belts;
    @SerializedName("rings")
    public List<RingData> rings;
    @SerializedName("updateTime")
    public String updateTime;
    @SerializedName("materials")
    public Map<String, Double> materials;
    @SerializedName("isLandable")
    public boolean isLandable;
    @SerializedName("gravity")
    public double gravity;
    @SerializedName("earthMasses")
    public double earthMasses;
    @SerializedName("radius")
    public double radius;
    @SerializedName("surfacePressure")
    public double surfacePressure;
    @SerializedName("volcanismType")
    public String volcanismType;
    @SerializedName("atmosphereType")
    public String atmosphereType;
    @SerializedName("atmosphereComposition")
    public Map<String, Double> atmosphereComposition;
    @SerializedName("solidComposition")
    public Map<String, Double> solidComposition;
    @SerializedName("terraformingState")
    public String terraformingState;

    public int getId() {
        return id;
    }

    public long getId64() {
        return id64;
    }

    public long getBodyId() {
        return bodyId;
    }

    public String getName() {
        return name;
    }

    public DiscoveryData getDiscovery() {
        return discovery;
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }

    public List<ParentBody> getParents() {
        return parents;
    }

    public double getDistanceToArrival() {
        return distanceToArrival;
    }

    public boolean isMainStar() {
        return isMainStar;
    }

    public boolean isScoopable() {
        return isScoopable;
    }

    public int getAge() {
        return age;
    }

    public String getSpectralClass() {
        return spectralClass;
    }

    public String getLuminosity() {
        return luminosity;
    }

    public double getAbsoluteMagnitude() {
        return absoluteMagnitude;
    }

    public double getSolarMasses() {
        return solarMasses;
    }

    public double getSolarRadius() {
        return solarRadius;
    }

    public double getSurfaceTemperature() {
        return surfaceTemperature;
    }

    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    public double getSemiMajorAxis() {
        return semiMajorAxis;
    }

    public double getOrbitalEccentricity() {
        return orbitalEccentricity;
    }

    public double getOrbitalInclination() {
        return orbitalInclination;
    }

    public double getArgOfPeriapsis() {
        return argOfPeriapsis;
    }

    public double getRotationalPeriod() {
        return rotationalPeriod;
    }

    public boolean isRotationalPeriodTidallyLocked() {
        return rotationalPeriodTidallyLocked;
    }

    public double getAxialTilt() {
        return axialTilt;
    }

    public List<BeltData> getBelts() {
        return belts;
    }

    public List<RingData> getRings() {
        return rings;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public Map<String, Double> getMaterials() {
        return materials;
    }

    public boolean isLandable() {
        return isLandable;
    }

    public double getGravity() {
        return gravity;
    }

    public double getEarthMasses() {
        return earthMasses;
    }

    /**
     * EDSM's raw radius, in KILOMETRES. The journal's {@code Radius} is in metres, so the two are a factor of 1000
     * apart: use {@link #getRadiusMeters()} for anything that stores a radius or computes gravity from one.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * EDSM's radius converted to metres, the unit every LocationDto radius and gravity calculation uses.
     */
    public double getRadiusMeters() {
        return radius * 1000.0;
    }

    public double getSurfacePressure() {
        return surfacePressure;
    }

    public String getVolcanismType() {
        return volcanismType;
    }

    public String getAtmosphereType() {
        return atmosphereType;
    }

    public Map<String, Double> getAtmosphereComposition() {
        return atmosphereComposition;
    }

    public Map<String, Double> getSolidComposition() {
        return solidComposition;
    }

    public String getTerraformingState() {
        return terraformingState;
    }
}
