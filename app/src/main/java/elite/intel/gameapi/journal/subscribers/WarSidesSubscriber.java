package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.ConflictZoneManager;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.LocationEvent;
import elite.intel.gameapi.signals.ActiveWar;

import java.util.List;

/**
 * Reads the Conflicts block of every arrival into the conflict-zone ledger: who is fighting here,
 * or - just as useful - that nobody is any more.
 * <p>
 * Quiet data acquisition, like the sweep that counts the zones. Nothing is spoken.
 */
@SuppressWarnings("unused")
public class WarSidesSubscriber {

    private final ConflictZoneManager conflictZones;

    public WarSidesSubscriber() {
        this(ConflictZoneManager.getInstance());
    }

    WarSidesSubscriber(ConflictZoneManager conflictZones) {
        this.conflictZones = conflictZones;
    }

    @Subscribe
    public void onFSDJump(FSDJumpEvent event) {
        if (event == null) return;
        Thread.ofVirtual().start(() -> record(event.getStarSystem(), event.getSystemAddress(),
                event.getStarPos(), event.getConflicts(), event.getTimestamp()));
    }

    @Subscribe
    public void onLocation(LocationEvent event) {
        if (event == null) return;
        Thread.ofVirtual().start(() -> record(event.getStarSystem(), event.getSystemAddress(),
                event.getStarPos(), event.getConflicts(), event.getTimestamp()));
    }

    void record(String starSystem, long systemAddress, double[] starPos,
                List<FSDJumpEvent.Conflict> conflicts, String timestamp) {
        if (starSystem == null || starSystem.isBlank()) return;

        ActiveWar war = activeWar(conflicts);
        if (war == null) {
            conflictZones.recordPeace(starSystem, timestamp);
            return;
        }
        Coordinates coordinates = starPos != null && starPos.length >= 3
                ? new Coordinates(starSystem, starPos[0], starPos[1], starPos[2])
                : null;
        conflictZones.recordWar(starSystem, systemAddress, coordinates,
                war.warType(), war.faction1(), war.faction2(), timestamp);
    }

    /**
     * The first war being fought here. A system can host two at once, and then the zones belong
     * to both and one pair of names is as good as the other for telling the commander what to expect.
     */
    private static ActiveWar activeWar(List<FSDJumpEvent.Conflict> conflicts) {
        if (conflicts == null) return null;
        for (FSDJumpEvent.Conflict conflict : conflicts) {
            ActiveWar war = ActiveWar.of(conflict.getWarType(), conflict.getStatus(),
                    conflict.getFaction1() == null ? null : conflict.getFaction1().getName(),
                    conflict.getFaction2() == null ? null : conflict.getFaction2().getName());
            if (war != null) return war;
        }
        return null;
    }
}
