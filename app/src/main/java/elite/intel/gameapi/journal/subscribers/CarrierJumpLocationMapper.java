package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;

/**
 * Builds the arrival-body row for a carrier jump. Shared by the live subscriber and the startup
 * pre-scan so both persist the same shape: previously the two drifted, and a system recorded while
 * the app was running carried different fields than the same system recovered from the journal.
 */
final class CarrierJumpLocationMapper {

    private CarrierJumpLocationMapper() {
    }

    /**
     * Fetches the existing row for the carrier's arrival body and populates it from the event.
     * The caller is responsible for saving it.
     */
    static LocationDto toArrivalLocation(CarrierJumpEvent event, LocationManager locationManager) {
        // WHY: fetch by the unique locationName that LocationDao.upsert conflicts on (fed from
        // planetName), not by bodyId. A bodyId miss builds a sparse row that the upsert then writes
        // over the top of the real one, destroying its scan data. LocationSubscriber keys the same way.
        LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBody());

        location.setStarName(event.getStarSystem());
        // WHY: without systemAddress the row is unreachable. The current-location pointer stores the
        // real address and every lookup joins on it, so a row saved with the default 0 is orphaned.
        location.setSystemAddress(event.getSystemAddress());
        location.setBodyId(event.getBodyId());
        location.setPlanetName(event.getBody());
        location.setX(event.getStarPos()[0]);
        location.setY(event.getStarPos()[1]);
        location.setZ(event.getStarPos()[2]);
        location.setAllegiance(event.getSystemAllegiance());
        location.setStationGovernment(event.getSystemGovernmentLocalised());
        location.setSecurity(event.getSystemSecurityLocalised());
        location.setControllingPower(event.getControllingPower());
        if (event.getSystemFaction() != null) location.setSystemFaction(event.getSystemFaction().getName());
        location.setPopulation(event.getPopulation());
        location.setPowerplayState(event.getPowerplayState());
        location.setPowerplayStateControlProgress(event.getPowerplayStateControlProgress());
        location.setPowerplayStateReinforcement(event.getPowerplayStateReinforcement());
        location.setPowerplayStateUndermining(event.getPowerplayStateUndermining());

        if (event.getBodyType() != null) {
            location.setBodyType(event.getBodyType());
            // WHY: a carrier always arrives at the system's primary star, hence isPrimaryStar=true.
            // A body type determineType does not recognise yields null; keep the existing type then.
            LocationDto.LocationType type = LocationDto.determineType(event.getBodyType(), true);
            if (type != null) location.setLocationType(type);
        }
        return location;
    }
}
