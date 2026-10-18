package elite.intel.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MissionReconciliation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Rebuilds the commander's mission list at startup from what the journal says happened while the app was closed.
 * <p>
 * WHY: the live {@code JournalParser} skips everything written before app start ({@link BaseEvent#isReplay()}), and
 * the game lists the missions held only once, in the {@code Missions} event at game load. A mission accepted after
 * that load but before the app started was therefore never stored by anyone: not live, not by
 * {@code MissingMissionMonitor} (which only hears a live snapshot), and not here. Seen in a support bundle - two
 * passenger missions taken twenty minutes before launch never reached the HUD or VEGA.
 * <p>
 * The newest {@code Missions} snapshot is the anchor: it is the game's own list of what was held at that moment.
 * After it, an accept adds a mission and a completion, failure or abandonment ends it, so a mission accepted and
 * then handed in before launch is not brought back. Every ended mission is also removed from storage, and anything
 * stored that the snapshot does not hold is dropped, by the same rule the live {@code MissionsEventSubscriber} uses.
 * <p>
 * Only replayed events count: anything at or after app start belongs to the live mission subscribers, so app start
 * is the handoff and nothing is handled twice. Silent, like every pre-scan: nothing is announced.
 * <p>
 * A mission held in the snapshot whose accept is older than the journals scanned has no details to store from; it
 * stays as it is in the database, and {@code MissingMissionMonitor} recovers it at the next live game load.
 */
public class MissionsPreScanAccumulator {

    private static final Logger log = LogManager.getLogger(MissionsPreScanAccumulator.class);

    private MissionsEvent snapshot;
    /**
     * Every replayed accept, by mission id, whether before or after the snapshot: the snapshot names what is held
     * but carries none of the details, and an accept from the previous session is where those come from.
     */
    private final Map<Long, MissionAcceptedEvent> accepts = new LinkedHashMap<>();
    /**
     * Accepted after the newest snapshot - or anywhere, when the journals hold no snapshot at all.
     */
    private final Set<Long> acceptedSinceSnapshot = new LinkedHashSet<>();
    /**
     * Completed, failed or abandoned after the newest snapshot - or anywhere, when there is none.
     */
    private final Set<Long> endedSinceSnapshot = new HashSet<>();
    /**
     * The latest replayed redirect per mission: it changes where the mission is handed in, and for a kill mission
     * it is the objectives-complete signal.
     */
    private final Map<Long, MissionRedirectedEvent> redirects = new HashMap<>();

    @Subscribe
    public void onMissions(MissionsEvent e) {
        if (!e.isReplay()) return;
        // A newer snapshot supersedes everything learned before it about what is held.
        snapshot = e;
        acceptedSinceSnapshot.clear();
        endedSinceSnapshot.clear();
    }

    @Subscribe
    public void onMissionAccepted(MissionAcceptedEvent e) {
        if (!e.isReplay()) return;
        accepts.put(e.getMissionID(), e);
        acceptedSinceSnapshot.add(e.getMissionID());
        endedSinceSnapshot.remove(e.getMissionID());
    }

    @Subscribe
    public void onMissionCompleted(MissionCompletedEvent e) {
        if (e.isReplay()) ended(e.getMissionID());
    }

    @Subscribe
    public void onMissionFailed(MissionFailedEvent e) {
        if (e.isReplay()) ended(e.getMissionID());
    }

    @Subscribe
    public void onMissionAbandoned(MissionAbandonedEvent e) {
        if (e.isReplay()) ended(e.getMissionID());
    }

    @Subscribe
    public void onMissionRedirected(MissionRedirectedEvent e) {
        if (e.isReplay() && e.getMissionID() != null) redirects.put(e.getMissionID(), e);
    }

    private void ended(long missionId) {
        acceptedSinceSnapshot.remove(missionId);
        endedSinceSnapshot.add(missionId);
    }

    /**
     * The missions held at app start, as far as these journals can tell: the snapshot's list, plus everything
     * accepted since, less everything that ended since.
     */
    Set<Long> heldIds() {
        Set<Long> held = new LinkedHashSet<>();
        if (snapshot != null && snapshot.getActive() != null) {
            snapshot.getActive().forEach(mission -> held.add(mission.getMissionID()));
        }
        held.addAll(acceptedSinceSnapshot);
        held.removeAll(endedSinceSnapshot);
        return held;
    }

    public void persist() {
        MissionManager missions = MissionManager.getInstance();

        endedSinceSnapshot.forEach(missions::remove);
        if (snapshot != null) {
            MissionReconciliation.stale(missions.getMissions(), snapshot).forEach(missions::remove);
        }

        Set<Long> stored = missions.getMissions().keySet();
        int restored = 0;
        for (Long id : heldIds()) {
            MissionDto mission = stored.contains(id) ? missions.getMission(id) : fromAccept(id);
            if (mission == null) continue;
            boolean redirected = applyRedirect(mission);
            if (!stored.contains(id)) restored++;
            if (!stored.contains(id) || redirected) missions.save(mission);
        }
        log.info("MissionsPreScan: restored {} mission(s) accepted while the app was closed, removed {} that ended",
                restored, endedSinceSnapshot.size());
    }

    private MissionDto fromAccept(long id) {
        MissionAcceptedEvent accept = accepts.get(id);
        return accept == null ? null : new MissionDto(accept);
    }

    /**
     * Applies a replayed redirect the way {@code MissionRedirectedSubscriber} does live.
     *
     * @return whether the mission changed
     */
    private boolean applyRedirect(MissionDto mission) {
        MissionRedirectedEvent redirect = redirects.get(mission.getMissionId());
        if (redirect == null || Objects.equals(redirect.getTimestamp(), mission.getRedirectedAt())) return false;
        String station = redirect.getNewDestinationStation();
        String system = redirect.getNewDestinationSystem();
        if (station != null && !station.isEmpty()) mission.setDestinationStation(station);
        if (system != null && !system.isEmpty()) mission.setDestinationSystem(system);
        mission.setRedirectedAt(redirect.getTimestamp());
        return true;
    }
}
