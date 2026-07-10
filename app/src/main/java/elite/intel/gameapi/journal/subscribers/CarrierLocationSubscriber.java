package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.SquadronCarrierRouteManager;
import elite.intel.gameapi.journal.events.CarrierLocationEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.StarSystemDto;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import elite.intel.util.ClipboardUtils;
import elite.intel.util.FleetCarrierRouteCalculator;

/**
 * CarrierLocation is written for every carrier arrival, whether or not the commander is aboard, and
 * always ahead of the CarrierJump event. It is therefore the single home for arrival bookkeeping:
 * fuel, route legs and re-plotting. CarrierJumpCompleteSubscriber adds only the aboard-specific work.
 */
public class CarrierLocationSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onCarrierLocationEvent(CarrierLocationEvent event) {
        Thread.ofVirtual().start(() -> {
            if ("FleetCarrier".equalsIgnoreCase(event.getCarrierType())) {
                onFleetCarrierArrived(event);
            } else if ("SquadronCarrier".equalsIgnoreCase(event.getCarrierType())) {
                onSquadronCarrierArrived(event);
            }
        });
    }

    private void onFleetCarrierArrived(CarrierLocationEvent event) {
        String starSystem = event.getStarSystem();
        playerSession.setLastKnownCarrierLocation(starSystem);

        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        CarrierJump completedLeg = route.findByPrimaryStar(starSystem);
        boolean routePlotted = !route.getFleetCarrierRoute().isEmpty();

        CarrierDataDto carrierData = playerSession.getFleetCarrierData();
        carrierData.setStarName(starSystem);
        carrierData.setSystemAddress(event.getSystemAddress());

        if (completedLeg == null) {
            resolveCoordinates(carrierData, starSystem);
        } else {
            carrierData.setX(completedLeg.getX());
            carrierData.setY(completedLeg.getY());
            carrierData.setZ(completedLeg.getZ());
            // WHY: the leg carries the tritium this hop burned, so it must be read before removeLeg.
            // Nothing else decrements carrier fuel on arrival.
            carrierData.setFuelLevel(carrierData.getFuelLevel() - completedLeg.getFuelUsed());
        }
        playerSession.setFleetCarrierData(carrierData);
        playerSession.setCarrierDepartureTime(null);

        // WHY: the system we are sitting in is never part of the remaining route.
        route.removeLeg(starSystem);

        // WHY: arriving somewhere that was not a plotted leg means the route no longer starts where
        // we are, so it has to be re-plotted from here. An on-route arrival needs no Spansh call.
        if (completedLeg == null && routePlotted) {
            replotFromCurrentPosition(route.getFinalDestination());
        }
    }

    private void onSquadronCarrierArrived(CarrierLocationEvent event) {
        String starSystem = event.getStarSystem();
        SquadronCarrierRouteManager route = SquadronCarrierRouteManager.getInstance();

        CarrierDataDto carrierData = new CarrierDataDto();
        carrierData.setStarName(starSystem);
        carrierData.setSystemAddress(event.getSystemAddress());

        CarrierJump completedLeg = route.findByPrimaryStar(starSystem);
        if (completedLeg == null) {
            resolveCoordinates(carrierData, starSystem);
        } else {
            carrierData.setX(completedLeg.getX());
            carrierData.setY(completedLeg.getY());
            carrierData.setZ(completedLeg.getZ());
        }
        playerSession.setSquadronCarrierData(carrierData);

        route.removeLeg(starSystem);
    }

    private void replotFromCurrentPosition(String finalDestination) {
        if (finalDestination == null || finalDestination.isBlank()) return;
        // The calculator reads its destination from the clipboard.
        ClipboardUtils.setClipboardText(finalDestination);
        FleetCarrierRouteCalculator.calculate();
    }

    /**
     * The arrival system was not on the plotted route, so its coordinates are unknown. Ask EDSM, and
     * fall back to whatever the local location table already holds.
     */
    private void resolveCoordinates(CarrierDataDto carrierData, String starSystem) {
        StarSystemDto starSystemDto = EdsmApiClient.searchStarSystem(starSystem, 1);
        StarSystemDto.Coords coords = starSystemDto.getCoords();
        boolean isSol = starSystemDto.getData() != null
                && "sol".equalsIgnoreCase(starSystemDto.getData().getName());
        boolean hasValidCoords = coords != null
                && (isSol || coords.getX() != 0 || coords.getY() != 0 || coords.getZ() != 0);

        if (hasValidCoords) {
            carrierData.setX(coords.getX());
            carrierData.setY(coords.getY());
            carrierData.setZ(coords.getZ());
            return;
        }

        LocationDto location = LocationManager.getInstance().findPrimaryStar(starSystem);
        carrierData.setX(location.getX());
        carrierData.setY(location.getY());
        carrierData.setZ(location.getZ());
    }
}
