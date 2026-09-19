package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.gameapi.search.edsm.EdsmApiClient;
import elite.intel.gameapi.search.edsm.dto.SystemBodiesDto;
import elite.intel.gameapi.search.edsm.dto.data.BodyData;
import elite.intel.gameapi.search.edsm.dto.data.ParentBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static elite.intel.util.GravityCalculator.calculateSurfaceGravity;

/**
 * Files EDSM's body list for a system into the location table, one record per body.
 *
 * <p>Shared by the two ways a session comes to be in a system: a jump arrival and a startup that finds the
 * commander already there. It used to belong to the jump alone, so a session that began docked in a system
 * had no body records until the commander's own scans wrote them - and the FSS does not repeat a Scan for a
 * body resolved on an earlier visit, so those never came.
 */
public final class EdsmSystemBodies {

    private EdsmSystemBodies() {
    }

    /**
     * Fetches the system's bodies from EDSM and records them. A system EDSM does not know leaves the table as it was.
     */
    public static void fetchAndRecord(String starSystem, long systemAddress, double[] starPos) {
        record(EdsmApiClient.searchSystemBodies(starSystem), systemAddress, starPos, starSystem);
    }

    /**
     * Whether the system's records say nothing yet about any planet or moon in it, so a fetch would tell
     * us something. Stars and stations do not count: the arrival writes those itself.
     */
    public static boolean hasNoClassifiedBodies(Collection<LocationDto> systemRecords) {
        for (LocationDto record : systemRecords) {
            LocationDto.LocationType type = record.getLocationType();
            if (type == LocationDto.LocationType.PLANET || type == LocationDto.LocationType.MOON) return false;
        }
        return true;
    }

    static void record(SystemBodiesDto systemBodiesDto, long systemAddress, double[] starPos, String starSystem) {
        if (systemBodiesDto == null) return;
        if (systemBodiesDto.getData() == null) return;
        List<BodyData> bodies = systemBodiesDto.getData().getBodies();
        if (bodies == null || bodies.isEmpty()) return;

        LocationManager locationManager = LocationManager.getInstance();
        for (BodyData data : bodies) {
            LocationDto stellarObject = locationManager.findBySystemAddress(systemAddress, data.getBodyId());
            stellarObject.setSystemAddress(systemAddress);
            stellarObject.setAtmosphere(data.getAtmosphereType());
            stellarObject.setBodyId(data.getBodyId());
            stellarObject.setHasRings(data.getRings() != null && !data.getRings().isEmpty());
            stellarObject.setTerraformable("Terraformable".equalsIgnoreCase(data.getTerraformingState()));
            stellarObject.setLandable(data.isLandable());
            stellarObject.setMaterials(toMaterials(data.getMaterials()));
            stellarObject.setPlanetName(data.getName());
            stellarObject.setMassEM(data.getEarthMasses());
            double radiusMeters = data.getRadiusMeters(); // EDSM reports km, the journal and LocationDto use metres
            stellarObject.setRadius(radiusMeters);
            Double surfaceGravity = calculateSurfaceGravity(data.getEarthMasses(), radiusMeters);
            stellarObject.setGravity(surfaceGravity == null ? 0 : surfaceGravity);
            stellarObject.setSurfaceTemperature(data.getSurfaceTemperature()); // Keep Kelvin
            stellarObject.setTidalLocked(data.isRotationalPeriodTidallyLocked());
            LocationDto.LocationType bodyType = classifyEdsmBody(data);
            if (bodyType != null) stellarObject.setLocationType(bodyType);
            if (starPos != null) {
                stellarObject.setX(starPos[0]);
                stellarObject.setY(starPos[1]);
                stellarObject.setZ(starPos[2]);
            }
            if (data.getDiscovery() != null) {
                // EDSM can only contradict a discovery claim, never make one. It holds what commanders
                // chose to upload, so "no discoverer recorded" covers most of the galaxy and reading it
                // as "nobody got here first" flagged ordinary charted bodies as ours - which then paid a
                // first-discovery bonus into every exobiology projection for them. A named commander is
                // real evidence; silence is not, and the journal's own WasDiscovered settles those.
                if (data.getDiscovery().getCommander() != null) {
                    stellarObject.setOurDiscovery(false);
                }
                stellarObject.setDiscoveredBy(data.getDiscovery().getCommander());
                stellarObject.setDiscoveredOn(data.getDiscovery().getDate());
            }
            stellarObject.setOrbitalPeriod(data.getOrbitalPeriod());
            stellarObject.setAxialTilt(data.getAxialTilt());
            stellarObject.setRotationPeriod(data.getRotationalPeriod());
            stellarObject.setVolcanism(data.getVolcanismType());
            applyBodyClass(stellarObject, data);
            stellarObject.setStarName(starSystem);
            locationManager.save(stellarObject);
        }
    }

    /**
     * Classifies an EDSM body, or returns null when EDSM says nothing useful about it.
     *
     * <p>WHY: {@link LocationDto#determineType} matches descriptive words ("body", "giant", "world",
     * "star"), which EDSM carries in {@code subType}. It used to be passed {@code type}, which is only
     * ever "Star" or "Planet", so every planet fell through to null. Since a null was then stored, each
     * jump wiped the type of every non-star body in the system, including bodies the commander's own
     * scan had classified correctly. Callers must skip a null rather than persist it.
     *
     * <p>EDSM has no distinct moon type, so a body whose parent is a planet is a moon. That is the same
     * rule {@link elite.intel.gameapi.journal.ScanBodyClassifier} applies to journal scans, and it keeps
     * this path from demoting a known moon to a planet.
     */
    static LocationDto.LocationType classifyEdsmBody(BodyData data) {
        String subType = data.getSubType();
        if (subType == null || subType.isBlank()) return null;

        LocationDto.LocationType type = LocationDto.determineType(subType, data.getDistanceToArrival() == 0);
        if (type != LocationDto.LocationType.PLANET) return type;
        return orbitsAPlanet(data) ? LocationDto.LocationType.MOON : LocationDto.LocationType.PLANET;
    }

    private static boolean orbitsAPlanet(BodyData data) {
        List<ParentBody> parents = data.getParents();
        if (parents == null) return false;
        for (ParentBody parent : parents) {
            if (parent.getStar() != null) return false;
            if (parent.getPlanet() != null) return true;
        }
        return false;
    }

    /**
     * Records an EDSM body's class in the field that matches what it actually is.
     *
     * <p>WHY: EDSM reports a star's class in {@code spectralClass} ("M5") and a planet's in
     * {@code subType} ("High metal content world"); {@code spectralClass} is absent for planets. This
     * used to write {@code spectralClass} into {@code planetClass}, so every EDSM star was stored as a
     * planet whose type was a spectral code, and its star class was lost. Downstream that reads as a
     * planet: BiomeAnalyzer treats "no star class but a planet class" as a planet.
     */
    static void applyBodyClass(LocationDto stellarObject, BodyData data) {
        if ("Star".equalsIgnoreCase(data.getType())) {
            stellarObject.setStarClass(data.getSpectralClass());
        } else {
            stellarObject.setPlanetClass(data.getSubType());
        }
    }

    private static List<MaterialDto> toMaterials(Map<String, Double> materials) {
        if (materials == null) return new ArrayList<>();
        ArrayList<MaterialDto> materialDtos = new ArrayList<>();
        for (Map.Entry<String, Double> entry : materials.entrySet()) {
            materialDtos.add(new MaterialDto(entry.getKey(), entry.getValue()));
        }
        return materialDtos;
    }
}
