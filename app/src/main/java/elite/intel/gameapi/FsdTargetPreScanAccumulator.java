package elite.intel.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.gameapi.journal.events.FSDTargetEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;

/**
 * Restores the FSD target at startup to the last one the journal names.
 * <p>
 * WHY: the live {@code JournalParser} skips everything written before app start ({@link BaseEvent#isReplay()}), so a
 * target the game selected while the app was closed never replaced the one stored by the previous run. Seen in a
 * support bundle: two jumps flown with the app closed, and "what is my target" and the jump announcement still named
 * a star two jumps back.
 * <p>
 * Only replayed events count: anything at or after app start belongs to the live {@code FSDTargetSubscriber}. The
 * restored target carries no EDSM system, traffic or death figures - a pre-scan makes no network calls - and the next
 * live {@code FSDTarget} replaces it whole. Silent, like every pre-scan.
 */
public class FsdTargetPreScanAccumulator {

    private FSDTargetEvent latest;

    @Subscribe
    public void onFsdTarget(FSDTargetEvent e) {
        if (e.isReplay()) latest = e;
    }

    /**
     * Stores the last replayed target; leaves the stored one alone when the journals name none.
     */
    public void persist() {
        if (latest == null || latest.getName() == null || latest.getName().isBlank()) return;
        LocationDto location = LocationManager.getInstance().findPrimaryStar(latest.getSystemAddress());
        location.setStarName(latest.getName());
        location.setStarClass(latest.getStarClass());
        PlayerSession.getInstance().setFsdTarget(new FsdTarget(latest.getName(), latest.getStarClass(), location,
                null, null, null, FuelScoop.starStatus(latest.getStarClass())));
    }
}
