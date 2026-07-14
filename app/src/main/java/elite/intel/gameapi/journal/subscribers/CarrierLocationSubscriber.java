package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CarrierRouteLegs;
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
 *
 * <p>The game also writes it at every LoadGame, where the carrier has not moved at all. Such a
 * position report must cost no tritium, cancel no scheduled jump and plot no route; it only confirms
 * where the carrier is. Everything here therefore turns on whether the reported system differs from
 * the one already on file, never on where the commander happens to be standing.
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

        // WHY read before writing: the game emits CarrierLocation at every LoadGame, not only on
        // arrival, so the event alone cannot say whether the carrier moved. Comparing it against the
        // system we already believed is the only way to tell an arrival from a position report.
        String previousSystem = playerSession.getCurrentFleetCarrierSystem();
        boolean carrierMoved = !CarrierRouteLegs.isSameSystem(previousSystem, starSystem);

        playerSession.setLastKnownCarrierLocation(starSystem);

        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        CarrierJump completedLeg = route.findByPrimaryStar(starSystem);
        boolean routePlotted = !route.getFleetCarrierRoute().isEmpty();

        CarrierDataDto carrierData = playerSession.getFleetCarrierData();
        carrierData.setStarName(starSystem);
        carrierData.setSystemAddress(event.getSystemAddress());

        if (completedLeg != null) {
            carrierData.setX(completedLeg.getX());
            carrierData.setY(completedLeg.getY());
            carrierData.setZ(completedLeg.getZ());
            if (carrierMoved) {
                // WHY: the leg carries the tritium this hop burned, so it must be read before
                // removeLeg. Nothing else decrements carrier fuel on arrival, and charging it again
                // for a system the carrier was already sitting in would burn tritium for no jump.
                carrierData.setFuelLevel(carrierData.getFuelLevel() - completedLeg.getFuelUsed());
            }
        } else if (carrierMoved || coordinatesUnknown(carrierData)) {
            // Off-route arrival: the coordinates on file belong to the system the carrier left, so
            // they have to be resolved afresh. A position report for the system we are already in
            // reaches for the network only when we never learned its coordinates at all.
            resolveCoordinates(carrierData, starSystem);
        }
        playerSession.setFleetCarrierData(carrierData);

        // WHY: the system we are sitting in is never part of the remaining route. Unconditional, so
        // that a position report also repairs a route left stale by a jump made while we were down.
        route.removeLeg(starSystem);

        if (!carrierMoved) {
            // WHY stop here: a pending departure is still pending, and a route that already starts
            // where the carrier is needs no Spansh call. Clearing the timer and re-plotting on every
            // LoadGame would forget a scheduled jump and overwrite the clipboard behind the
            // commander's back.
            return;
        }

        playerSession.setCarrierDepartureTime(null);

        // WHY: arriving somewhere that was not a plotted leg means the route no longer starts where
        // we are, so it has to be re-plotted from here. An on-route arrival needs no Spansh call.
        if (completedLeg == null && routePlotted) {
            replotFromCurrentPosition(route.getFinalDestination());
        }
    }

    private static boolean coordinatesUnknown(CarrierDataDto carrierData) {
        return carrierData.getX() == 0 && carrierData.getY() == 0 && carrierData.getZ() == 0;
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
