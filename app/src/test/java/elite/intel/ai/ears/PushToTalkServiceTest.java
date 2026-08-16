package elite.intel.ai.ears;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.db.util.Database;
import elite.intel.devices.events.DeviceButtonEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.eventbus.DeviceBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.PttButtonStateEvent;
import elite.intel.ui.event.PttModeChangedEvent;
import elite.intel.ui.event.PushToTalkSettingsChangedEvent;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The push-to-talk gate as a service: which button transitions it acts on, and what it publishes.
 *
 * <p>WHY these exist: this behaviour used to live in a Swing settings panel, where the only way to exercise it
 * was to open a settings tab and press a physical button, so it was never covered at all. The gate state it
 * publishes is what the STT capture window is built on, and a silent regression here costs the commander the
 * microphone.
 *
 * <p>No controller is connected in a test JVM, so a press can never match a configured device: the matching
 * guard is exercised from the negative side, and the arming policy through the settings and lifecycle paths,
 * neither of which depends on SDL.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PushToTalkServiceTest {

    /**
     * Collects everything published on one bus, so a case can assert on what was and was not announced.
     */
    private static final class Collector {
        private final List<Object> events = new ArrayList<>();

        @Subscribe
        public void onEvent(Object event) {
            events.add(event);
        }

        boolean sawGateChange() {
            return events.stream().anyMatch(event -> event instanceof PttButtonStateEvent);
        }

        boolean sawArmed(boolean armed) {
            return events.stream().anyMatch(event ->
                    event instanceof PttModeChangedEvent mode && mode.isActive() == armed);
        }

        void clear() {
            events.clear();
        }
    }

    private final Collector ui = new Collector();
    private final Collector game = new Collector();

    private boolean savedEnabled;
    private String savedController;
    private int savedButton;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void setUp() {
        SystemSession session = SystemSession.getInstance();
        savedEnabled = session.isPushToTalkEnabled();
        savedController = session.getPushToTalkControllerName();
        savedButton = session.getPushToTalkButtonIndex();

        UiBus.register(ui);
        GameEventBus.register(game);
    }

    @AfterEach
    void tearDown() {
        PushToTalkService.getInstance().stop();
        UiBus.unregister(ui);
        GameEventBus.unregister(game);
        ui.clear();
        game.clear();

        SystemSession session = SystemSession.getInstance();
        session.setPushToTalkEnabled(savedEnabled);
        session.setPushToTalkControllerName(savedController);
        session.setPushToTalkButtonIndex(savedButton);
    }

    /**
     * Arming is what tells the rest of the app that the button now owns the microphone: the STT pipeline
     * discards anything captured without it, and the AI tab says so on its status badge.
     */
    @Test
    void armingAnnouncesThatTheButtonOwnsTheGate() {
        configure(true);

        PushToTalkService.getInstance().start();

        assertTrue(ui.sawArmed(true), "push-to-talk on means the button owns the gate");
        assertFalse(ui.sawGateChange(), "but nothing is held until the commander presses it");
    }

    @Test
    void startingWithPushToTalkOffChangesNothing() {
        configure(false);
        ui.clear();

        PushToTalkService.getInstance().start();

        assertTrue(ui.events.isEmpty(), "a disabled gate has no policy to enforce and nothing to announce");
    }

    @Test
    void turningPushToTalkOffHandsTheMicrophoneBack() {
        configure(true);
        PushToTalkService.getInstance().start();
        ui.clear();

        configure(false);
        UiBus.publish(new PushToTalkSettingsChangedEvent());

        assertTrue(ui.sawArmed(false), "with the gate gone the microphone listens on its own again");
    }

    /**
     * A button on a controller that is not the configured one must be ignored, or every device in the
     * commander's cockpit would cut her off mid-sentence.
     */
    @Test
    void aButtonFromAnUnknownControllerIsIgnored() {
        configure(true);
        PushToTalkService.getInstance().start();
        ui.clear();
        game.clear();

        DeviceBus.publish(new DeviceButtonEvent(4242, 0, true));

        assertTrue(game.events.stream().noneMatch(event -> event instanceof TTSInterruptEvent),
                "no interrupt for a device we are not mapped to");
        assertFalse(ui.sawGateChange(), "and the gate stays shut");
    }

    /**
     * The disconnect release exists because a controller cannot report the release of a button it no longer
     * has. It must fire only for the controller actually holding the gate, or an unrelated device being
     * unplugged would slam the gate shut mid-sentence.
     */
    @Test
    void aDisconnectThatIsNotHoldingTheGateChangesNothing() {
        configure(true);
        PushToTalkService.getInstance().start();
        ui.clear();

        DeviceBus.publish(new DeviceDisconnectedEvent(4242));

        assertFalse(ui.sawGateChange(), "nothing was holding the gate, so nothing is released");
    }

    @Test
    void stoppingWhileIdlePublishesNoRelease() {
        configure(true);
        PushToTalkService.getInstance().start();
        ui.clear();

        PushToTalkService.getInstance().stop();

        assertFalse(ui.sawGateChange(), "an idle gate has nothing to release");
    }

    /**
     * The service is a singleton shared by the service registry, so a restart must not double-register its
     * subscriptions: Guava throws on unregistering twice, and a doubled subscription would beep twice per
     * press.
     */
    @Test
    void startAndStopAreIdempotent() {
        configure(true);
        PushToTalkService.getInstance().start();
        PushToTalkService.getInstance().start();
        PushToTalkService.getInstance().stop();
        PushToTalkService.getInstance().stop();

        PushToTalkService.getInstance().start();
        ui.clear();
        DeviceBus.publish(new DeviceDisconnectedEvent(4242));

        assertFalse(ui.sawGateChange());
    }

    private static void configure(boolean enabled) {
        SystemSession session = SystemSession.getInstance();
        session.setPushToTalkEnabled(enabled);
        session.setPushToTalkControllerName("Test HOTAS");
        session.setPushToTalkButtonIndex(0);
    }
}
