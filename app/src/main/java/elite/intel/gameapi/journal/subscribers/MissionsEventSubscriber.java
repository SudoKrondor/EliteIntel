package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MissionManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.MissionsEvent;
import elite.intel.gameapi.missions.MissionReconciliation;
import elite.intel.ui.event.AppLogEvent;

import java.util.Set;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * Keeps our mission list honest against the game's own.
 *
 * <p>The {@code Missions} event is the whole mission log, so what it does not list, the commander does not
 * have. See {@link MissionReconciliation} for why that is the test rather than the {@code Complete} and
 * {@code Failed} lists this used to read.
 *
 * <p>The other direction - a mission the game holds and we have never seen - belongs to
 * {@code MissingMissionMonitor}, which digs the accept event out of the journal history. Nothing is spoken
 * here either; the outstanding-missions announcement is that monitor's job.
 */
@SuppressWarnings("unused")
public class MissionsEventSubscriber {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe
    public void onMissionsEventSubscriber(MissionsEvent event) {
        Thread.ofVirtual().start(() -> {
            Set<Long> stale = MissionReconciliation.stale(missionManager.getMissions(), event);
            if (stale.isEmpty()) return;
            stale.forEach(missionManager::remove);
            UiBus.publish(new AppLogEvent(localizedEvent("event.missions.removedOld", stale.size())));
        });
    }
}
