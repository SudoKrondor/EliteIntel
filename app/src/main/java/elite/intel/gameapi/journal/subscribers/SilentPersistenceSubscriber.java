package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.CarrierRouteLegs;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.PreScanOnly;
import elite.intel.gameapi.SignalName;
import elite.intel.gameapi.journal.ScanBodyClassifier;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.GravityCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;

/**
 * Registered only on the pre-scan private EventBus. Never on the main bus!
 * Writes location and ship data to the DB silently; fires no events, makes no
 * network calls, and does not touch the live GameEventBus.
 *
 * <p>The {@link PreScanOnly} marker is what enforces that: this class sits in a package
 * {@link elite.intel.gameapi.SubscriberRegistration} scans, so without it the live bus picks the
 * class up and its narrower writes race — and beat — the real subscribers.
 */
@PreScanOnly
public class SilentPersistenceSubscriber {

    private static final Logger log = LogManager.getLogger(SilentPersistenceSubscriber.class);

    private final LocationManager locationManager = LocationManager.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    // Tracked across events so Loadout can record the commander that owns this ship.
    private String lastCommanderName = null;

    // Seeded before the replay starts, then advanced by each replayed arrival. See recordArrival.
    private String lastKnownCarrierSystem = playerSession.getCurrentFleetCarrierSystem();
    private boolean carrierRouteNeedsReplot = false;

    @Subscribe
    public void onLoadGame(LoadGameEvent event) {
        lastCommanderName = event.getCommander();
        String displayName = playerSession.getAlternativeName() == null
                ? event.getCommander()
                : playerSession.getAlternativeName();
        playerSession.setPlayerName(displayName);
        playerSession.setInGameName(event.getCommander());
        playerSession.setCurrentShip(event.getShip());
    }

    @Subscribe
    public void onCommander(CommanderEvent event) {
        lastCommanderName = event.getName();
        playerSession.setInGameName(event.getName());
    }

    @Subscribe
    public void onLocation(LocationEvent event) {
        LocationDto dto = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBody());
        dto.setX(event.getStarPos()[0]);
        dto.setY(event.getStarPos()[1]);
        dto.setZ(event.getStarPos()[2]);
        dto.setBodyId(event.getBodyID());
        dto.setStarName(event.getStarSystem());
        dto.setPlanetName(event.getBody());
        dto.setAllegiance(event.getSystemAllegiance());
        dto.setBodyType(event.getBodyType());
        dto.setControllingPower(event.getControllingPower());
        dto.setGovernment(event.getSystemGovernmentLocalised());
        dto.setPopulation(event.getPopulation());
        dto.setSecurity(event.getSystemSecurity());
        dto.setStarName(event.getStarSystem());
        dto.setDistance(event.getDistFromStarLS());
        dto.setEconomy(event.getSystemEconomyLocalised());
        dto.setSecondEconomy(event.getSystemSecondEconomyLocalised());

        // Mirrors LocationSubscriber exactly - the pre-scan and live writers must not disagree about a body.
        // Only when the journal's body type maps to something: determineType answers null for "Planet", and
        // assigning that erases the classification a scan established.
        String bodyType = event.getBodyType() != null ? event.getBodyType().toLowerCase(Locale.ROOT) : "";
        LocationDto.LocationType type = LocationDto.determineType(bodyType, event.getDistFromStarLS() > 0);
        if (type != null) {
            dto.setLocationType(type);
        }

        dto.setPopulation(event.getPopulation());
        dto.setPowerplayState(event.getPowerplayState());
        dto.setPowerplayStateControlProgress(event.getPowerplayStateControlProgress());
        dto.setPowerplayStateReinforcement(event.getPowerplayStateReinforcement());
        dto.setPowerplayStateUndermining(event.getPowerplayStateUndermining());
        dto.setSecurity(event.getSystemSecurityLocalised());
        if (event.getSystemFaction() != null) dto.setSystemFaction(event.getSystemFaction().getName());

        if (dto.getStarName() != null && !dto.getStarName().isEmpty()) {
            locationManager.save(dto);
            // The station standing here is not the body this event names, so it gets a record of its own -
            // written after the body, for the same reason as in LocationSubscriber.
            if (event.isDocked()) {
                DockedStationRecord.of(event).store();
            }
            // Update the player's current-location pointer so queries like
            // "where are we" resolve correctly on a fresh DB.
            playerSession.setCurrentPrimaryStarName(event.getStarSystem());
            if (event.getBodyID() != null) {
                playerSession.setCurrentLocationId(event.getBodyID(), event.getSystemAddress());
            }
            log.debug("PreScan: saved location {}", dto.getStarName());
        }
    }

    @Subscribe
    public void onFSDJump(FSDJumpEvent event) {
        // WHY: keyed on the arrival body name, which is the unique column LocationDao.upsert conflicts
        // on. Keying on bodyId can miss a row that the upsert then replaces wholesale, losing its scan
        // data whenever the journal reports a different BodyID for the same body.
        LocationDto primaryStar = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBody());
        primaryStar.setBodyId(event.getBodyId());
        primaryStar.setSystemAddress(event.getSystemAddress());
        primaryStar.setStationGovernment(event.getSystemGovernmentLocalised());
        primaryStar.setAllegiance(event.getSystemAllegiance());
        primaryStar.setSecurity(event.getSystemSecurityLocalised());
        primaryStar.setStarName(event.getStarSystem());
        primaryStar.setPlanetName(event.getBody());
        primaryStar.setLocationType(PRIMARY_STAR);
        primaryStar.setX(event.getStarPos()[0]);
        primaryStar.setY(event.getStarPos()[1]);
        primaryStar.setZ(event.getStarPos()[2]);
        primaryStar.setPopulation(event.getPopulation());
        primaryStar.setPowerplayState(event.getPowerplayState());
        primaryStar.setPowerplayStateControlProgress(event.getPowerplayStateControlProgress());
        primaryStar.setPowerplayStateReinforcement(event.getPowerplayStateReinforcement());
        primaryStar.setPowerplayStateUndermining(event.getPowerplayStateUndermining());
        locationManager.save(primaryStar);
        playerSession.setCurrentPrimaryStarName(event.getStarSystem());
        playerSession.setCurrentLocationId(event.getBodyId(), event.getSystemAddress());
        log.debug("PreScan: saved jump destination {}", event.getStarSystem());
    }

    /**
     * A carrier jump moves the commander without an FSDJump or Location event. Replaying the journal
     * without this leaves the current-location pointer at the system he was in before the carrier
     * took off, so the app reports a stale position on startup.
     *
     * <p>The DOCKED status flag is not consulted: it is set only while the commander is in his ship,
     * and he may well have been on foot in the concourse. The event's own Docked/OnFoot fields say
     * whether he was aboard.
     */
    @Subscribe
    public void onCarrierJump(CarrierJumpEvent event) {
        boolean commanderAboard = event.isDocked() || event.isOnFoot();
        if (!commanderAboard) return;

        if (event.getBodyId() == null) {
            log.warn("PreScan: CarrierJump for {} carries no BodyID; location not persisted", event.getStarSystem());
            return;
        }

        LocationDto arrival = CarrierJumpLocationMapper.toArrivalLocation(event, locationManager);
        locationManager.save(arrival);
        playerSession.setCurrentPrimaryStarName(event.getStarSystem());
        // WHY: point at the id the row actually holds. LocationDto.setBodyId ignores a lower id, so
        // an event reporting BodyID 0 for an already identified body would leave the pointer stale.
        playerSession.setCurrentLocationId(arrival.getBodyId(), event.getSystemAddress());
        playerSession.setLastKnownCarrierLocation(event.getStarSystem());
        log.debug("PreScan: saved carrier jump destination {}", event.getStarSystem());
    }

    /**
     * CarrierLocation is written for every carrier arrival, including those the commander was not
     * aboard for (which produce no CarrierJump at all). Replaying it keeps the carrier's last known
     * system correct across a restart.
     *
     * <p>WHY: deliberately narrower than the live CarrierLocationSubscriber. It does not decrement
     * fuel, because the journal is replayed on every start and the decrement would compound; and it
     * does not consult EDSM, because the pre-scan makes no network calls. Coordinates come from the
     * location table, or failing that from the SystemAddress, which needs no network either.
     *
     * <p>Coordinates are written in every case: those on file belong to the system the carrier left,
     * and left in place they would be read as this system's.
     *
     * <p>A carrier jumps on its own schedule, with or without the commander in the game at all, so an
     * arrival the app was down for reaches it here or nowhere: the live parser drops every journal
     * line older than app start ({@code BaseEvent.isReplay}). That makes this the only place that can
     * retire the departure it was waiting for, and the only place that can notice the plotted route no
     * longer starts where the carrier is.
     */
    @Subscribe
    public void onCarrierLocation(CarrierLocationEvent event) {
        if (!"FleetCarrier".equalsIgnoreCase(event.getCarrierType())) return;

        String starSystem = event.getStarSystem();
        recordArrival(starSystem);
        playerSession.setLastKnownCarrierLocation(starSystem);

        CarrierDataDto carrierData = playerSession.getFleetCarrierData();
        // WHY read before the name is overwritten: the name on file is the only thing that says which
        // system the coordinates on file were resolved for.
        boolean coordinatesAreThisSystems = CarrierCoordinates.alreadyResolvedFor(carrierData, starSystem);
        carrierData.setStarName(starSystem);
        carrierData.setSystemAddress(event.getSystemAddress());

        LocationDto known = locationManager.findPrimaryStar(starSystem);
        if (known.getX() != 0 || known.getY() != 0 || known.getZ() != 0) {
            CarrierCoordinates.apply(carrierData, known.getX(), known.getY(), known.getZ());
        } else if (!coordinatesAreThisSystems) {
            // WHY only then: the journal is replayed on every start, and coordinates already resolved
            // for this system - a plotted leg's exact ones, say - must not be downgraded to an
            // estimate of it. Anything else on file belongs to the system the carrier left.
            if (!CarrierCoordinates.applyBoxelCentre(carrierData, event.getSystemAddress())) {
                CarrierCoordinates.clear(carrierData);
            }
        }
        playerSession.setFleetCarrierData(carrierData);
        log.debug("PreScan: carrier last known at {}", starSystem);
    }

    /**
     * Retires the scheduled departure this arrival completed, and notes whether it left the plotted
     * route behind.
     *
     * <p>WHY tracked in a field rather than read back from the session: this class also writes
     * {@code lastKnownCarrierLocation}, and a handler that reads the value it is about to write cannot
     * tell an arrival from a position report. The field is seeded once, before the replay begins.
     *
     * <p>WHY the departure time is cleared only on a move: the game writes CarrierLocation at every
     * LoadGame, where the carrier has not gone anywhere and a pending jump is still pending. Clearing
     * on every replay would forget it. A departure the carrier has demonstrably made is over, and
     * leaving it on file leaves the app counting down to a jump that already happened.
     */
    private void recordArrival(String starSystem) {
        String arrival = CarrierRouteLegs.normalise(starSystem);
        if (CarrierRouteLegs.isSameSystem(lastKnownCarrierSystem, arrival)) return;

        // WHY read before the route is consulted anywhere else: the arrival leg is still in the table
        // at this point, and it is the only thing that tells an on-route arrival from an off-route one.
        boolean arrivedOffRoute = FleetCarrierRouteManager.getInstance().findByPrimaryStar(arrival) == null;
        carrierRouteNeedsReplot = arrivedOffRoute;
        lastKnownCarrierSystem = arrival;

        playerSession.setCarrierDepartureTime(null);
        log.debug("PreScan: carrier moved to {} while we were down; off-route: {}", arrival, arrivedOffRoute);
    }

    /**
     * Whether the replay found the carrier somewhere its plotted route does not run from, which no
     * later live event will repair on its own. Answered after the replay, by
     * {@link elite.intel.gameapi.JournalPreScanner}, because re-plotting needs the network this class
     * must not touch.
     */
    public boolean carrierRouteNeedsReplot() {
        return carrierRouteNeedsReplot;
    }

    @Subscribe
    public void onScan(ScanEvent event) {
        if (event.getBodyID() == null) return;

        LocationDto.LocationType locationType = ScanBodyClassifier.classify(event);
        if (BELT_CLUSTER.equals(locationType)) return;

        LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());
        LocationDto primaryStar = locationManager.findPrimaryStar(event.getStarSystem());

        location.setX(primaryStar.getX());
        location.setY(primaryStar.getY());
        location.setZ(primaryStar.getZ());
        location.setStarName(event.getStarSystem());
        location.setBodyId(event.getBodyID());
        location.setSystemAddress(event.getSystemAddress());
        location.setLocationType(locationType);
        location.setPlanetName(event.getBodyName());
        location.setPlanetShortName(subtractBodyName(event.getBodyName(), event.getStarSystem()));
        location.setStarClass(event.getStarType());
        location.setVolcanism(event.getVolcanism());
        location.setMassEM(event.getMassEM());
        location.setRadius(event.getRadius());
        location.setSurfaceTemperature(event.getSurfaceTemperature());
        location.setLandable(event.isLandable());
        location.setPlanetClass(event.getPlanetClass());
        location.setTerraformable("Terraformable".equalsIgnoreCase(event.getTerraformState()));
        location.setTidalLocked(event.isTidalLock());
        location.setAtmosphere(event.getAtmosphereType());
        location.setDistance(event.getDistanceFromArrivalLS());
        // Same guard the live path applies: a nav beacon's discovery flags describe how this scan
        // learned about the body, not who charted it, and it reports WasDiscovered:false for bodies
        // settled decades ago. See ScanEventSubscriber#carriesNoDiscoveryInformation.
        if (!ScanEventSubscriber.carriesNoDiscoveryInformation(event)) {
            location.setOurDiscovery(!event.isWasDiscovered());
            location.setWeMappedIt(!event.isWasMapped());
        }
        location.setOrbitalPeriod(event.getOrbitalPeriod());
        location.setRotationPeriod(event.getRotationPeriod());
        location.setAxialTilt(event.getAxialTilt());

        Double gravity = GravityCalculator.calculateSurfaceGravity(event.getMassEM(), event.getRadius());
        if (gravity != null) location.setGravity(gravity);

        if (MOON.equals(locationType) && event.getParents() != null && !event.getParents().isEmpty()) {
            ScanEvent.Parent first = event.getParents().get(0);
            if (first.getPlanet() != null) location.setParentBodyId(first.getPlanet());
        }

        if (PLANETARY_RING.equals(locationType)) {
            location.setParentBodyName(event.getBodyName().replaceAll(" [A-Z] Ring$", ""));
        }

        if (event.getMaterials() != null) {
            List<MaterialDto> materials = new ArrayList<>();
            for (ScanEvent.Material m : event.getMaterials()) {
                materials.add(new MaterialDto(m.getName(), m.getPercent()));
            }
            location.setMaterials(materials);
        }

        locationManager.save(location);
        log.debug("PreScan: saved scan {}", event.getBodyName());
    }

    @Subscribe
    public void onSAASignalsFound(SAASignalsFoundEvent event) {
        if (event.getBodyName() == null) return;

        LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());
        LocationDto primaryStar = locationManager.findBySystemAddress(event.getSystemAddress());
        location.setBodyId(event.getBodyID());
        location.setSystemAddress(event.getSystemAddress());
        location.setPlanetName(event.getBodyName());
        location.setStarName(primaryStar.getStarName());
        location.setX(primaryStar.getX());
        location.setY(primaryStar.getY());
        location.setZ(primaryStar.getZ());
        location.addSaaSignals(event.getSignals());

        // Rings are bodies: classify, record the parent, and keep the mining reserves. Without this
        // the pre-scan drops ring signals entirely (no SAA handler previously existed here).
        if (event.getBodyName().matches(".* [A-Z] Ring")) {
            location.setLocationType(PLANETARY_RING);
            location.setParentBodyName(event.getBodyName().replaceAll(" [A-Z] Ring$", ""));
            if (event.getSignals() != null && !event.getSignals().isEmpty()) {
                List<MaterialDto> materials = new ArrayList<>();
                for (SAASignalsFoundEvent.Signal signal : event.getSignals()) {
                    // Mirrors SAASignalsFoundSubscriber.toMaterials - the two must not disagree about a name.
                    String reserve = SignalName.display(signal.getTypeLocalised(), signal.getType());
                    if (reserve != null) materials.add(new MaterialDto(reserve, 100, true));
                }
                location.setMaterials(materials);
            }
        }

        if (location.getStarName() != null && !location.getStarName().isEmpty()) {
            locationManager.save(location);
            log.debug("PreScan: saved SAA signals {}", event.getBodyName());
        }
    }

    @Subscribe
    public void onLoadout(LoadoutEvent event) {
        String shipName = LoadoutConverter.toDisplayShipName(event);

        ShipDao.Ship existing = shipManager.getShipById(event.getShipId());
        if (existing == null) {
            shipManager.save(event.getShipId(), shipName, event.getCargoCapacity(),
                    event.getShip(), KokoroVoices.BELLA.name(), lastCommanderName);
        } else {
            existing.setCargoCapacity(event.getCargoCapacity());
            existing.setShipIdentifier(event.getShip());
            existing.setShipName(shipName);
            if (lastCommanderName != null) existing.setCommanderName(lastCommanderName);
            shipManager.saveShip(existing);
        }

        // Persist the full loadout and ship name so getShipLoadout() / getShipId()
        // never return null after a fresh-install scan.
        ShipLoadOutDto loadout = LoadoutConverter.toShipLoadOutDto(event);
        playerSession.setShipLoadout(loadout);
        playerSession.setCurrentShipName(shipName);
        log.debug("PreScan: saved ship {} ({})", shipName, event.getShipId());
    }

    private static String subtractBodyName(String bodyName, String starSystem) {
        if (bodyName == null || starSystem == null) return bodyName;
        String trimmed = bodyName.replace(starSystem, "").trim();
        return trimmed.isEmpty() ? bodyName : trimmed;
    }
}
