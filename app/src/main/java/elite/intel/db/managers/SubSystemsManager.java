package elite.intel.db.managers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.SubSystemDao;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.i18n.EventsTextProvider;
import elite.intel.gameapi.journal.events.ShipTargetedEvent;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;
import elite.intel.util.SleepNoThrow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_CYCLE_NEXT_SUBSYSTEM;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_CYCLE_PREVIOUS_SUBSYSTEM;

public class SubSystemsManager {

    private static final Logger log = LogManager.getLogger(SubSystemsManager.class);
    private static volatile SubSystemsManager instance;

    private static final int PAUSE_TIMEOUT_MS = 1500;

    /**
     * Hard backstop on key presses. Full-loop detection (returning to the first subsystem we
     * saw) is the authoritative "module not installed" signal and fires well before this; the
     * cap only guards against a pathological state where the anchor is never re-observed.
     */
    private static final int MAX_PRESSES = 40;

    /**
     * Machine-key prefixes for modules that cluster at the bottom of the subsystem cycle
     * (core internals plus the drive). Targeting these is fastest by cycling <em>previous</em>
     * (approaching from the bottom) instead of forward through the whole list.
     */
    private static final Set<String> BOTTOM_CLUSTER_PREFIXES = Set.of(
            "int_powerplant",
            "int_hyperdrive",
            "int_lifesupport",
            "int_powerdistributor",
            "int_shieldgenerator",
            "int_shieldcellbank",
            "int_refinery",
            "ext_drive"
    );

    private volatile String target;               // canonical name, used for spoken announcements
    private volatile String targetMachineKey;     // reliable journal key, used for matching
    private volatile Bindings.GameCommand cycleBinding = BINDING_CYCLE_NEXT_SUBSYSTEM;
    private volatile boolean continueTargeting = false;
    private volatile boolean pause = false;
    private volatile int consecutiveTimeouts = 0;
    private volatile Instant lastKeyPressInstant = Instant.EPOCH;

    // Full-loop ("module not installed") detection state. The cycle wraps with no wall, so a
    // module that isn't fitted is only knowable by returning to the first subsystem we observed.
    private volatile String anchorRaw;
    private volatile boolean leftAnchor;

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
     * uses fuzzy search to resolve the subsystem name, looks up its reliable journal machine
     * key, chooses a cycle direction from where that module type sits in the cycle, and presses
     * the cycle key repeatedly until the target is matched, the cycle has gone fully around
     * (module not installed), or the routine times out.
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
        if (resolved == null || resolved.isEmpty()) {
            log.debug("[3] no fuzzy match - cycling will NOT start");
            continueTargeting = false;
            return;
        }

        String machineKey = Database.withDao(SubSystemDao.class, dao -> dao.getMachineKeyBySubsystem(resolved));
        if (machineKey == null || machineKey.isBlank()) {
            // Without a machine key we cannot match reliably against the journal (the localised
            // name is missing on some ships), so we do not start a cycle we can't terminate.
            log.debug("[3] no machine_key for [{}] - cycling will NOT start", resolved);
            continueTargeting = false;
            return;
        }

        setTarget(resolved);
        targetMachineKey = machineKey.toLowerCase(Locale.ROOT);
        cycleBinding = directionFor(targetMachineKey);
        anchorRaw = null;
        leftAnchor = false;
        consecutiveTimeouts = 0;
        continueTargeting = true;
        log.debug("[3] target=[{}] machineKey=[{}] direction=[{}] - cycling starting",
                resolved, targetMachineKey, cycleBinding);

        new Thread(() -> {
            int pressCount = 0;
            while (continueTargeting) {
                if (!pause) {
                    if (!continueTargeting) break;
                    if (pressCount >= MAX_PRESSES) {
                        log.debug("[cycle] safety cap ({}) reached - stopping", MAX_PRESSES);
                        continueTargeting = false;
                        break;
                    }
                    lastKeyPressInstant = Instant.now();
                    log.debug("[cycle] press #{} target=[{}] key=[{}]", pressCount, getTarget(), cycleBinding);
                    GameControllerBus.publish(GameInputSequenceEvent.of(
                            GameInputStep.bindingHold(cycleBinding.getGameBinding(), 50),
                            GameInputStep.delay(250)
                    ));
                    pause = true;
                    pressCount++;
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
     * Handles the event triggered when a ship is targeted. Matching is done purely on the
     * journal's raw {@code Subsystem} machine key, because {@code Subsystem_Localised} is
     * missing on some ships (a known journal defect). The same machine key also drives
     * full-loop detection so an un-fitted module can be reported to the commander.
     *
     * @param event the ShipTargetedEvent containing details about the targeting action.
     */
    @Subscribe public void onShipTargetedEvent(ShipTargetedEvent event) {
        if (event == null) return;
        if (!continueTargeting) return;

        if (!event.isTargetLocked()) {
            log.debug("[journal] target lock lost - stopping");
            continueTargeting = false;
            return;
        }

        String stripped = stripRawKey(event.getSubsystem());
        if (stripped == null) {
            log.debug("[journal] ShipTargeted: no machine key (scanStage={}, raw=[{}]) - cycling on",
                    event.getScanStage(), event.getSubsystem());
            consecutiveTimeouts = 0;
            pause = false;
            return;
        }

        Instant eventInstant = Instant.parse(event.getTimestamp());
        Instant keyPressFloor = lastKeyPressInstant.truncatedTo(ChronoUnit.SECONDS);
        if (eventInstant.isBefore(keyPressFloor)) {
            log.debug("[journal] stale event filtered: eventTime=[{}] keyPressFloor=[{}]", eventInstant, keyPressFloor);
            return;
        }

        log.debug("[journal] stripped=[{}] targetKey=[{}]", stripped, targetMachineKey);

        if (targetMachineKey != null && stripped.contains(targetMachineKey)) {
            log.debug("[journal] MATCH - stopping");
            continueTargeting = false;
            GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_1));
            consecutiveTimeouts = 0;
            pause = false;
            return;
        }

        // Not the target. Track the cycle to detect a full revolution (module not installed).
        if (anchorRaw == null) {
            anchorRaw = stripped;
        } else if (!stripped.equals(anchorRaw)) {
            leftAnchor = true;
        } else if (leftAnchor) {
            log.debug("[journal] returned to anchor [{}] - full loop, [{}] not installed", anchorRaw, getTarget());
            continueTargeting = false;
            announceNotInstalled();
            consecutiveTimeouts = 0;
            pause = false;
            return;
        }

        log.debug("[journal] no match - continuing");
        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
        consecutiveTimeouts = 0;
        pause = false;
    }

    private void announceNotInstalled() {
        String text = EventsTextProvider.getText("subsystem.not_installed", getTarget());
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(text));
        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
    }

    /**
     * Picks the cycle direction for a target. Core internals and the drive cluster at the bottom
     * of the subsystem cycle, so they are reached fastest by cycling previous; everything else
     * (hardpoints, utilities, cargo hatch) keeps the default forward cycle.
     */
    private static Bindings.GameCommand directionFor(String machineKey) {
        boolean bottomCluster = BOTTOM_CLUSTER_PREFIXES.stream().anyMatch(machineKey::startsWith);
        return bottomCluster ? BINDING_CYCLE_PREVIOUS_SUBSYSTEM : BINDING_CYCLE_NEXT_SUBSYSTEM;
    }

    /**
     * Normalises the journal's raw {@code Subsystem} field (e.g. {@code $int_powerplant_size5_class5_name;})
     * to a lower-case machine key fragment ({@code int_powerplant_size5_class5}) for substring matching.
     */
    private static String stripRawKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return null;
        return rawKey.replaceAll("^\\$", "")
                .replaceAll("_name;?$", "")
                .replaceAll(";$", "")
                .toLowerCase(Locale.ROOT);
    }
}
