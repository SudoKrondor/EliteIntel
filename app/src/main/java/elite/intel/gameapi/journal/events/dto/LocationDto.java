package elite.intel.gameapi.journal.events.dto;

import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.SAASignalsFoundEvent;
import elite.intel.search.edsm.dto.*;
import elite.intel.search.edsm.dto.data.BodyData;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import java.util.*;

import static elite.intel.util.StringUtls.subtractString;

public class LocationDto implements ToJsonConvertible {

    private List<MaterialDto> materials = new ArrayList<>();
    private List<SAASignalsFoundEvent.Signal> saaSignals = new ArrayList<>();
    private List<FSSBodySignalsEvent.Signal> fssSignals;
    private List<GenusDto> genus = new ArrayList<>();
    private List<BioSampleDto> partialBioSamples = new ArrayList<>();
    private List<String> stationServices;
    private List<String> powers;
    private Set<FssSignalDto> detectedSignals = new HashSet<>();
    private double X;
    private double Y;
    private double Z;
    private double distance;
    private double rotationPeriod;
    private double axialTilt;
    private double orbitalPeriod;
    private String starName;
    private String starClass;
    private boolean isFuelStar;
    private String planetName = "";
    private String planetShortName = "";
    private BodyData planetData;
    private TrafficDto trafficDto;
    private DeathsDto deathsDto;
    private String stationName;
    private String stationType;
    private long marketID;
    private long systemAddress;
    private String stationFaction;
    private String stationGovernment;
    private String stationAllegiance;
    private String stationEconomy;
    private String allegiance;
    private String economy;
    private String secondEconomy;
    private String government;
    private String security;
    private long population;
    private String bodyType;
    private String controllingPower;
    private String powerplayState;
    private double powerplayStateControlProgress;
    private Integer powerplayStateReinforcement;
    private Integer powerplayStateUndermining;
    private double gravity;
    private double surfaceTemperature;
    private double[] landingCoordinates;
    private double orbitalCruiseEntryAltitude;
    private boolean ourDiscovery = false;
    private boolean weMappedIt = false;
    private String volcanism;
    private boolean isHomeSystem;
    private String discoveredBy;
    private String discoveredOn;

    //private BioStatus bioStatus = BioStatus.SCAN_REQUIRED;
    private MarketDto market;
    private OutfittingDto outfitting;
    private ShipyardDto shipyard;
    private LocationType locationType = LocationType.UNCLASSIFIED;
    private long bodyId;
    private boolean isLandable;
    private String planetClass;
    private boolean isTerraformable;
    private boolean isTidalLocked;
    private String atmosphere;
    private double radius;
    private double massEM;
    private int bioSignals;
    private int geoSignals;
    private boolean hasRings;
    private String parentBodyName;
    private Long parentBodyId; // in-game bodyID of parent body (set for moons: the parent planet's bodyID)

    public LocationDto(long bodiId) {
        setBodyId(bodiId);
    }

    public LocationDto(Long bodyId, Long systemAddress) {
        setBodyId(bodyId == null ? -1L : bodyId);
        setSystemAddress(systemAddress == null ? -1L : systemAddress);
    }


    public LocationDto(long id, String starName) {
        setBodyId(id);
        setStarName(starName);
    }

    private LocationDto() {
        // serialization
    }

    public LocationDto(LocationType locationType) {
        setLocationType(locationType);
    }

    public static LocationType determineType(String data, boolean isPrimaryStar) {
        String type = data.toLowerCase(Locale.ROOT);
        if (type.contains("star") && isPrimaryStar) return LocationType.PRIMARY_STAR;
        if (type.contains("star") && !isPrimaryStar) return LocationType.STAR;
        if (type.contains("body")) return LocationType.PLANET;
        if (type.contains("giant")) return LocationType.PLANET;
        if (type.contains("world")) return LocationType.PLANET;
        if (type.contains("rogueplanet")) return LocationType.PLANET;
        if (type.contains("black hole")) return LocationType.BLACK_HOLE;
        if (type.contains("nebula")) return LocationType.NEBULA;
        if (type.contains("station")) return LocationType.STATION;
        if (type.contains("fleetcarrier")) return LocationType.FLEET_CARRIER;
        if (type.contains("planetaryring")) return LocationType.PLANETARY_RING;
        return null;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public void setSystemAddress(@NotBlank long systemAddress) {
        this.systemAddress = systemAddress;
    }

    public void setMarket(MarketDto marketDto) {
        if (this.market != null && marketDto == null) return;
        this.market = marketDto;
    }

    public MarketDto getMarket() {
        return market;
    }

    public void setOutfitting(OutfittingDto outfittingDto) {
        if (this.outfitting != null && outfittingDto == null) return;
        this.outfitting = outfittingDto;
    }

    public OutfittingDto getOutfitting() {
        return outfitting;
    }

    public void setShipyard(ShipyardDto shipyardDto) {
        if (this.shipyard != null && shipyardDto == null) return;
        this.shipyard = shipyardDto;
    }

    public ShipyardDto getShipyard() {
        return shipyard;
    }

    public void deletePartialBioSamples() {
        partialBioSamples.clear();
    }

    public double getAxialTilt() {
        return axialTilt;
    }

    public void setAxialTilt(double axialTilt) {
        if (this.axialTilt > 0) return;
        this.axialTilt = axialTilt;
    }

    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    public void setOrbitalPeriod(double orbitalPeriod) {
        if (this.orbitalPeriod > 0) return;
        this.orbitalPeriod = orbitalPeriod;
    }

    public double getX() {
        return X;
    }

    public void setX(double x) {
        if (x == 0) return;
        X = x;
    }

    public double getY() {
        return Y;
    }

    public void setY(double y) {
        if (y == 0) return;
        Y = y;
    }

    public double getZ() {
        return Z;
    }

    public void setZ(double z) {
        if (z == 0) return;
        Z = z;
    }

    public double getRotationPeriod() {
        return rotationPeriod;
    }

    public void setRotationPeriod(double rotationPeriod) {
        if (this.rotationPeriod > 0) return;
        this.rotationPeriod = rotationPeriod;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        if (this.distance > 0) return;
        this.distance = distance;
    }

    public String getStarName() {
        return starName;
    }

    public void setStarName(String starName) {
        if (starName == null) return;
        this.starName = starName;
    }

    public String getPlanetName() {
        return planetName;
    }

    public void setPlanetName(String planetName) {
        if (planetName == null) return;
        this.planetName = planetName;
        if (starName != null && planetShortName == null) {
            this.planetShortName = StringUtls.subtractString(planetName, starName);
            if (this.planetShortName.toLowerCase(Locale.ROOT).contains("ring")) {
                setLocationType(LocationType.PLANETARY_RING);
            }
        }
    }

    public BodyData getPlanetData() {
        return planetData;
    }

    public void setPlanetData(BodyData planetData) {
        if (this.planetData != null) return;
        this.planetData = planetData;
    }

    public void addSaaSignals(List<SAASignalsFoundEvent.Signal> signals) {
        if (signals == null || signals.isEmpty()) return;
        if (this.saaSignals == null) {
            signals = new ArrayList<>();
        }
        if (this.saaSignals != null) {
            this.saaSignals.addAll(signals);
        } else {
            this.saaSignals = signals;
        }
    }

    public List<GenusDto> getGenus() {
        return genus;
    }

    public void setGenus(List<GenusDto> genus) {
        if (genus.isEmpty()) return;
        this.genus = genus;
    }

    public List<BioSampleDto> getPartialBioSamples() {
        return partialBioSamples;
    }

    public void addDetectedSignal(FssSignalDto signal) {
        if (signal == null) return;
        this.detectedSignals.add(signal);
    }

    public void addDetectedSignals(List<FssSignalDto> signals) {
        if (signals == null || signals.isEmpty()) return;
        this.detectedSignals.addAll(signals);
    }

    public Set<FssSignalDto> getDetectedSignals() {
        return detectedSignals;
    }

    public void addBioScan(BioSampleDto bioSampleDto) {
        if (bioSampleDto == null) return;
        this.partialBioSamples.add(bioSampleDto);
    }

    public DeathsDto getDeathsDto() {
        return deathsDto;
    }

    public void setDeathsDto(DeathsDto deathsDto) {
        if (deathsDto == null) return;
        this.deathsDto = deathsDto;
    }

    public TrafficDto getTrafficDto() {
        return trafficDto;
    }

    public void setTrafficDto(TrafficDto trafficDto) {
        if (trafficDto == null) return;
        this.trafficDto = trafficDto;
    }

    public void setPartialBioSamples(List<BioSampleDto> partialBioSamples) {
        if (partialBioSamples == null || partialBioSamples.isEmpty()) return;
        this.partialBioSamples = partialBioSamples;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationType() {
        return stationType == null ? "FleetCarrier" : stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public long getMarketID() {
        return marketID;
    }

    public void setMarketID(long marketID) {
        this.marketID = marketID;
    }

    public String getStationFaction() {
        return stationFaction;
    }

    public void setStationFaction(String stationFaction) {
        this.stationFaction = stationFaction;
    }

    public String getStationGovernment() {
        return stationGovernment;
    }

    public void setStationGovernment(String stationGovernment) {
        this.stationGovernment = stationGovernment;
    }

    public String getStationAllegiance() {
        return stationAllegiance;
    }

    public void setStationAllegiance(String stationAllegiance) {
        this.stationAllegiance = stationAllegiance;
    }

    public List<String> getStationServices() {
        return stationServices;
    }

    public void setStationServices(List<String> stationServices) {
        if (stationServices == null) return;
        this.stationServices = stationServices;
    }

    public String getStationEconomy() {
        return stationEconomy;
    }

    public void setStationEconomy(String stationEconomy) {
        this.stationEconomy = stationEconomy;
    }

    public String getAllegiance() {
        return allegiance;
    }

    public void setAllegiance(String allegiance) {
        if (allegiance == null) return;
        this.allegiance = allegiance;
    }

    public String getEconomy() {
        return economy;
    }

    public void setEconomy(String economy) {
        if (economy == null) return;
        this.economy = economy;
    }

    public String getSecondEconomy() {
        return secondEconomy;
    }

    public void setSecondEconomy(String secondEconomy) {
        if (secondEconomy == null) return;
        this.secondEconomy = secondEconomy;
    }

    public String getGovernment() {
        return government;
    }

    public void setGovernment(String government) {
        if (government == null) return;
        this.government = government;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        if (security == null) return;
        this.security = security;
    }

    public long getPopulation() {
        return population;
    }

    public void setPopulation(long population) {
        if (population == 0) return;
        this.population = population;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        if (bodyType == null || bodyType.isEmpty()) return;
        this.bodyType = bodyType;
    }

    public String getControllingPower() {
        return controllingPower;
    }

    public void setControllingPower(String controllingPower) {
        if (controllingPower == null || controllingPower.isEmpty()) return;
        this.controllingPower = controllingPower;
    }

    public List<String> getPowers() {
        return powers;
    }

    public void setPowers(List<String> powers) {
        if (powers == null || powers.isEmpty()) return;
        this.powers = powers;
    }

    public String getPowerplayState() {
        return powerplayState;
    }

    public void setPowerplayState(String powerplayState) {
        if (powerplayState == null) return;
        this.powerplayState = powerplayState;
    }

    public double getPowerplayStateControlProgress() {
        return powerplayStateControlProgress;
    }

    public void setPowerplayStateControlProgress(double powerplayStateControlProgress) {
        if (powerplayStateControlProgress == 0) return;
        this.powerplayStateControlProgress = powerplayStateControlProgress;
    }

    public Integer getPowerplayStateReinforcement() {
        return powerplayStateReinforcement;
    }

    public void setPowerplayStateReinforcement(Integer powerplayStateReinforcement) {
        if (powerplayStateReinforcement == null || powerplayStateReinforcement == 0) return;
        this.powerplayStateReinforcement = powerplayStateReinforcement;
    }

    public Integer getPowerplayStateUndermining() {
        return powerplayStateUndermining;
    }

    public void setPowerplayStateUndermining(Integer powerplayStateUndermining) {
        if (powerplayStateUndermining == null || powerplayStateUndermining == 0) return;
        this.powerplayStateUndermining = powerplayStateUndermining;
    }

    public double getGravity() {
        return gravity;
    }

    public void setGravity(double gravity) {
        if (gravity == 0) return;
        this.gravity = gravity;
    }

    public double getSurfaceTemperature() {
        return surfaceTemperature;
    }

    public void setSurfaceTemperature(double surfaceTemperature) {
        if (surfaceTemperature == 0) return;
        this.surfaceTemperature = surfaceTemperature;
    }

    public void addMaterial(MaterialDto materialDto) {
        if (materialDto == null) return;
        this.materials.add(materialDto);
    }

    public List<MaterialDto> getMaterials() {
        return materials;
    }

    public String getPlanetShortName() {
        if (starName != null && planetShortName == null) {
            return StringUtls.subtractString(planetName, starName);
        } else {
            return planetShortName;
        }
    }

    public void setPlanetShortName(String planetShortName) {
        if (planetShortName == null) return;
        this.planetShortName = subtractString(planetShortName, getStarName());
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    public double[] getLandingCoordinates() {
        return landingCoordinates != null ? landingCoordinates : new double[0];
    }

    public void setLandingCoordinates(double[] landingCoordinates) {
        if (landingCoordinates == null || landingCoordinates.length == 0) return;
        this.landingCoordinates = landingCoordinates;
    }

    public double getOrbitalCruiseEntryAltitude() {
        return orbitalCruiseEntryAltitude;
    }

    public void setOrbitalCruiseEntryAltitude(double orbitalCruiseEntryAltitude) {
        if (orbitalCruiseEntryAltitude == 0) return;
        this.orbitalCruiseEntryAltitude = orbitalCruiseEntryAltitude;
    }

    public boolean isOurDiscovery() {
        return ourDiscovery;
    }

    public void setOurDiscovery(boolean ourDiscovery) {
        if (this.ourDiscovery == true) return;
        this.ourDiscovery = ourDiscovery;
    }

    public void setMaterials(List<MaterialDto> materials) {
        if (materials == null || materials.isEmpty()) return;
        this.materials = materials;
    }

    public List<SAASignalsFoundEvent.Signal> getSaaSignals() {
        return saaSignals;
    }

    public void setSaaSignals(List<SAASignalsFoundEvent.Signal> saaSignals) {
        if (saaSignals == null || saaSignals.isEmpty()) return;
        this.saaSignals = saaSignals;
    }

    public List<FSSBodySignalsEvent.Signal> getFssSignals() {
        return fssSignals;
    }

    public void setFssSignals(List<FSSBodySignalsEvent.Signal> fssSignals) {
        if (fssSignals == null || fssSignals.isEmpty()) return;
        this.fssSignals = fssSignals;
    }

    public void setDetectedSignals(Set<FssSignalDto> detectedSignals) {
        if (detectedSignals == null || detectedSignals.isEmpty()) return;
        this.detectedSignals = detectedSignals;
    }

    public long getBodyId() {
        return bodyId;
    }

    public void setBodyId(long bodyId) {
        if (this.bodyId > bodyId) return;
        this.bodyId = bodyId;
    }

    public boolean isLandable() {
        return isLandable;
    }

    public void setLandable(boolean landable) {
        if (this.isLandable) return;
        this.isLandable = landable;
    }

    public String getPlanetClass() {
        return planetClass;
    }

    public void setPlanetClass(String planetClass) {
        if (this.planetClass != null) return;
        this.planetClass = planetClass;
    }

    public boolean isTerraformable() {
        return isTerraformable;
    }

    public void setTerraformable(boolean terraformable) {
        if (this.isTerraformable) return;
        this.isTerraformable = terraformable;
    }

    public boolean isTidalLocked() {
        return isTidalLocked;
    }

    public void setTidalLocked(boolean tidalLocked) {
        if (this.isTidalLocked) return;
        this.isTidalLocked = tidalLocked;
    }

    public String getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(String atmosphere) {
        if (atmosphere == null) return;
        this.atmosphere = atmosphere;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        if (radius == 0) return;
        this.radius = radius;
    }

    public double getMassEM() {
        return massEM;
    }

    public void setMassEM(double massEM) {
        if (massEM == 0) return;
        this.massEM = massEM;
    }

    public int getBioSignals() {
        return bioSignals;
    }

    public void setBioSignals(int bioSignals) {
        if (bioSignals > 0) { // do not override to 0 on bad data.
            this.bioSignals = bioSignals;
        }
    }

    public int getGeoSignals() {
        return geoSignals;
    }

    public void setGeoSignals(int geoSignals) {
        if (geoSignals > 0) return; //do not overridde
        this.geoSignals = geoSignals;
    }

    public boolean isHasRings() {
        return hasRings;
    }

    public void setHasRings(boolean hasRings) {
        if (this.hasRings) return;
        this.hasRings = hasRings;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public boolean isWeMappedIt() {
        return weMappedIt;
    }

    public void setWeMappedIt(boolean weMappedIt) {
        if (this.weMappedIt) return;
        this.weMappedIt = weMappedIt;
    }

    public String getStarClass() {
        return starClass;
    }

    public void setStarClass(String starClass) {
        if (this.starClass != null) return;
        this.starClass = starClass;
        if (starClass != null && "KGBFOAM".contains(starClass)) {
            setFuelStar(true);
        }
    }

    public boolean isFuelStar() {
        return isFuelStar;
    }

    public void setFuelStar(boolean fuelStar) {
        if (this.isFuelStar) return;
        isFuelStar = fuelStar;
    }

    public String getVolcanism() {
        return volcanism;
    }

    public void setVolcanism(String volcanism) {
        if (this.volcanism != null) return;
        this.volcanism = volcanism;
    }

    public boolean isHomeSystem() {
        return isHomeSystem;
    }

    public void setHomeSystem(boolean homeSystem) {
        if (this.isHomeSystem) return;
        isHomeSystem = homeSystem;
    }

    public String getDiscoveredBy() {
        return discoveredBy;
    }

    public void setDiscoveredBy(String discoveredBy) {
        if (this.discoveredBy != null) return;
        this.discoveredBy = discoveredBy;
    }

    public String getDiscoveredOn() {
        return discoveredOn;
    }

    public void setDiscoveredOn(String discoveredOn) {
        if (this.discoveredOn != null && !this.discoveredOn.isEmpty()) return;
        this.discoveredOn = discoveredOn;
    }

    @Override public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        LocationDto that = (LocationDto) o;
        return Double.compare(getX(), that.getX()) == 0 && Double.compare(getY(), that.getY()) == 0 && Double.compare(getZ(), that.getZ()) == 0 && Double.compare(getDistance(), that.getDistance()) == 0 && Double.compare(getRotationPeriod(), that.getRotationPeriod()) == 0 && Double.compare(getAxialTilt(), that.getAxialTilt()) == 0 && Double.compare(getOrbitalPeriod(), that.getOrbitalPeriod()) == 0 && isFuelStar() == that.isFuelStar() && Double.compare(getPowerplayStateControlProgress(), that.getPowerplayStateControlProgress()) == 0 && Double.compare(getGravity(), that.getGravity()) == 0 && Double.compare(getSurfaceTemperature(), that.getSurfaceTemperature()) == 0 && Double.compare(getOrbitalCruiseEntryAltitude(), that.getOrbitalCruiseEntryAltitude()) == 0 && isOurDiscovery() == that.isOurDiscovery() && isWeMappedIt() == that.isWeMappedIt() && isHomeSystem() == that.isHomeSystem() && isLandable() == that.isLandable() && isTerraformable() == that.isTerraformable() && isTidalLocked() == that.isTidalLocked() && Double.compare(getRadius(), that.getRadius()) == 0 && Double.compare(getMassEM(), that.getMassEM()) == 0 && getBioSignals() == that.getBioSignals() && getGeoSignals() == that.getGeoSignals() && isHasRings() == that.isHasRings() && Objects.equals(getMaterials(), that.getMaterials()) && Objects.equals(getSaaSignals(), that.getSaaSignals()) && Objects.equals(getFssSignals(), that.getFssSignals()) && Objects.equals(getGenus(), that.getGenus()) && Objects.equals(getPartialBioSamples(), that.getPartialBioSamples()) && Objects.equals(getStationServices(), that.getStationServices()) && Objects.equals(getPowers(), that.getPowers()) && Objects.equals(getDetectedSignals(), that.getDetectedSignals()) && Objects.equals(getStarName(), that.getStarName()) && Objects.equals(getStarClass(), that.getStarClass()) && Objects.equals(getPlanetName(), that.getPlanetName()) && Objects.equals(getPlanetShortName(), that.getPlanetShortName()) && Objects.equals(getPlanetData(), that.getPlanetData()) && Objects.equals(getTrafficDto(), that.getTrafficDto()) && Objects.equals(getDeathsDto(), that.getDeathsDto()) && Objects.equals(getStationName(), that.getStationName()) && Objects.equals(getStationType(), that.getStationType()) && Objects.equals(getMarketID(), that.getMarketID()) && Objects.equals(getSystemAddress(), that.getSystemAddress()) && Objects.equals(getStationFaction(), that.getStationFaction()) && Objects.equals(getStationGovernment(), that.getStationGovernment()) && Objects.equals(getStationAllegiance(), that.getStationAllegiance()) && Objects.equals(getStationEconomy(), that.getStationEconomy()) && Objects.equals(getAllegiance(), that.getAllegiance()) && Objects.equals(getEconomy(), that.getEconomy()) && Objects.equals(getSecondEconomy(), that.getSecondEconomy()) && Objects.equals(getGovernment(), that.getGovernment()) && Objects.equals(getSecurity(), that.getSecurity()) && Objects.equals(getPopulation(), that.getPopulation()) && Objects.equals(getBodyType(), that.getBodyType()) && Objects.equals(getControllingPower(), that.getControllingPower()) && Objects.equals(getPowerplayState(), that.getPowerplayState()) && Objects.equals(getPowerplayStateReinforcement(), that.getPowerplayStateReinforcement()) && Objects.equals(getPowerplayStateUndermining(), that.getPowerplayStateUndermining()) && Arrays.equals(getLandingCoordinates(), that.getLandingCoordinates()) && Objects.equals(getVolcanism(), that.getVolcanism()) && Objects.equals(getDiscoveredBy(), that.getDiscoveredBy()) && Objects.equals(getDiscoveredOn(), that.getDiscoveredOn()) && Objects.equals(getMarket(), that.getMarket()) && Objects.equals(getOutfitting(), that.getOutfitting()) && Objects.equals(getShipyard(), that.getShipyard()) && getLocationType() == that.getLocationType() && Objects.equals(getBodyId(), that.getBodyId()) && Objects.equals(getPlanetClass(), that.getPlanetClass()) && Objects.equals(getAtmosphere(), that.getAtmosphere()) && Objects.equals(getParentBodyName(), that.getParentBodyName());
    }

    @Override public int hashCode() {
        int result = Objects.hashCode(getMaterials());
        result = 31 * result + Objects.hashCode(getSaaSignals());
        result = 31 * result + Objects.hashCode(getFssSignals());
        result = 31 * result + Objects.hashCode(getGenus());
        result = 31 * result + Objects.hashCode(getPartialBioSamples());
        result = 31 * result + Objects.hashCode(getStationServices());
        result = 31 * result + Objects.hashCode(getPowers());
        result = 31 * result + Objects.hashCode(getDetectedSignals());
        result = 31 * result + Double.hashCode(getX());
        result = 31 * result + Double.hashCode(getY());
        result = 31 * result + Double.hashCode(getZ());
        result = 31 * result + Double.hashCode(getDistance());
        result = 31 * result + Double.hashCode(getRotationPeriod());
        result = 31 * result + Double.hashCode(getAxialTilt());
        result = 31 * result + Double.hashCode(getOrbitalPeriod());
        result = 31 * result + Objects.hashCode(getStarName());
        result = 31 * result + Objects.hashCode(getStarClass());
        result = 31 * result + Boolean.hashCode(isFuelStar());
        result = 31 * result + Objects.hashCode(getPlanetName());
        result = 31 * result + Objects.hashCode(getPlanetShortName());
        result = 31 * result + Objects.hashCode(getPlanetData());
        result = 31 * result + Objects.hashCode(getTrafficDto());
        result = 31 * result + Objects.hashCode(getDeathsDto());
        result = 31 * result + Objects.hashCode(getStationName());
        result = 31 * result + Objects.hashCode(getStationType());
        result = 31 * result + Objects.hashCode(getMarketID());
        result = 31 * result + Objects.hashCode(getSystemAddress());
        result = 31 * result + Objects.hashCode(getStationFaction());
        result = 31 * result + Objects.hashCode(getStationGovernment());
        result = 31 * result + Objects.hashCode(getStationAllegiance());
        result = 31 * result + Objects.hashCode(getStationEconomy());
        result = 31 * result + Objects.hashCode(getAllegiance());
        result = 31 * result + Objects.hashCode(getEconomy());
        result = 31 * result + Objects.hashCode(getSecondEconomy());
        result = 31 * result + Objects.hashCode(getGovernment());
        result = 31 * result + Objects.hashCode(getSecurity());
        result = 31 * result + Objects.hashCode(getPopulation());
        result = 31 * result + Objects.hashCode(getBodyType());
        result = 31 * result + Objects.hashCode(getControllingPower());
        result = 31 * result + Objects.hashCode(getPowerplayState());
        result = 31 * result + Double.hashCode(getPowerplayStateControlProgress());
        result = 31 * result + Objects.hashCode(getPowerplayStateReinforcement());
        result = 31 * result + Objects.hashCode(getPowerplayStateUndermining());
        result = 31 * result + Double.hashCode(getGravity());
        result = 31 * result + Double.hashCode(getSurfaceTemperature());
        result = 31 * result + Arrays.hashCode(getLandingCoordinates());
        result = 31 * result + Double.hashCode(getOrbitalCruiseEntryAltitude());
        result = 31 * result + Boolean.hashCode(isOurDiscovery());
        result = 31 * result + Boolean.hashCode(isWeMappedIt());
        result = 31 * result + Objects.hashCode(getVolcanism());
        result = 31 * result + Boolean.hashCode(isHomeSystem());
        result = 31 * result + Objects.hashCode(getDiscoveredBy());
        result = 31 * result + Objects.hashCode(getDiscoveredOn());
        result = 31 * result + Objects.hashCode(getMarket());
        result = 31 * result + Objects.hashCode(getOutfitting());
        result = 31 * result + Objects.hashCode(getShipyard());
        result = 31 * result + Objects.hashCode(getLocationType());
        result = 31 * result + Objects.hashCode(getBodyId());
        result = 31 * result + Boolean.hashCode(isLandable());
        result = 31 * result + Objects.hashCode(getPlanetClass());
        result = 31 * result + Boolean.hashCode(isTerraformable());
        result = 31 * result + Boolean.hashCode(isTidalLocked());
        result = 31 * result + Objects.hashCode(getAtmosphere());
        result = 31 * result + Double.hashCode(getRadius());
        result = 31 * result + Double.hashCode(getMassEM());
        result = 31 * result + getBioSignals();
        result = 31 * result + getGeoSignals();
        result = 31 * result + Boolean.hashCode(isHasRings());
        result = 31 * result + Objects.hashCode(getParentBodyName());
        return result;
    }

    public void setParentBodyName(String parentBodyName) {
        this.parentBodyName = parentBodyName;
    }

    public String getParentBodyName() {
        return parentBodyName;
    }

    public Long getParentBodyId() {
        return parentBodyId;
    }

    public void setParentBodyId(Long parentBodyId) {
        if (this.parentBodyId != null && this.parentBodyId > 0) return;
        this.parentBodyId = parentBodyId;
    }


    public enum BioStatus {
        BIO_FORMS_PRESENT,
        NO_BIO_FORMS,
        SCAN_REQUIRED;
    }


    public enum LocationType {
        STAR,
        PRIMARY_STAR,
        PLANET,
        MOON,
        PLANETARY_RING,
        BLACK_HOLE,
        NEBULA,
        STATION,
        FLEET_CARRIER,
        FACILITY,
        MARKET,
        BELT_CLUSTER,
        UNCLASSIFIED
    }
}
