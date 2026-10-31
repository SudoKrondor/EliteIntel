package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.*;
import elite.intel.gameapi.PreScanOnly;
import elite.intel.gameapi.journal.ScanBodyClassifier;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.BELT_CLUSTER;

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
    private final FssSurveyManager fssSurveys = FssSurveyManager.getInstance();

    // Tracked across events so Loadout can record the commander that owns this ship.
    private String lastCommanderName = null;

    // Where the carrier was when the app last ran, read before the replay rewrites it, and the last
    // position the replay saw. Compared once the replay is over - see settleCarrierArrival.
    private final String carrierSystemBeforeReplay = playerSession.getCurrentFleetCarrierSystem();
    private String lastReplayedCarrierSystem = null;

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
        LocationDto body = LocationEventMapper.record(event, record -> {
        });
        // The station standing here is not the body this event names, so it gets a record of its own -
        // written after the body, for the same reason as in LocationSubscriber.
        if (event.isDocked()) {
            DockedStationRecord.of(event).store();
        }
        // WHY: point at the id the row actually holds, as LocationSubscriber and the carrier jump do.
        // LocationDto.setBodyId ignores a lower id, so the journal's 0 for an already identified body
        // would leave the pointer aimed at nothing.
        Long bodyId = body == null ? event.getBodyID() : Long.valueOf(body.getBodyId());
        playerSession.moveTo(new Position(event.getSystemAddress(), event.getStarSystem(), bodyId));
        log.debug("PreScan: saved location {}", event.getStarSystem());
    }

    @Subscribe
    public void onFSDJump(FSDJumpEvent event) {
        FsdJumpArrival.record(event);
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

        LocationDto arrival = CarrierJumpLocationMapper.recordArrival(event, locationManager);
        // WHY: point at the id the row actually holds. LocationDto.setBodyId ignores a lower id, so
        // an event reporting BodyID 0 for an already identified body would leave the pointer stale.
        playerSession.moveTo(new Position(event.getSystemAddress(), event.getStarSystem(), arrival.getBodyId()));
        lastReplayedCarrierSystem = event.getStarSystem();
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
        lastReplayedCarrierSystem = starSystem;
        playerSession.setLastKnownCarrierLocation(starSystem);

        CarrierDataDto carrierData = playerSession.getFleetCarrierData();
        // WHY read before the name is overwritten: the name on file is the only thing that says which
        // system the coordinates on file were resolved for.
        boolean coordinatesAreThisSystems = CarrierCoordinates.alreadyResolvedFor(carrierData, starSystem);
        carrierData.setStarName(starSystem);
        carrierData.setSystemAddress(event.getSystemAddress());

        LocationDto known = locationManager.findSystemRecord(event.getSystemAddress());
        if (known.getBodyId() != -1) {
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
     * Called once the replay is over: retires the scheduled departure the carrier completed while the
     * app was down, and voids the plotted route if the carrier left it behind.
     *
     * <p>WHY once at the end rather than on each replayed CarrierLocation: the replay walks history
     * forward from the oldest journal, but the system it is compared against is where the carrier was
     * when the app last ran - its newest position. The first CarrierLocation of the replay is
     * therefore usually an older system, and reading it as an arrival voided every route whose
     * carrier had moved anywhere inside the replayed journals: a commander who jumped leg 1 and
     * restarted the app found the rest of his route gone. Only the carrier's last position in the
     * replay can say whether it moved since then.
     *
     * <p>WHY the departure time is cleared only on a move: the game writes CarrierLocation at every
     * LoadGame, where the carrier has not gone anywhere and a pending jump is still pending. Clearing
     * on every replay would forget it. A departure the carrier has demonstrably made is over, and
     * leaving it on file leaves the app counting down to a jump that already happened.
     *
     * <p>WHY the route is dropped rather than re-plotted: it used to be re-plotted, which is the one
     * network call the pre-scan made - and since the destination came from the route being replaced,
     * a route the commander had cleared came back at the next start. A carrier that jumped somewhere
     * the route never mentioned is not on that voyage any more, and the app has nothing to say about
     * a voyage the commander abandoned.
     */
    public void settleCarrierArrival() {
        String arrival = CarrierRouteLegs.normalise(lastReplayedCarrierSystem);
        if (arrival == null || CarrierRouteLegs.isSameSystem(carrierSystemBeforeReplay, arrival)) return;

        playerSession.setCarrierDepartureTime(null);

        // WHY consulted before anything else writes the route: the arrival leg is still in the table at
        // this point, and it is the only thing that tells an on-route arrival from an off-route one.
        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        boolean arrivedOffRoute = route.findByPrimaryStar(arrival) == null;
        if (arrivedOffRoute && route.hasStoredLegs()) {
            log.debug("PreScan: carrier jumped to {} off its plotted route while we were down; route voided", arrival);
            route.clear();
        }
        log.debug("PreScan: carrier moved to {} while we were down; off-route: {}", arrival, arrivedOffRoute);
    }

    @Subscribe
    public void onScan(ScanEvent event) {
        if (event.getBodyName() == null || event.getBodyID() == null) return;
        if (BELT_CLUSTER.equals(ScanBodyClassifier.classify(event))) return;

        LocationDto system = locationManager.findSystemRecord(event.getSystemAddress());
        locationManager.updateNamedBody(event.getSystemAddress(), event.getBodyID(), event.getBodyName(),
                body -> ScanRecord.describe(body, event, system));
        log.debug("PreScan: saved scan {}", event.getBodyName());
    }

    @Subscribe
    public void onFSSDiscoveryScan(FSSDiscoveryScanEvent event) {
        // Mirrors FssSurveySubscriber: a honk made before the app started still gives the explorer card its count.
        fssSurveys.recordBodyCount(event.getSystemAddress(), event.getBodyCount());
    }

    @Subscribe
    public void onFSSAllBodiesFound(FSSAllBodiesFoundEvent event) {
        fssSurveys.markAllBodiesFound(event.getSystemAddress(), event.getCount());
    }

    /**
     * The FSS repeats no Scan for a body the commander resolved on an earlier visit, so for such a body the
     * signal report is the only journal line there is - and the live bus skips every line older than the
     * app start. Without this the six bio moons the commander FSS'd before a restart stayed unknown until
     * they were FSS'd again.
     */
    @Subscribe
    public void onFSSBodySignals(FSSBodySignalsEvent event) {
        if (FSSBodySignalsSubscriber.record(event) != null) {
            log.debug("PreScan: saved FSS body signals {}", event.getBodyName());
        }
    }

    @Subscribe
    public void onSAASignalsFound(SAASignalsFoundEvent event) {
        if (SaaSignalsRecord.record(event, body -> {
        }) != null) {
            log.debug("PreScan: saved SAA signals {}", event.getBodyName());
        }
    }

    @Subscribe
    public void onLoadout(LoadoutEvent event) {
        String shipName = LoadoutConverter.toDisplayShipName(event);

        ShipDao.Ship existing = shipManager.getShipById(event.getShipId());
        if (existing == null) {
            shipManager.save(event.getShipId(), shipName, event.getCargoCapacity(),
                    event.getShip(), SupertonicVoices.BELLA.name(), lastCommanderName);
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

}
