package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.PowerConflictZoneManager;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.LocationEvent;

import java.util.List;

/**
 * Reads the powers present in every system the commander arrives in into the power conflict zone
 * ledger, so a sweep that finds the zones has the powers to join - or, when no power is there any
 * more, says the zones are gone.
 * <p>
 * The powerplay twin of {@link WarSidesSubscriber}. Quiet data acquisition: nothing is spoken.
 */
@SuppressWarnings("unused")
public class PowersPresentSubscriber {

    private final PowerConflictZoneManager powerConflictZones;

    public PowersPresentSubscriber() {
        this(PowerConflictZoneManager.getInstance());
    }

    PowersPresentSubscriber(PowerConflictZoneManager powerConflictZones) {
        this.powerConflictZones = powerConflictZones;
    }

    @Subscribe
    public void onFSDJump(FSDJumpEvent event) {
        if (event == null) return;
        Thread.ofVirtual().start(() -> record(event.getStarSystem(), event.getSystemAddress(), event.getStarPos(),
                event.getControllingPower(), event.getPowers(), event.getPowerplayState(), event.getTimestamp()));
    }

    @Subscribe
    public void onLocation(LocationEvent event) {
        if (event == null) return;
        Thread.ofVirtual().start(() -> record(event.getStarSystem(), event.getSystemAddress(), event.getStarPos(),
                event.getControllingPower(), event.getPowers(), event.getPowerplayState(), event.getTimestamp()));
    }

    void record(String starSystem, long systemAddress, double[] starPos, String controllingPower,
                List<String> powers, String powerplayState, String timestamp) {
        if (starSystem == null || starSystem.isBlank() || systemAddress <= 0) return;
        Coordinates coordinates = starPos != null && starPos.length >= 3
                ? new Coordinates(starSystem, starPos[0], starPos[1], starPos[2])
                : null;
        powerConflictZones.recordArrival(starSystem, systemAddress, coordinates, controllingPower, powers,
                powerplayState, timestamp, true);
    }
}
