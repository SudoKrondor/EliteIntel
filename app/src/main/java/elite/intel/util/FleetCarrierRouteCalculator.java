package elite.intel.util;

import elite.intel.companion.CompanionRuntime;
import elite.intel.db.managers.CarrierRouteLegs;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import elite.intel.search.spansh.carrierroute.CarrierRouteCriteria;
import elite.intel.search.spansh.carrierroute.SpanshCarrierRouteClient;
import elite.intel.search.spansh.nearest.NearestKnownLocationSearchClient;
import elite.intel.session.PlayerSession;

import java.util.Map;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

public class FleetCarrierRouteCalculator {

    public static String calculate() {

        SpanshCarrierRouteClient client = new SpanshCarrierRouteClient();
        PlayerSession playerSession = PlayerSession.getInstance();
        FleetCarrierRouteManager routeManager = FleetCarrierRouteManager.getInstance();
        CarrierDataDto carrierData = playerSession.getFleetCarrierData();

        int fuelSupply = carrierData.getFuelLevel();
        int tritiumInReserve = carrierData.getFuelReserve();
        if (tritiumInReserve > 0) {
            fuelSupply = fuelSupply + tritiumInReserve;
        }

        int cargoCapacity = carrierData.getCargoCapacity();
        int cargoSpaceUsed = carrierData.getCargoSpaceUsed();
        String destination = CarrierRouteLegs.normalise(ClipboardUtils.getClipboardText());

        if (destination == null) {
            return localizedEvent("event.carrier.route.noDestination");
        }

        CompanionRuntime.narrator().filler(localizedEvent("event.carrier.route.accessing"), false);

        String origin = resolveOrigin(playerSession, carrierData);
        if (origin == null) {
            return localizedEvent("event.carrier.route.locationUnavailable");
        }

        CarrierRouteCriteria carrierRouteCriteria = new CarrierRouteCriteria(
                origin,
                destination,
                cargoCapacity,
                cargoSpaceUsed,
                fuelSupply
        );

        Map<Integer, CarrierJump> plotted = client.calculateRoute(carrierRouteCriteria);
        if (plotted.isEmpty()) {
            // WHY return before storing: an unplottable destination must leave the current route
            // standing, and must not be reported against the legs of the route it failed to replace.
            return localizedEvent("event.carrier.route.navFailed", destination);
        }

        routeManager.setFleetCarrierRoute(plotted);

        // WHY read back rather than count the plot: the manager drops the legs the carrier has
        // already flown, so only the stored route knows what is still ahead of it.
        int numJumps = routeManager.getFleetCarrierRoute().size();
        int fuelRequired = routeManager.getTotalFuelRequired();

        if (numJumps == 0) {
            return localizedEvent("event.carrier.route.navFailed", destination);
        } else {
            return localizedEvent("event.carrier.route.calculated", destination, localizedEventPlural(numJumps, "event.carrier.jump.count"), fuelRequired)
                   + " "
                   + localizedEvent("event.carrier.route.nextStep");
        }
    }

    /**
     * The system the route is plotted from: the one the carrier is actually in.
     *
     * <p>WHY not the nearest system to the carrier's coordinates: the coordinates lag the system
     * name. The startup pre-scan keeps the previous system's coordinates whenever the new one is not
     * yet in the location table, so a coordinate lookup resolves to the system the carrier left.
     * Spansh then plots from there, and since the client drops only the first jump of what it is
     * given, the carrier's own system comes back as leg 1 — a destination it is already sitting in.
     *
     * <p>Coordinates remain the fallback for a carrier whose system name we have never seen.
     *
     * <p>Asks the session for the same answer the route table truncates at, so the plot cannot start
     * one system away from where the route is trimmed.
     */
    private static String resolveOrigin(PlayerSession playerSession, CarrierDataDto carrierData) {
        String carrierSystem = playerSession.getCurrentFleetCarrierSystem();
        if (carrierSystem != null) return carrierSystem;

        boolean coordinatesUnknown = carrierData.getX() == 0 && carrierData.getY() == 0 && carrierData.getZ() == 0;
        if (coordinatesUnknown) return null;

        LocationDto nearest = NearestKnownLocationSearchClient.findNearest(
                carrierData.getX(), carrierData.getY(), carrierData.getZ());
        return nearest == null ? null : CarrierRouteLegs.normalise(nearest.getStarName());
    }
}
