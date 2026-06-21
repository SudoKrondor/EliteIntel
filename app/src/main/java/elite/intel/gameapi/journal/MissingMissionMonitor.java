package elite.intel.gameapi.journal;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.MissionManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.HistoricalMissionScanner;
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

public class MissingMissionMonitor implements Runnable, ManagedService {

    private static volatile MissingMissionMonitor instance;
    private final Logger log = LogManager.getLogger(MissingMissionMonitor.class);
    private final MissionManager missionManager = MissionManager.getInstance();
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final List<MissionsEvent> missionEvent = new ArrayList<>();
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
            //noinspection BusyWait
            if (!scanning.get()) return;

            Thread.sleep(10 * 1000);

            List<Long> acceptedMissionIds = new ArrayList<>();
            for (MissionsEvent event : missionEvent) {
                List<MissionsEvent.Mission> activeMissions = event.getActive();
                for (MissionsEvent.Mission mission : activeMissions) {
                    acceptedMissionIds.add(mission.getMissionID());
                }
            }
            if (acceptedMissionIds.isEmpty()) return;

            List<Long> existingMissionIds = new ArrayList<>(missionManager.getMissions().keySet());
            Set<Long> filtered = new HashSet<>(acceptedMissionIds);
            existingMissionIds.forEach(filtered::remove);
            HistoricalMissionScanner scanner = HistoricalMissionScanner.getInstance();
            List<MissionAcceptedEvent> missingMissions = scanner.scanForPendingAcceptedEvents(filtered);
            for (MissionAcceptedEvent mission : missingMissions) {
                GameEventBus.publish(new AiVoxResponseEvent(
                        "%s! i detected a %s mission that i haven't catalogued.".formatted(
                                PlayerSession.getInstance().getVariablePlayerName(),
                                mission.getName()
                        )
                ));
                missionManager.save(new MissionDto(mission));
            }
            scanning.set(false); //Go back to sleep
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Subscribe public void onMissionEvent(MissionsEvent event) {
        this.missionEvent.add(event);
        scanning.set(true);
    }
}
