package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.ButtonMapGeneration.Controller;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;
import elite.intel.ui.event.BindForgeActivatedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The startup service: controllers already connected are handled, nothing runs after stop, one failure is
 * contained.
 */
class ButtonMapStartupTest {

    /** A real VIRPIL GUID - VID 3344, PID 83F4 - as {@code DeviceIdentitiesTest} carries it. */
    private static final Device LVWAP =
            new Device(7, "VIRPIL Controls 20220720", 3, 30, "", "03002cec44330000f483000000000000", false);
    private static final Device NO_GUID = new Device(8, "Mystery Stick", 2, 10, "", "", false);

    /** LVWAP as its HID description reads: X, Y and its twist on RZ, and one hat. */
    private static final DeviceDescription LVWAP_DESCRIPTION = new DeviceDescription(true,
            List.of(GameAxis.X, GameAxis.Y, GameAxis.RZ), 0, List.of(GameAxis.X, GameAxis.Y, GameAxis.RZ), 1);
    private static final IntFunction<Optional<DeviceDescription>> DESCRIBED =
            id -> Optional.of(id == LVWAP.id() ? LVWAP_DESCRIPTION : DeviceDescription.axesUnknown(0));

    private static final BooleanSupplier ACTIVATED = () -> true;

    private ButtonMapStartup startup;

    @AfterEach
    void stopIt() {
        if (startup != null) startup.stop();
    }

    @Test
    void controllersAlreadyConnectedAtStartAreHandled() throws InterruptedException {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        startup = new ButtonMapStartup(recording(seen, done), () -> List.of(LVWAP), DESCRIBED, ACTIVATED);

        startup.start();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(List.of(new Controller("3344", "83F4", 30, 3, LVWAP_DESCRIPTION)), seen);
    }

    /** A controller gone before it is handled has no description, and its next connect is handled then. */
    @Test
    void aControllerAlreadyGoneIsSkipped() throws InterruptedException {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        IntFunction<Optional<DeviceDescription>> onlyLvwap =
                id -> id == LVWAP.id() ? Optional.of(LVWAP_DESCRIPTION) : Optional.empty();
        Device gone = new Device(9, "Gone", 1, 12, "", "03002cec44330000f583000000000000", false);
        startup = new ButtonMapStartup(recording(seen, done), () -> List.of(gone, LVWAP), onlyLvwap, ACTIVATED);

        startup.start();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(List.of(new Controller("3344", "83F4", 30, 3, LVWAP_DESCRIPTION)), seen);
    }

    @Test
    void aConnectAfterStopDoesNothing() {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        startup = new ButtonMapStartup(recording(seen, new CountDownLatch(1)), List::of, DESCRIBED, ACTIVATED);
        startup.start();
        startup.stop();

        startup.onDeviceConnected(new DeviceConnectedEvent(LVWAP));

        assertTrue(seen.isEmpty());
    }

    /** A controller still queued when stop is called is dropped, and the one being handled is interrupted. */
    @Test
    void queuedWorkDoesNotRunOnceStopReturns() throws InterruptedException {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        CountDownLatch busy = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        Consumer<Controller> generation = controller -> {
            seen.add(controller);
            busy.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.countDown();
            }
        };
        Device other = new Device(9, "Other", 1, 12, "", "03002cec44330000f583000000000000", false);
        startup = new ButtonMapStartup(generation, () -> List.of(LVWAP, other), DESCRIBED, ACTIVATED);
        startup.start();
        assertTrue(busy.await(5, TimeUnit.SECONDS));

        startup.stop();

        assertTrue(interrupted.await(0, TimeUnit.SECONDS), "stop must interrupt the controller being handled");
        assertEquals(1, seen.size());
    }

    /** One controller's failure is logged, and the next controller is still handled. */
    @Test
    void oneFailureDoesNotStopTheNext() throws InterruptedException {
        CountDownLatch second = new CountDownLatch(1);
        Consumer<Controller> generation = controller -> {
            if (controller.buttonCount() == 30) throw new IllegalStateException("disk on fire");
            second.countDown();
        };
        Device other = new Device(9, "Other", 1, 12, "", "03002cec44330000f583000000000000", false);
        startup = new ButtonMapStartup(generation, () -> List.of(LVWAP, other), DESCRIBED, ACTIVATED);

        startup.start();

        assertTrue(second.await(5, TimeUnit.SECONDS));
    }

    /** A controller with no usable GUID has no VID/PID to match, so it is skipped rather than failing. */
    @Test
    void aControllerWithNoGuidIsSkipped() throws InterruptedException {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        startup = new ButtonMapStartup(recording(seen, done), () -> List.of(NO_GUID, LVWAP), DESCRIBED, ACTIVATED);

        startup.start();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(1, seen.size());
    }

    /**
     * Before BindForge is activated nothing is written into a game folder - the backup at activation must be first.
     */
    @Test
    void nothingIsGeneratedBeforeActivation() throws InterruptedException {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        AtomicBoolean activated = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(1);
        startup = new ButtonMapStartup(recording(seen, done), () -> List.of(LVWAP), DESCRIBED, activated::get);

        startup.start();
        startup.onDeviceConnected(new DeviceConnectedEvent(LVWAP));

        assertFalse(done.await(300, TimeUnit.MILLISECONDS), "generation ran before activation");
        assertTrue(seen.isEmpty());
    }

    /**
     * Activation hands the controllers connected at that moment what startup held back from them.
     */
    @Test
    void activationHandlesTheControllersAlreadyConnected() throws InterruptedException {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        AtomicBoolean activated = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(1);
        CountDownLatch heldBack = new CountDownLatch(1);
        // WHY: startup's own look at LVWAP must have answered "not activated" before activation, or a slow worker
        // reads the flag after it is set and LVWAP is handled twice - once from startup, once from activation.
        BooleanSupplier askedOnce = () -> {
            boolean answer = activated.get();
            heldBack.countDown();
            return answer;
        };
        startup = new ButtonMapStartup(recording(seen, done), () -> List.of(LVWAP), DESCRIBED, askedOnce);
        startup.start();
        assertTrue(heldBack.await(5, TimeUnit.SECONDS));

        activated.set(true);
        startup.onActivated(new BindForgeActivatedEvent());

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(List.of(new Controller("3344", "83F4", 30, 3, LVWAP_DESCRIPTION)), seen);
    }

    private static Consumer<Controller> recording(List<Controller> seen, CountDownLatch done) {
        return controller -> {
            seen.add(controller);
            done.countDown();
        };
    }
}
