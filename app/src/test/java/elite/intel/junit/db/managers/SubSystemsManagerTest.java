package elite.intel.junit.db.managers;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.SubSystemsManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.ShipTargetedEvent;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;
import elite.intel.util.SleepNoThrow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives {@link SubSystemsManager} end-to-end against an in-memory DB (seeded by the
 * standard migrations, which populate {@code sub_system} with machine keys).
 *
 * <p>The real game is simulated with a {@code GameResponder} that listens on the
 * {@link GameControllerBus} for cycle key presses and replies — asynchronously, like the
 * journal — with a {@link ShipTargetedEvent} for the next subsystem in a fixed cycle. The
 * replies carry only the raw {@code Subsystem} machine key (no {@code Subsystem_Localised}),
 * proving the manager matches on the reliable field.
 */
class SubSystemsManagerTest {

    private final SubSystemsManager manager = SubSystemsManager.getInstance();

    private final List<String> beeps = Collections.synchronizedList(new ArrayList<>());
    private final List<String> announcements = Collections.synchronizedList(new ArrayList<>());

    private final Object captor = new Object() {
        @Subscribe
        public void onBeep(PlayBeepEvent e) {
            beeps.add(e.getSoundFile());
        }

        @Subscribe
        public void onAnnounce(MissionCriticalAnnouncementEvent e) {
            announcements.add(e.getText());
        }
    };

    private GameResponder responder;

    @BeforeEach
    void setUp() {
        beeps.clear();
        announcements.clear();
        GameEventBus.register(captor);
    }

    @AfterEach
    void tearDown() {
        if (responder != null) GameControllerBus.unregister(responder);
        GameEventBus.unregister(captor);
        // Targeting stops itself on a terminal event; give the worker thread a moment to exit.
        SleepNoThrow.sleep(50);
    }

    @Test
    void moduleFound_stopsAndBeepsSuccess() throws InterruptedException {
        // Cycle presents two non-targets, then the Power Plant.
        startResponder(
                raw("int_lifesupport"),
                raw("int_powerdistributor"),
                raw("int_powerplant"));

        manager.targetSubSystem("power plant");

        awaitTrue(() -> beeps.contains(AudioPlayer.BEEP_1));
        assertTrue(beeps.contains(AudioPlayer.BEEP_1), "expected success beep on match");
        assertTrue(announcements.isEmpty(), "should not announce 'not installed' when the module is found");
    }

    @Test
    void moduleNotInstalled_announcesAfterFullLoop() throws InterruptedException {
        // Five fitted subsystems, none of them a Refinery. The cycle wraps back to the first.
        startResponder(
                raw("int_powerplant"),
                raw("int_hyperdrive"),
                raw("int_lifesupport"),
                raw("int_powerdistributor"),
                raw("int_shieldgenerator"));

        manager.targetSubSystem("refinery");

        awaitTrue(() -> !announcements.isEmpty());
        assertEquals(1, announcements.size(), "should announce exactly once on a completed loop");
        assertTrue(announcements.get(0).contains("Refinery"),
                "announcement should name the missing module, was: " + announcements.get(0));
        assertFalse(beeps.contains(AudioPlayer.BEEP_1), "must not signal a successful match");
    }

    @Test
    void coreInternalTargetCyclesPrevious() throws InterruptedException {
        startResponder(raw("int_powerplant"));
        manager.targetSubSystem("power plant");

        awaitTrue(() -> !responder.pressedBindings.isEmpty());
        assertEquals("CyclePreviousSubsystem", responder.pressedBindings.get(0),
                "core internals cluster at the bottom — approach by cycling previous");
    }

    @Test
    void hardpointTargetCyclesNext() throws InterruptedException {
        startResponder(raw("hpt_beamlaser"));
        manager.targetSubSystem("beam laser");

        awaitTrue(() -> !responder.pressedBindings.isEmpty());
        assertEquals("CycleNextSubsystem", responder.pressedBindings.get(0),
                "hardpoints sit at the top — keep the forward cycle");
    }

    // ── harness ──────────────────────────────────────────────────────────────

    private void startResponder(String... cycle) {
        responder = new GameResponder(cycle);
        GameControllerBus.register(responder);
    }

    /**
     * Builds a realistic raw Subsystem key, e.g. {@code $int_powerplant_size5_class5_name;}.
     */
    private static String raw(String machineKey) {
        return "$" + machineKey + "_size5_class5_name;";
    }

    private static ShipTargetedEvent shipTargeted(String rawSubsystem) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "ShipTargeted");
        j.addProperty("TargetLocked", true);
        j.addProperty("ScanStage", 3);
        j.addProperty("Subsystem", rawSubsystem); // deliberately no Subsystem_Localised
        return new ShipTargetedEvent(j);
    }

    /**
     * Simulates the game: each key press advances the cycle and replies asynchronously.
     */
    private static final class GameResponder {
        private final String[] cycle;
        private final AtomicInteger idx = new AtomicInteger(-1);
        final List<String> pressedBindings = Collections.synchronizedList(new ArrayList<>());

        GameResponder(String[] cycle) {
            this.cycle = cycle;
        }

        @Subscribe
        public void onPress(GameInputSequenceEvent e) {
            pressedBindings.add(e.getSteps().get(0).getBindingId());
            String rawSubsystem = cycle[Math.floorMod(idx.incrementAndGet(), cycle.length)];
            Thread t = new Thread(() -> {
                SleepNoThrow.sleep(15); // reply after the manager has parked on pause, like the journal
                GameEventBus.publish(shipTargeted(rawSubsystem));
            }, "TestGameResponder");
            t.setDaemon(true);
            t.start();
        }
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 5 seconds");
            Thread.sleep(10);
        }
    }
}