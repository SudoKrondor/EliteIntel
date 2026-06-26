package elite.intel.util;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
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

    /**
     * Calculates the fleet-carrier jump route and returns its outcome (see {@link CommandOutcome}) instead
     * of narrating itself, so each caller folds it in: the command returns it as its outcome, the
     * carrier-jump event subscriber speaks it. Returns {@code null} only if there is nothing to say.
     */
    public static JsonObject calculate() {

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
        String destination = ClipboardUtils.getClipboardText();

        if (carrierData.getX() == 0 && carrierData.getY() == 0 && carrierData.getZ() == 0) {
            return CommandOutcome.speak(localizedEvent("event.carrier.route.locationUnavailable"));
        }

        LocationDto nearestStartingPoint = NearestKnownLocationSearchClient.findNearest(carrierData.getX(), carrierData.getY(), carrierData.getZ());

        if (destination == null || nearestStartingPoint == null) {
            return CommandOutcome.speak(localizedEvent("event.carrier.route.noDestination"));
        }

        CarrierRouteCriteria carrierRouteCriteria = new CarrierRouteCriteria(
                nearestStartingPoint.getStarName(),
                destination,
                cargoCapacity,
                cargoSpaceUsed,
                fuelSupply
        );

        Map<Integer, CarrierJump> route = client.calculateRoute(carrierRouteCriteria);
        routeManager.setFleetCarrierRoute(route);

        int fuelRequired = route.values().stream().mapToInt(CarrierJump::getFuelUsed).sum();
        int numJumps = route.size();

        if (numJumps == 0) {
            return CommandOutcome.speak(localizedEvent("event.carrier.route.navFailed", destination));
        }
        return CommandOutcome.speak(
                localizedEvent("event.carrier.route.calculated",
                        destination,
                        localizedEventPlural(numJumps, "event.carrier.jump.count"),
                        fuelRequired)
                        + " " + localizedEvent("event.carrier.route.nextStep")
        );
    }
}