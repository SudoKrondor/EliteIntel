package elite.intel.gameapi.journal;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.MissionManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.HistoricalMissionScanner;
import elite.intel.gameapi.MissionTitle;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.MissionsEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;
import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static elite.intel.util.StringUtls.localizedSpeech;

public class MissingMissionMonitor implements Runnable, ManagedService {

    /**
     * How long the monitor waits between looks. Long because the only thing that gives it work is a mission
     * snapshot, and the game writes one at game load.
     */
    private static final long SCAN_PAUSE_MS = 10_000;

    private static volatile MissingMissionMonitor instance;
    private final Logger log = LogManager.getLogger(MissingMissionMonitor.class);
    private final MissionManager missionManager = MissionManager.getInstance();
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    /**
     * The game's most recent mission log, and only that one.
     * <p>
     * WHY not every snapshot seen: this used to accumulate them and read the active list out of all of them
     * at once, so a mission that was active at game load and completed an hour later was still "active"
     * according to the first snapshot. The scan then found it missing from the database - because completing
     * it had correctly removed it - and put it back, announcing it as an uncatalogued mission. Each snapshot
     * supersedes the one before it; an older one describes a mission log that no longer exists.
     */
    private volatile MissionsEvent missionLog;
    private ScheduledExecutorService executor;

    private MissingMissionMonitor() {
        GameEventBus.register(this);
    }

    public static MissingMissionMonitor getInstance() {
        if (instance == null) {
            synchronized (MissingMissionMonitor.class) {
                if (instance == null) {
                    instance = new MissingMissionMonitor();
                }
            }
        }
        return instance;
    }

    public synchronized void start() {
        if (executor != null || scanning.get()) {
            log.debug("Monitor already running");
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Missing-Mission-Monitor");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this, 1, 5, TimeUnit.MINUTES);
        scanning.set(true);
        log.info("MissingMissionMonitor started");
    }

    public synchronized void stop() {
        scanning.set(false);
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Monitor did not terminate");
                }
            } catch (InterruptedException e) {
                log.warn("Interrupt while stopping monitor", e);
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        log.info("MissingMissionMonitor stopped");
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            scan();
        }
    }

    private void scan() {
        try {
            // Sleep BEFORE the check, not after it: with the order reversed, an idle monitor returned
            // immediately and run()'s loop called straight back into it, spinning a core flat out between
            // the mission snapshots that are minutes or hours apart.
            //noinspection BusyWait
            Thread.sleep(SCAN_PAUSE_MS);

            if (!scanning.get()) return;

            MissionsEvent snapshot = missionLog;
            List<Long> acceptedMissionIds = activeIds(snapshot);
            if (acceptedMissionIds.isEmpty()) {
                scanning.set(false); //the commander holds nothing; go back to sleep
                return;
            }

            List<Long> existingMissionIds = new ArrayList<>(missionManager.getMissions().keySet());
            Set<Long> filtered = new HashSet<>(acceptedMissionIds);
            existingMissionIds.forEach(filtered::remove);
            HistoricalMissionScanner scanner = HistoricalMissionScanner.getInstance();
            List<MissionAcceptedEvent> missingMissions = scanner.scanForPendingAcceptedEvents(filtered);
            for (MissionAcceptedEvent mission : missingMissions) {
                GameEventBus.publish(new AiVoxResponseEvent(
                        localizedSpeech(
                                "speech.warning.uncataloguedMissionDetected",
                                PlayerSession.getInstance().getVariablePlayerName(),
                                // The title, not the raw key: "Mission_Courier_RankEmp" is not something to
                                // say out loud. MissionTitle prefers the game's own localised name.
                                MissionTitle.of(mission.getName(), mission.getLocalisedName())
                        )
                ));
                missionManager.save(new MissionDto(mission));
            }
            scanning.set(false); //Go back to sleep
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The ids in the snapshot's active list, empty when there is no usable snapshot to read.
     */
    private static List<Long> activeIds(MissionsEvent snapshot) {
        if (snapshot == null || snapshot.getActive() == null) return List.of();
        List<Long> ids = new ArrayList<>();
        for (MissionsEvent.Mission mission : snapshot.getActive()) {
            ids.add(mission.getMissionID());
        }
        return ids;
    }

    @Subscribe public void onMissionEvent(MissionsEvent event) {
        this.missionLog = event;
        scanning.set(true);
    }
}
