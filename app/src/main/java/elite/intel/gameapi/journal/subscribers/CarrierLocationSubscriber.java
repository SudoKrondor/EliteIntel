package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.SquadronCarrierRouteManager;
import elite.intel.gameapi.journal.events.CarrierLocationEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;

/**
 * CarrierLocation is written for every carrier arrival, whether or not the commander is aboard, and
 * always ahead of the CarrierJump event. It is therefore the first to reach {@link CarrierArrival},
 * which owns the arrival bookkeeping the two events share. CarrierJumpCompleteSubscriber adds only
 * the aboard-specific work.
 */
public class CarrierLocationSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onCarrierLocationEvent(CarrierLocationEvent event) {
        Thread.ofVirtual().start(() -> {
            if ("FleetCarrier".equalsIgnoreCase(event.getCarrierType())) {
                CarrierArrival.recordFleetArrival(event.getStarSystem(), event.getSystemAddress());
            } else if ("SquadronCarrier".equalsIgnoreCase(event.getCarrierType())) {
                onSquadronCarrierArrived(event);
            }
        });
    }

    /**
     * Records where the squadron carrier has arrived, ON its existing record.
     * <p>
     * It used to build a fresh {@link CarrierDataDto} here, which quietly threw away everything the record
     * held that an arrival says nothing about - the callsign, the cargo ledger, the spare tritium the
     * commander had told us about. An arrival is news about WHERE the carrier is, and must not be read as
     * news about what is aboard it.
     */
    private void onSquadronCarrierArrived(CarrierLocationEvent event) {
        String starSystem = event.getStarSystem();
        SquadronCarrierRouteManager route = SquadronCarrierRouteManager.getInstance();

        CarrierDataDto carrierData = playerSession.getSquadronCarrierData();
        if (carrierData == null) carrierData = new CarrierDataDto();
        carrierData.setStarName(starSystem);
        carrierData.setSystemAddress(event.getSystemAddress());

        CarrierJump completedLeg = route.findByPrimaryStar(starSystem);
        if (completedLeg == null) {
            CarrierArrival.resolveCoordinates(carrierData, starSystem, event.getSystemAddress());
        } else {
            carrierData.setX(completedLeg.getX());
            carrierData.setY(completedLeg.getY());
            carrierData.setZ(completedLeg.getZ());
        }
        playerSession.setSquadronCarrierData(carrierData);

        route.removeLeg(starSystem);
    }
}
