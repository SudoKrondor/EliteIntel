package elite.intel.util;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.eventbus.GameEventBus;
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

    public static void calculate() {

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

        GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.carrier.route.accessing")));

        if(carrierData.getX() == 0 && carrierData.getY() == 0 && carrierData.getZ() == 0) {
            GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.carrier.route.locationUnavailable")));
            return;
        }

        LocationDto nearestStartingPoint = NearestKnownLocationSearchClient.findNearest(carrierData.getX(), carrierData.getY(), carrierData.getZ());

        if (destination == null || nearestStartingPoint == null) {
            GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.carrier.route.noDestination")));
            return;
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
            GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.carrier.route.navFailed", destination)));
        } else {
            GameEventBus.publish(new AiVoxResponseEvent(
                    localizedEvent("event.carrier.route.calculated",
                            destination,
                            localizedEventPlural(numJumps, "event.carrier.jump.count"),
                            fuelRequired)
                            + " " + localizedEvent("event.carrier.route.nextStep")
            ));
        }
    }
}