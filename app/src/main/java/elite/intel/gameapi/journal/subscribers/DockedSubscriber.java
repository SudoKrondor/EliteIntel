package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.PlayerSession;

public class DockedSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onDockedEvent(DockedEvent event) {
        Thread.ofVirtual().start(() -> {
            // The station gets a record of its own. It used to be written onto the record of the body we
            // dropped at - Docked carries no BodyID, so that was the only record to hand - which re-labelled
            // the body as the station standing next to it. See DockedStationRecord.
            DockedStationRecord station = DockedStationRecord.of(event);
            station.store();

            if (station.isFleetCarrier()) {
                rememberCarrierPosition(event);
            }
        });
    }

    /**
     * Standing on a carrier's deck says where a carrier is - but only ours.
     * <p>
     * WHY the callsign check: the galaxy is full of other commanders' carriers, and docking at one used to
     * move OUR carrier onto their system. The station name of a fleet carrier is its callsign, so it says
     * whose deck this is.
     */
    private void rememberCarrierPosition(DockedEvent event) {
        CarrierDataDto carrierData = playerSession.getFleetCarrierData();
        String ourCallSign = carrierData.getCallSign();
        boolean callSignKnown = ourCallSign != null && !ourCallSign.isBlank();
        boolean ourCarrier = callSignKnown && ourCallSign.equalsIgnoreCase(event.getStationName());
        boolean foreignCarrier = callSignKnown && !ourCarrier;

        LocationDao.Coordinates coordinates = LocationManager.getInstance().getGalacticCoordinates();
        if (!foreignCarrier && coordinates != null) {
            carrierData.setX(coordinates.x());
            carrierData.setY(coordinates.y());
            carrierData.setZ(coordinates.z());
            carrierData.setStarName(event.getStarSystem());
            playerSession.setFleetCarrierData(carrierData);
        }

        if (ourCarrier) {
            // Standing on our own carrier's deck is first-hand evidence of where it is.
            // WHY positive proof rather than absence of a mismatch: this field decides
            // which legs the route drops, so a carrier we cannot name must not move it.
            playerSession.setLastKnownCarrierLocation(event.getStarSystem());
        }
    }
}
