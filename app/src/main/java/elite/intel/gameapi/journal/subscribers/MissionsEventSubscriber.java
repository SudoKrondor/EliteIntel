package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MissionManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.HistoricalMissionScanner;
import elite.intel.gameapi.journal.events.MissionsEvent;
import elite.intel.ui.event.AppLogEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

@SuppressWarnings("unused")
public class MissionsEventSubscriber {

    private final MissionManager missionManager = MissionManager.getInstance();
    private final HistoricalMissionScanner missionScanner = HistoricalMissionScanner.getInstance();

    @Subscribe
    /// NOTE: handled by MissingMissionMonitor
    public void onMissionsEventSubscriber(MissionsEvent event) {
        Thread.ofVirtual().start(() -> {
            if (!event.getActive().isEmpty()) {
                GameEventBus.register(new MissionCriticalAnnouncementEvent(localizedEventPlural(event.getActive().size(), "event.missions.outstanding")));
            }
            if (!event.getComplete().isEmpty() || !event.getFailed().isEmpty()) {
                // Removes old and completed missions from the database.
                Set<Long> existingMissionIds = missionManager.getMissions().keySet();
                Set<Long> completedOrFailedIDs = new HashSet<>();
                event.getComplete().forEach(m -> completedOrFailedIDs.add(m.getMissionID()));
                event.getFailed().forEach(m -> completedOrFailedIDs.add(m.getMissionID()));

                Set<Long> offset = existingMissionIds.stream()
                        .filter(completedOrFailedIDs::contains)
                        .collect(Collectors.toSet());

                if (offset.isEmpty()) return;
                offset.forEach(missionManager::remove);
                UiBus.publish(new AppLogEvent(localizedEvent("event.missions.removedOld", offset.size())));
            }
        });
    }
}
