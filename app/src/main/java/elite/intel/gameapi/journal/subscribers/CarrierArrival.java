package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.CarrierRouteLegs;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.carrier.OurCarriers;
import elite.intel.gameapi.journal.events.CarrierStatsEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.edsm.EdsmApiClient;
import elite.intel.gameapi.search.edsm.dto.StarSystemDto;
import elite.intel.gameapi.search.edsm.dto.data.StarSystemCoordinates;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * What a carrier arrival costs and changes: the tritium burned, the leg reached, the scheduled departure it
 * ends, and whether the plotted route survives it.
 *
 * <p>WHY a single owner rather than a step in each subscriber: the game reports one arrival twice when the
 * commander rode along, as CarrierLocation and then CarrierJump, and each subscriber does its work on its own
 * virtual thread. The whole sequence turns on one read - "is the system it reports different from the one on
 * file?" - and both events answer it by writing that same field. Whichever thread wrote first made the other
 * believe the carrier had never moved, so the jump was never charged, the scheduled departure was never
 * cleared, and an off-route arrival never voided the route. The commander heard the level his depot held
 * <em>before</em> the jump, which after departing full reads as the depot's exact capacity.
 *
 * <p>So the decision runs under one lock and is idempotent: the first event through does the bookkeeping, and
 * the second finds the carrier already recorded here and costs nothing. The lock covers only the decision and
 * the writes it implies; resolving coordinates reaches the network and is done outside it, so a slow lookup
 * can never stall the arrival behind it or the announcement waiting on it.
 *
 * <p>Nothing here plots a route. The commander's own {@code calculate_fleet_carrier_route} is the only thing
 * in the app that writes legs, so an arrival can consume a route or void one, never create one.
 */
final class CarrierArrival {

    private static final Logger log = LogManager.getLogger(CarrierArrival.class);

    /**
     * Serialises the two events that describe one arrival.
     */
    private static final Object LOCK = new Object();

    /**
     * What the last arrival did to the plotted voyage, for the announcement to report.
     */
    enum VoyageStatus {
        /**
         * No route was plotted, so this arrival says nothing about one.
         */
        NO_ROUTE,
        /**
         * A plotted leg was reached and more remain.
         */
        EN_ROUTE,
        /**
         * The last plotted leg was reached: the voyage is over.
         */
        DESTINATION_REACHED,
        /**
         * The carrier arrived somewhere the route never mentioned, so the route was dropped.
         */
        ROUTE_ABANDONED
    }

    /**
     * The system the status below was recorded for, and that status.
     *
     * <p>WHY recorded rather than worked out by the announcement: the two events that describe one
     * arrival reach this class in either order, and only the first one finds the route as it was
     * before the arrival. By the time CarrierJumpCompleteSubscriber writes its announcement the legs
     * are already consumed or the route already dropped, so the table can no longer say which
     * happened. Keyed by system so the second event of the same arrival reads the first one's answer.
     */
    private static String lastArrivalSystem;
    private static VoyageStatus lastArrivalStatus = VoyageStatus.NO_ROUTE;

    private CarrierArrival() {
    }

    /**
     * Records the fleet carrier as being in {@code starSystem}, charging the arrival exactly once.
     *
     * <p>The game also writes an arrival event at every LoadGame, where the carrier has not moved at all.
     * Such a position report must cost no tritium, cancel no scheduled jump and plot no route; it only
     * confirms where the carrier is. Everything here therefore turns on whether the reported system differs
     * from the one already on file, never on where the commander happens to be standing.
     */
    static void recordFleetArrival(String starSystem, Long systemAddress) {
        recordFleetArrival(starSystem, systemAddress, null);
    }

    /**
     * As above, for an event that carries the destination's own coordinates.
     *
     * <p>CarrierJump does, and they are exact and free, so passing them here both improves on what the leg
     * or the network could tell us and spares the announcement a lookup it does not need: an off-route
     * arrival otherwise resolves coordinates over EDSM, and the commander would wait out that round trip
     * before hearing where his carrier is.
     *
     * @param authoritativeStarPos exact galactic coordinates, or null when the event carries none
     */
    static void recordFleetArrival(String starSystem, Long systemAddress, double[] authoritativeStarPos) {
        boolean carrierMoved;
        boolean coordinatesNeedResolving;

        // Everything the two events race on, and nothing else: reading whether the carrier moved and acting
        // on that answer is one indivisible step, but it touches no network.
        synchronized (LOCK) {
            PlayerSession playerSession = PlayerSession.getInstance();

            // WHY read before writing: the event alone cannot say whether the carrier moved. Comparing it
            // against the system we already believed is the only way to tell an arrival from a position report.
            String previousSystem = playerSession.getCurrentFleetCarrierSystem();
            carrierMoved = !CarrierRouteLegs.isSameSystem(previousSystem, starSystem);

            playerSession.setLastKnownCarrierLocation(starSystem);

            FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
            CarrierJump completedLeg = route.findByPrimaryStar(starSystem);
            // WHY the raw count and not the route a read hands out: that one is truncated at the
            // carrier's own system, so a route the carrier is already sitting at the end of reads as
            // no route at all - and its dead rows would then survive this arrival and come back as
            // leg 1 the next time the carrier moves.
            boolean routePlotted = route.hasStoredLegs();

            CarrierDataDto carrierData = playerSession.getFleetCarrierData();
            carrierData.setStarName(starSystem);
            carrierData.setSystemAddress(systemAddress);

            if (completedLeg != null && carrierMoved) {
                // WHY: the leg carries the tritium this hop burned, so it must be read before
                // removeLeg. Nothing else decrements carrier fuel on arrival, and charging it again
                // for a system the carrier was already sitting in would burn tritium for no jump.
                carrierData.chargeEstimatedFuel(completedLeg.getFuelUsed());
            }

            if (authoritativeStarPos != null) {
                CarrierCoordinates.apply(carrierData,
                        authoritativeStarPos[0], authoritativeStarPos[1], authoritativeStarPos[2]);
            } else if (completedLeg != null) {
                CarrierCoordinates.apply(carrierData,
                        completedLeg.getX(), completedLeg.getY(), completedLeg.getZ());
            }
            // Off-route arrival with no coordinates of its own: the ones on file belong to the system the
            // carrier left, so they have to be resolved afresh - outside this block, below. A position
            // report for the system we are already in resolves only when we never learned its position.
            coordinatesNeedResolving = authoritativeStarPos == null && completedLeg == null
                    && (carrierMoved || coordinatesUnknown(carrierData));

            playerSession.setFleetCarrierData(carrierData);

            // WHY: the system we are sitting in is never part of the remaining route. Unconditional, so
            // that a position report also repairs a route left stale by a jump made while we were down.
            route.removeLeg(starSystem);

            // WHY a position report stops here: the game writes an arrival event at every LoadGame, where
            // a pending departure is still pending and the route is still the one the carrier is on.
            // Clearing the timer on every replay would forget a scheduled jump, and voiding the route on
            // one would throw away a voyage the carrier has not deviated from.
            if (carrierMoved) {
                playerSession.setCarrierDepartureTime(null);
            }

            // WHY the route is dropped rather than re-plotted from here: Spansh plots a carrier's legs
            // to the ton, so a carrier that arrives somewhere the route never mentioned has not drifted
            // off it - the commander changed his mind and jumped elsewhere, and the voyage he plotted is
            // not the one he is on. Re-plotting instead made an abandoned route immortal: it survived
            // every manual jump, was quoted back at him as "N jumps left" on each one, and re-plotted
            // itself to the same destination from wherever he landed - including at startup, which is
            // where a commander who had cleared it got it back.
            boolean routeAbandoned = carrierMoved && completedLeg == null && routePlotted;
            if (routeAbandoned) {
                route.clear();
            }

            if (carrierMoved) {
                lastArrivalSystem = CarrierRouteLegs.normalise(starSystem);
                lastArrivalStatus = recordedStatus(route, completedLeg, routeAbandoned);
            }
        }

        // WHY out here: both of these reach the network, and a lock held across a call that can hang would
        // stall every later arrival behind it - including the announcement, which waits on this owner to be
        // sure it quotes a fuel level the arrival has already charged.
        if (coordinatesNeedResolving) {
            resolveAndCommitCoordinates(starSystem, systemAddress);
        }
    }

    /**
     * What this arrival did to the voyage, read once while the route is still settled under the lock.
     */
    private static VoyageStatus recordedStatus(FleetCarrierRouteManager route,
                                               CarrierJump completedLeg,
                                               boolean routeAbandoned) {
        if (routeAbandoned) return VoyageStatus.ROUTE_ABANDONED;
        if (completedLeg == null) return VoyageStatus.NO_ROUTE;
        return route.getFleetCarrierRoute().isEmpty()
                ? VoyageStatus.DESTINATION_REACHED
                : VoyageStatus.EN_ROUTE;
    }

    /**
     * What the arrival in {@code starSystem} did to the plotted voyage.
     *
     * <p>{@link VoyageStatus#NO_ROUTE} for any system this class did not record an arrival for, which
     * is the right answer for the case that produces it: a one-off jump the commander scheduled
     * himself, with no route to report on either side of it.
     */
    static VoyageStatus voyageStatusAt(String starSystem) {
        synchronized (LOCK) {
            return CarrierRouteLegs.isSameSystem(lastArrivalSystem, starSystem)
                    ? lastArrivalStatus
                    : VoyageStatus.NO_ROUTE;
        }
    }

    /**
     * Resolves the arrival system's coordinates and writes them, unless a later arrival has moved the
     * carrier on in the meantime: those coordinates belong to a system it has already left.
     */
    private static void resolveAndCommitCoordinates(String starSystem, Long systemAddress) {
        CarrierDataDto resolved = new CarrierDataDto();
        resolved.setStarName(starSystem);
        resolveCoordinates(resolved, starSystem, systemAddress);

        synchronized (LOCK) {
            PlayerSession playerSession = PlayerSession.getInstance();
            CarrierDataDto carrierData = playerSession.getFleetCarrierData();
            if (!starSystem.equalsIgnoreCase(carrierData.getStarName())) {
                return;
            }
            CarrierCoordinates.apply(carrierData, resolved.getX(), resolved.getY(), resolved.getZ());
            playerSession.setFleetCarrierData(carrierData);
        }
    }

    /**
     * The carrier record as it stands once the arrival has been accounted for, for an announcement to read.
     * Taking the lock means an announcement can never quote a level the arrival is still about to change.
     */
    static CarrierDataDto settledFleetCarrierData() {
        synchronized (LOCK) {
            return PlayerSession.getInstance().getFleetCarrierData();
        }
    }

    /**
     * Applies a full reading of the carrier from the game, taken when the commander opens carrier management.
     *
     * <p>Under the arrival lock like everything else that rewrites this record: it is read, changed and
     * written back, so landing it in the middle of an arrival would drop whichever change lost the race.
     * Losing this one is the worse way round - it is the only exact figure we ever get, and the commander
     * most often asks for it just as the carrier arrives.
     */
    static void applyCarrierStats(CarrierStatsEvent event) {
        synchronized (LOCK) {
            PlayerSession.getInstance().setCarrierStats(event);
        }
    }

    /**
     * Applies the depot total a fuel deposit confirms, which the game states outright and we therefore treat
     * as a reading rather than as arithmetic.
     * <p>
     * Filed under the carrier the deposit names. Falling back to the fleet carrier when the id matches
     * neither is not a guess but the old behaviour preserved: a carrier whose management panel has never
     * been opened has no id on file, and for the great majority of commanders there is only ever one
     * carrier for the reading to be about.
     */
    static void applyFuelReading(long carrierId, int tons) {
        synchronized (LOCK) {
            Optional<OurCarriers.Ours> named = OurCarriers.byId(carrierId);
            if (named.isPresent()) {
                named.get().update(carrier -> carrier.setMeasuredFuelLevel(tons));
                return;
            }
            FleetCarrierManager manager = FleetCarrierManager.getInstance();
            CarrierDataDto carrierData = manager.get();
            carrierData.setMeasuredFuelLevel(tons);
            manager.save(carrierData);
        }
    }

    private static boolean coordinatesUnknown(CarrierDataDto carrierData) {
        return carrierData.getX() == 0 && carrierData.getY() == 0 && carrierData.getZ() == 0;
    }

    /**
     * The arrival system was not on the plotted route, so its coordinates have to be resolved from
     * scratch: the event itself never carries StarPos, and the coordinates already on file belong to
     * the system the carrier left.
     *
     * <p>Three sources, best first. The location table is exact and free whenever the commander has
     * flown here himself. EDSM is exact for charted space. The SystemAddress is approximate but
     * always available, and it is the only one of the three that answers in uncharted space — which
     * is where a carrier arrival the commander was not aboard for usually happens.
     *
     * <p>WHY it must write something in every case: coordinates left over from the previous system
     * would be read as this system's, and the distance query would confidently report a figure for a
     * place the carrier is not. Failing all three sources, they are cleared, and the query says it
     * does not know.
     */
    static void resolveCoordinates(CarrierDataDto carrierData, String starSystem, Long systemAddress) {
        LocationDto location = LocationManager.getInstance().findPrimaryStar(starSystem);
        if (location.getX() != 0 || location.getY() != 0 || location.getZ() != 0) {
            CarrierCoordinates.apply(carrierData, location.getX(), location.getY(), location.getZ());
            return;
        }

        StarSystemDto starSystemDto = EdsmApiClient.searchStarSystem(starSystem, 1);
        StarSystemCoordinates coords = starSystemDto.getCoords();
        boolean isSol = starSystemDto.getData() != null
                && "sol".equalsIgnoreCase(starSystemDto.getData().getName());
        boolean hasValidCoords = coords != null
                && (isSol || coords.getX() != 0 || coords.getY() != 0 || coords.getZ() != 0);

        if (hasValidCoords) {
            CarrierCoordinates.apply(carrierData, coords.getX(), coords.getY(), coords.getZ());
            return;
        }

        if (CarrierCoordinates.applyBoxelCentre(carrierData, systemAddress)) return;

        log.warn("No coordinate source for carrier system {}; clearing the stale ones", starSystem);
        CarrierCoordinates.clear(carrierData);
    }
}
