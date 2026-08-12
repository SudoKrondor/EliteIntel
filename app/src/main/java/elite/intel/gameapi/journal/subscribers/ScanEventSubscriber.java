package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.commons.BiomeAnalyzer;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.ScanBodyClassifier;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;
import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.subtractString;

@SuppressWarnings("unused")
public class ScanEventSubscriber {


    /**
     * {@code ScanType} of the bulk system dump a nav beacon produces. Its discovery flags are not to be
     * believed; see {@link #carriesNoDiscoveryInformation}.
     */
    private static final String NAV_BEACON_SCAN = "NavBeaconDetail";
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

    /**
     * Bodies already announced in {@link #announcedSystemAddress}; see {@link #isFirstAnnouncementForBody}.
     */
    private final Set<Long> announcedBodies = new HashSet<>();
    private long announcedSystemAddress = Long.MIN_VALUE;

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
            LocationDto.LocationType locationType = ScanBodyClassifier.classify(event);

            if (BELT_CLUSTER.equals(locationType)) {
                return; // skip belt clusters.
            }

            // Read-modify-write under a per-body lock so a concurrent SAASignalsFound for the same
            // body can't interleave and wipe its signals/materials (blind whole-JSON upsert).
            locationManager.updateBody(event.getSystemAddress(), event.getBodyID(), location -> {
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
                // A nav beacon's discovery flags describe how this scan learned about the body, not who found
                // it first (see carriesNoDiscoveryInformation), so they are not recorded. Leaving the stored
                // values alone keeps whatever a real scan established; an unseen body then stays "not ours",
                // which is the only answer a beacon can support and the safe one in a populated system.
                if (!carriesNoDiscoveryInformation(event)) {
                    location.setOurDiscovery(!event.isWasDiscovered());
                    location.setWeMappedIt(!event.isWasMapped());
                }
                location.setRotationPeriod(event.getRotationPeriod());
                location.setOrbitalPeriod(event.getOrbitalPeriod());
                location.setAxialTilt(event.getAxialTilt());
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
                playerSession.setLastScan(location);
            });
        });
    }

    private void announceIfNewDiscovery(ScanEvent event, LocationDto location) {
        if (carriesNoDiscoveryInformation(event)) return;

        boolean wasDiscovered = event.isWasDiscovered();
        boolean wasMapped = event.isWasMapped();
        String shortName = subtractString(event.getBodyName(), event.getStarSystem());

        if (!wasDiscovered && PLANET.equals(location.getLocationType())) {
            if (event.getTerraformState() != null && !event.getTerraformState().isEmpty()) {
                announceOnce(event, localizedEvent("event.scan.newTerraformable", shortName));
            } else if (event.getPlanetClass() != null && valuablePlanetClasses.contains(event.getPlanetClass().toLowerCase())) {
                announceOnce(event, localizedEvent("event.scan.newDiscovery", event.getPlanetClass()));
            }
        }

        if (wasDiscovered && !STAR.equals(location.getLocationType())) {
            if (!BELT_CLUSTER.equals(location.getLocationType())) {
                String sensorData = getDetails(event, shortName);

                log.info(sensorData);
            }
        } else if (!wasDiscovered && PRIMARY_STAR.equals(location.getLocationType())) {
            announceOnce(event, localizedEvent("event.scan.newSystem"));
        }
    }

    /**
     * A nav beacon hands over the whole system's body data at once, and the Scan events it produces report
     * {@code WasDiscovered:false} for bodies that were charted decades ago. The flags describe how <em>this</em>
     * scan learned about the body, not whether anyone had been there before.
     * <p>
     * The journal contradicts itself outright: of 97 {@code NavBeaconDetail} scans in one session, 37 claimed
     * {@code WasMapped:true} with {@code WasDiscovered:false}, which cannot happen. Every {@code WasDiscovered:false}
     * in that session came from this scan type; {@code AutoScan} and {@code Detailed} were always true.
     * <p>
     * Believing them announced "New System discovered!" for Wolf 1323, a populated system with named planets.
     * The signal is in fact the opposite of a discovery: nav beacons only exist in populated systems, so a
     * beacon scan is near proof the system has been settled for a long time.
     */
    static boolean carriesNoDiscoveryInformation(ScanEvent event) {
        return NAV_BEACON_SCAN.equalsIgnoreCase(event.getScanType());
    }

    /**
     * Announces a discovery at most once per body.
     * <p>
     * The game emits several Scan events for the same body - an {@code AutoScan} on arrival and a
     * {@code Detailed} scan when it is honked or targeted - all carrying the same
     * {@code WasDiscovered:false}. Without this guard each one announced, so arriving in an
     * undiscovered system said "New System discovered!" twice, seconds apart.
     */
    private void announceOnce(ScanEvent event, String message) {
        if (isFirstAnnouncementForBody(event.getSystemAddress(), event.getBodyID())) {
            CompanionRuntime.narrator().announce(message, false);
        }
    }

    /**
     * Records a body as announced and reports whether this call was the first to do so.
     * <p>
     * Scoped to the current system so the set stays bounded on a long exploration run: leaving a
     * system drops its bodies, and re-entering one can only re-announce bodies that are by then
     * flagged {@code WasDiscovered:true} anyway. Synchronized because scans are handled on virtual
     * threads and two scans of the same body can be in flight together.
     * <p>
     * // WHY: a body with no {@code BodyID} always announces. Folding those onto one shared key would
     * silence every unidentified body in the system after the first, turning a missing field into a
     * lost discovery; repeating an announcement is the cheaper failure than never making it.
     */
    synchronized boolean isFirstAnnouncementForBody(long systemAddress, Long bodyId) {
        if (systemAddress != announcedSystemAddress) {
            announcedSystemAddress = systemAddress;
            announcedBodies.clear();
        }
        if (bodyId == null) {
            return true;
        }
        return announcedBodies.add(bodyId);
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
