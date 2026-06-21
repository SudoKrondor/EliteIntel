package elite.intel.db.managers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.SubSystemDao;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.ShipTargetedEvent;
import elite.intel.util.AudioPlayer;
import elite.intel.util.SleepNoThrow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_CYCLE_NEXT_SUBSYSTEM;

public class SubSystemsManager {

    private static final Logger log = LogManager.getLogger(SubSystemsManager.class);
    private static volatile SubSystemsManager instance;

    private static final int PAUSE_TIMEOUT_MS = 1500;

    private String target;
    private volatile boolean continueTargeting = false;
    private volatile boolean pause = false;
    private volatile int consecutiveTimeouts = 0;
    private volatile Instant lastKeyPressInstant = Instant.EPOCH;

    private SubSystemsManager() {
        GameEventBus.register(this);
    }

    public static SubSystemsManager getInstance() {
        if (instance == null) {
            synchronized (SubSystemsManager.class) {
                if (instance == null) {
                    instance = new SubSystemsManager();
                }
            }
        }
        return instance;
    }

    private String getTarget() {
        return target;
    }

    private void setTarget(String target) {
        this.target = target;
    }

    /**
     * Initiates the targeting process for a specified subsystem within a game. The method
     * uses fuzzy search to resolve the subsystem name and attempts to cycle through subsystems
     * using game inputs until the target is confirmed or cycling conditions are exhausted.
     *
     * @param subsystem the name of the subsystem to target. This can be a partial or full
     *                  name which is resolved using a fuzzy search algorithm. If a match
     *                  is not found, the method terminates the process.
     */
    public void targetSubSystem(String subsystem) {
        log.debug("[1] targetSubSystem raw input: [{}]", subsystem);
        pause = false;
        String resolved = FuzzySearch.fuzzySubSystemSearch(subsystem, 4);
        log.debug("[2] fuzzy resolved: [{}]", resolved);
        setTarget(resolved);
        continueTargeting = getTarget() != null && !getTarget().isEmpty();

        if (!continueTargeting) {
            log.debug("[3] no fuzzy match - cycling will NOT start");
            return;
        }
        log.debug("[3] target set to [{}] - cycling starting", getTarget());

        consecutiveTimeouts = 0;
        new Thread(() -> {
            int cycleCount = 0;
            boolean usingFallback = false;
            while (continueTargeting) {
                if (!pause) {
                    if (!continueTargeting) break;
                    if (cycleCount >= 25) {
                        if (usingFallback) {
                            log.debug("[cycle] exhausted fallback - giving up");
                            continueTargeting = false;
                            break;
                        }
                        log.debug("[cycle] limit reached - falling back to Power Plant");
                        setTarget("Power Plant");
                        usingFallback = true;
                        cycleCount = 0;
                    }
                    lastKeyPressInstant = Instant.now();
                    log.debug("[cycle] count={} pressing key, target=[{}]", cycleCount, getTarget());
                    GameControllerBus.publish(GameInputSequenceEvent.of(
                            GameInputStep.bindingHold(BINDING_CYCLE_NEXT_SUBSYSTEM.getGameBinding(), 50),
                            GameInputStep.delay(250)
                    ));
                    pause = true;
                    cycleCount++;
                } else if (Instant.now().isAfter(lastKeyPressInstant.plusMillis(PAUSE_TIMEOUT_MS))) {
                    consecutiveTimeouts++;
                    log.debug("[cycle] journal timeout #{} after {}ms - no ShipTargeted event received", consecutiveTimeouts, PAUSE_TIMEOUT_MS);
                    if (consecutiveTimeouts >= 3) {
                        log.debug("[cycle] 3 consecutive timeouts - assuming target lost, stopping");
                        continueTargeting = false;
                        break;
                    }
                    pause = false;
                }
                SleepNoThrow.sleep(10);
            }
        }, "SubSystemTargeting-Thread").start();
    }

    /**
     * Handles the event triggered when a ship is targeted. This method processes
     * the event to determine if the targeted ship's subsystem matches a specific
     * target, and acts accordingly by updating targeting state and playing audio cues.
     *
     * @param event the ShipTargetedEvent containing details about the targeting action.
     *              If the event is null, the method returns immediately.
     *              Key data from the event includes:
     *              - Target lock status (targetLocked)
     *              - Localized and raw subsystem names (subsystemLocalised, subsystem)
     *              - Event timestamp for filtering stale events
     *              - Scan stage indicating the progression of targeting
     */
    @Subscribe public void onShipTargetedEvent(ShipTargetedEvent event) {
        if (event == null) return;

        if (!event.isTargetLocked()) {
            log.debug("[journal] target lock lost - stopping");
            continueTargeting = false;
            return;
        }

        String effectiveName = resolveSubsystemName(event.getSubsystemLocalised(), event.getSubsystem());
        if (effectiveName == null) {
            log.debug("[journal] ShipTargeted: no resolvable subsystem (scanStage={}, raw=[{}])",
                    event.getScanStage(), event.getSubsystem());
            return;
        }

        Instant eventInstant = Instant.parse(event.getTimestamp());
        Instant keyPressFloor = lastKeyPressInstant.truncatedTo(ChronoUnit.SECONDS);
        if (eventInstant.isBefore(keyPressFloor)) {
            log.debug("[journal] stale event filtered: eventTime=[{}] keyPressFloor=[{}]", eventInstant, keyPressFloor);
            return;
        }

        String trimmed = effectiveName.trim();
        log.debug("[journal] resolved=[{}] target=[{}]", trimmed, getTarget());

        if (trimmed.equalsIgnoreCase(getTarget())) {
            log.debug("[journal] MATCH - stopping");
            continueTargeting = false;
            AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_1);
        } else {
            log.debug("[journal] no match - continuing");
            AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_2);
        }
        consecutiveTimeouts = 0;
        pause = false;
    }


    /**
     * Resolves the name of a subsystem by prioritizing a localized name if provided,
     * or falling back to querying a database using a processed version of the raw key.
     *
     * @param localised the localized subsystem name, which may be null or blank
     * @param rawKey the raw identifier for the subsystem, which may be null or blank
     * @return the resolved subsystem name as a String, or null if the resolution fails
     */
    private static String resolveSubsystemName(String localised, String rawKey) {
        if (localised != null && !localised.isBlank()) return localised;
        if (rawKey == null || rawKey.isBlank()) return null;

        String stripped = rawKey.replaceAll("^\\$", "")
                .replaceAll("_name;?$", "")
                .replaceAll(";$", "");
        return Database.withDao(SubSystemDao.class, dao -> dao.findSubsystemByRawKey(stripped));
    }
}
