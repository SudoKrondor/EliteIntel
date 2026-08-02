package elite.intel.util;

import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.CarrierRouteLegs;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierRouteCriteria;
import elite.intel.gameapi.search.spansh.carrierroute.SpanshCarrierRouteClient;
import elite.intel.gameapi.search.spansh.nearest.NearestKnownLocationSearchClient;
import elite.intel.session.PlayerSession;

import java.util.Map;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

public class FleetCarrierRouteCalculator {

    public static String calculate() {

        PlayerSession playerSession = PlayerSession.getInstance();
        FleetCarrierRouteManager routeManager = FleetCarrierRouteManager.getInstance();
        CarrierDataDto carrierData = playerSession.getFleetCarrierData();

        String destination = CarrierRouteLegs.normalise(ClipboardUtils.getClipboardText());

        if (destination == null) {
            return localizedEvent("event.carrier.route.noDestination");
        }

        CompanionRuntime.narrator().filler(localizedEvent("event.carrier.route.accessing"), false);

        String origin = resolveOrigin(playerSession, carrierData);
        if (origin == null) {
            return localizedEvent("event.carrier.route.locationUnavailable");
        }

        if (!plotAndStore(origin, destination)) {
            // WHY nothing was stored: an unplottable destination must leave the current route
            // standing, and must not be reported against the legs of the route it failed to replace.
            return localizedEvent("event.carrier.route.navFailed", destination);
        }

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
     * Plots from one system to another and stores the result, saying nothing to anyone.
     *
     * <p>WHY separate from {@link #calculate()}: that one is the commander's own request, so it reads
     * his clipboard for the destination and speaks while it works. An automatic re-plot has its
     * destination already, must not touch the clipboard, and may run before the companion is up.
     *
     * @return false when Spansh found no route, in which case the stored route is left standing.
     */
    public static boolean plotAndStore(String origin, String destination) {
        CarrierDataDto carrierData = PlayerSession.getInstance().getFleetCarrierData();

        int tritiumInReserve = carrierData.getFuelReserve();
        int fuelSupply = carrierData.getFuelLevel() + Math.max(0, tritiumInReserve);

        CarrierRouteCriteria criteria = new CarrierRouteCriteria(
                origin,
                destination,
                carrierData.getCargoCapacity(),
                carrierData.getCargoSpaceUsed(),
                fuelSupply
        );

        Map<Integer, CarrierJump> plotted = new SpanshCarrierRouteClient().calculateRoute(criteria);
        if (plotted.isEmpty()) return false;

        FleetCarrierRouteManager.getInstance().setFleetCarrierRoute(plotted);
        return true;
    }

    /**
     * The system the route is plotted from: the one the carrier is actually in.
     *
     * <p>WHY not the nearest system to the carrier's coordinates: the name is exact and the
     * coordinates need not be. For a system nobody has flown to, they are the centre of the boxel the
     * SystemAddress names, so the nearest known system to them can be a neighbour rather than the
     * carrier's own. Spansh would then plot from there, and since the client drops only the first jump
     * of what it is given, the carrier's own system could come back as leg 1: a destination it is
     * already sitting in.
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
