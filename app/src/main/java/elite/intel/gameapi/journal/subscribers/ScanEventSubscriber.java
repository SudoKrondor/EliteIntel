package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.commons.BiomeAnalyzer;
import elite.intel.ai.mouth.subscribers.events.DiscoveryAnnouncementEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.SAASignalsFoundEvent;
import elite.intel.gameapi.journal.events.ScanEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.util.GravityCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;
import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.subtractString;

@SuppressWarnings("unused")
public class ScanEventSubscriber {


    private static final Logger log = LogManager.getLogger(ScanEventSubscriber.class);
    private static final Set<String> valuablePlanetClasses = Set.of(
            "ammonia world",
            "water world",
            "earthlike body",
            "water giant",
            "gas giant with ammonia-based life",
            "helium gas giant",
            "class v gas giant",
            "class iv gas giant",
            "sudarsky class ii gas giant",
            "gas giant with water-based life"
    );
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final BiomeAnalyzer biomeAnalyzer = BiomeAnalyzer.getInstance();

    private static String getDetails(ScanEvent event, String shortName) {
        boolean hasMats = event.getMaterials() != null && !event.getMaterials().isEmpty();
        boolean isTerraformable = event.getTerraformState() != null && !event.getTerraformState().isEmpty();
        boolean isLandable = event.isLandable();
        return "Previously discovered: " + shortName + " "
                + (hasMats ? ". Materials detected. " : " ")
                + (isTerraformable ? " Terraformable, " : " ")
                + (isLandable ? " landable. " : ". ");
    }

    @Subscribe
    public void onScanEvent(ScanEvent event) {
        Thread.ofVirtual().start(() -> {

            String shortName = subtractString(event.getBodyName(), event.getStarSystem());
            String bodyName = event.getBodyName();

            LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());
            LocationDto.LocationType locationType = determineLocationType(event);
            LocationDto primaryStarLocation = locationManager.findPrimaryStar(event.getStarSystem());
            location.setBodyId(event.getBodyID());
            location.setStarName(primaryStarLocation.getStarName());
            location.setX(primaryStarLocation.getX());
            location.setY(primaryStarLocation.getY());
            location.setZ(primaryStarLocation.getZ());
            location.setStarName(event.getStarSystem());
            location.setBodyId(event.getBodyID());
            location.setSystemAddress(event.getSystemAddress());
            location.setOrbitalPeriod(event.getOrbitalPeriod());


            if (BELT_CLUSTER.equals(locationType)) {
                return; // skip belt clusters.
            }

            location.setLocationType(locationType);

            // For moons, record the parent planet's bodyID so day-length can use the planet's orbital period
            if (MOON.equals(locationType) && event.getParents() != null && !event.getParents().isEmpty()) {
                ScanEvent.Parent firstParent = event.getParents().get(0);
                if (firstParent.getPlanet() != null) {
                    location.setParentBodyId(firstParent.getPlanet());
                }
            }
            location.setSystemAddress(event.getSystemAddress());
            location.setStarClass(event.getStarType());
            location.setPlanetName(event.getBodyName());
            location.setBodyId(event.getBodyID());
            location.setPlanetShortName(shortName);
            location.setVolcanism(event.getVolcanism());


            Double gravity = GravityCalculator.calculateSurfaceGravity(event.getMassEM(), event.getRadius());
            if (gravity != null)
                location.setGravity(gravity); //DO NOT use event.getSurfaceGravity() as it is not accurate
            location.setMassEM(event.getMassEM());
            location.setStarName(event.getStarSystem());
            location.setPlanetName(event.getBodyName());
            location.setRadius(event.getRadius());
            location.setSurfaceTemperature(event.getSurfaceTemperature());
            location.setLandable(event.isLandable());
            location.setPlanetClass(event.getPlanetClass());
            location.setTerraformable("Terraformable".equalsIgnoreCase(event.getTerraformState()));
            location.setTidalLocked(event.isTidalLock());
            location.setAtmosphere(event.getAtmosphereType());
            location.setMaterials(toListOfMaterials(event.getMaterials()));
            location.setDistance(event.getDistanceFromArrivalLS());
            location.setOurDiscovery(!event.isWasDiscovered());
            location.setRotationPeriod(event.getRotationPeriod());
            location.setOrbitalPeriod(event.getOrbitalPeriod());
            location.setAxialTilt(event.getAxialTilt());
            location.setWeMappedIt(!event.isWasMapped());
            location.setPlanetShortName(subtractString(event.getBodyName(), event.getStarSystem()));


            List<FSSBodySignalsEvent.Signal> fssSignals = locationManager.getLocation(event.getStarSystem(), event.getBodyID()).getFssSignals();
            List<SAASignalsFoundEvent.Signal> saaSignals = locationManager.getLocation(event.getStarSystem(), event.getBodyID()).getSaaSignals();

            int countBioSignals = 0;
            int countGeological = 0;
            if (fssSignals != null && !fssSignals.isEmpty()) {
                for (FSSBodySignalsEvent.Signal signal : fssSignals) {
                    if ("$SAA_SignalType_Biological;".equalsIgnoreCase(signal.getType())) {
                        countBioSignals = countBioSignals + signal.getCount();
                    }
                    if ("$SAA_SignalType_Geological;".equalsIgnoreCase(signal.getType())) {
                        countGeological = countGeological + signal.getCount();
                    }
                }
            }

            /// IF fss did not catch it, try saa
            if (countBioSignals == 0 || countGeological == 0) {
                if (saaSignals != null && !saaSignals.isEmpty()) {
                    for (SAASignalsFoundEvent.Signal signal : saaSignals) {
                        if (countBioSignals == 0 && "$SAA_SignalType_Biological;".equalsIgnoreCase(signal.getType())) {
                            countBioSignals = countBioSignals + signal.getCount();
                        }
                        if (countGeological == 0 && "$SAA_SignalType_Geological;".equalsIgnoreCase(signal.getType())) {
                            countGeological = countGeological + signal.getCount();
                        }
                    }
                }
            }

            location.setBioSignals(countBioSignals);
            location.setGeoSignals(countGeological);


            List<MaterialDto> materials = new ArrayList<>();
            if (event.getMaterials() != null) {
                for (ScanEvent.Material material : event.getMaterials()) {
                    materials.add(new MaterialDto(material.getName(), material.getPercent()));
                }
                location.setMaterials(materials);
            }

            announceIfNewDiscovery(event, location);
            locationManager.save(location);
            playerSession.setLastScan(location);
        });
    }

    private LocationDto.LocationType determineLocationType(ScanEvent event) {
        boolean isStar = event.getStarType() != null && !event.getStarType().isEmpty() || event.getSurfaceTemperature() > 1000;
        boolean isPrimaryStar = event.getDistanceFromArrivalLS() == 0;
        boolean isBeltCluster = event.getBodyName().contains("Belt Cluster");
        boolean isPlanet = false;
        boolean isMoon = false;
        List<ScanEvent.Parent> parents = event.getParents();
        if (parents == null || parents.isEmpty()) {
            if (isPrimaryStar) return PRIMARY_STAR;
            return isStar ? STAR : UNCLASSIFIED;
        }
        for (ScanEvent.Parent parent : parents) {
            if (parent.getStar() != null && parent.getStar() >= 0) {
                isPlanet = true;
                break;
            }
            if (parent.getPlanet() != null && parent.getPlanet() > 0) {
                isMoon = true;
                break;
            }
            if (parent.getStar() == null && event.getSurfaceTemperature() > 1000) {
                isStar = true;
                break;
            }
        }

        if (isPrimaryStar) {
            return PRIMARY_STAR;
        } else if (isStar) {
            return STAR;
        } else if (isBeltCluster) {
            return BELT_CLUSTER;
        } else if (isPlanet) {
            return PLANET;
        } else if (isMoon) {
            return MOON;
        } else {
            return UNCLASSIFIED;
        }
    }

    private void announceIfNewDiscovery(ScanEvent event, LocationDto location) {
        boolean wasDiscovered = event.isWasDiscovered();
        boolean wasMapped = event.isWasMapped();
        String shortName = subtractString(event.getBodyName(), event.getStarSystem());


        boolean isStar = event.getStarType() != null && !event.getStarType().isEmpty() || event.getSurfaceTemperature() > 1000;
        boolean isPrimaryStar = event.getDistanceFromArrivalLS() == 0;
        boolean isBeltCluster = event.getBodyName().contains("Belt Cluster");

        if (!wasDiscovered && PLANET.equals(location.getLocationType())) {
            if (event.getTerraformState() != null && !event.getTerraformState().isEmpty()) {
                EventBusManager.publish(new DiscoveryAnnouncementEvent(localizedEvent("event.scan.newTerraformable", shortName)));
            } else if (event.getPlanetClass() != null && valuablePlanetClasses.contains(event.getPlanetClass().toLowerCase())) {
                EventBusManager.publish(new DiscoveryAnnouncementEvent(localizedEvent("event.scan.newDiscovery", event.getPlanetClass())));
            }
        }

        if (wasDiscovered && !STAR.equals(location.getLocationType())) {
            if (!BELT_CLUSTER.equals(location.getLocationType())) {
                String sensorData = getDetails(event, shortName);

                log.info(sensorData);
            }
        } else if (!wasDiscovered && PRIMARY_STAR.equals(location.getLocationType())) {
            EventBusManager.publish(new DiscoveryAnnouncementEvent(localizedEvent("event.scan.newSystem")));
        }
    }

    private List<MaterialDto> toListOfMaterials(List<ScanEvent.Material> materials) {
        if (materials == null) return new ArrayList<>();
        ArrayList<MaterialDto> result = new ArrayList<>();
        for (ScanEvent.Material material : materials) {
            result.add(new MaterialDto(material.getName(), material.getPercent()));
        }
        return result;
    }
}
