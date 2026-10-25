package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Position;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.PRIMARY_STAR;

/**
 * Records a hyperspace arrival: the star the ship arrives at, and the commander's new position. Shared by the live
 * subscriber and the startup pre-scan, which kept two copies of this that had drifted - the live one looked the star
 * up by BodyID, outside the body lock, and could save over a Scan that landed in between.
 * <p>
 * A ship's jump always arrives at the system's entry point, so the arrival body is the primary star whatever its
 * name or BodyID: across 2568 jumps the arrival star's BodyID was 0 only 56% of the time, and its name was not the
 * system's in 40%.
 */
final class FsdJumpArrival {

    private FsdJumpArrival() {
    }

    /**
     * Files the arrival star under its journal name, in one locked write, then moves the commander there - to the
     * body id the record holds, which {@link LocationDto#setBodyId} may have kept over the event's.
     */
    static void record(FSDJumpEvent event) {
        LocationDto star = LocationManager.getInstance().updateNamedBody(
                event.getSystemAddress(), event.getBodyId(), event.getBody(), arrival -> describe(arrival, event));
        PlayerSession.getInstance().moveTo(new Position(event.getSystemAddress(), event.getStarSystem(), star.getBodyId()));
    }

    static void describe(LocationDto star, FSDJumpEvent event) {
        star.setBodyId(event.getBodyId());
        star.setSystemAddress(event.getSystemAddress());
        star.setStationGovernment(event.getSystemGovernmentLocalised());
        star.setAllegiance(event.getSystemAllegiance());
        star.setSecurity(event.getSystemSecurityLocalised());
        if (event.getSystemFaction() != null) star.setSystemFaction(event.getSystemFaction().getName());
        star.setStarName(event.getStarSystem());
        star.setPlanetName(event.getBody());
        star.setLocationType(PRIMARY_STAR);
        star.setX(event.getStarPos()[0]);
        star.setY(event.getStarPos()[1]);
        star.setZ(event.getStarPos()[2]);
        star.setPopulation(event.getPopulation());
        star.setPowerplayState(event.getPowerplayState());
        star.setPowerplayStateControlProgress(event.getPowerplayStateControlProgress());
        star.setPowerplayStateReinforcement(event.getPowerplayStateReinforcement());
        star.setPowerplayStateUndermining(event.getPowerplayStateUndermining());
    }
}
