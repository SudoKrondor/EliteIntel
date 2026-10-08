package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.ButtonMapGeneration.Controller;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.model.Device;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The startup service: controllers already connected are handled, nothing runs after stop, one failure is
 * contained.
 */
class ButtonMapStartupTest {

    /** A real VIRPIL GUID - VID 3344, PID 83F4 - as {@code DeviceIdentitiesTest} carries it. */
    private static final Device LVWAP =
            new Device(7, "VIRPIL Controls 20220720", 3, 30, "", "03002cec44330000f483000000000000");
    private static final Device NO_GUID = new Device(8, "Mystery Stick", 2, 10, "", "");

    private ButtonMapStartup startup;

    @AfterEach
    void stopIt() {
        if (startup != null) startup.stop();
    }

    @Test
    void controllersAlreadyConnectedAtStartAreHandled() throws InterruptedException {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        startup = new ButtonMapStartup(recording(seen, done), () -> List.of(LVWAP));

        startup.start();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(List.of(new Controller("3344", "83F4", 30, 3)), seen);
    }

    @Test
    void aConnectAfterStopDoesNothing() {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        startup = new ButtonMapStartup(recording(seen, new CountDownLatch(1)), List::of);
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
        Device other = new Device(9, "Other", 1, 12, "", "03002cec44330000f583000000000000");
        startup = new ButtonMapStartup(generation, () -> List.of(LVWAP, other));
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
        Device other = new Device(9, "Other", 1, 12, "", "03002cec44330000f583000000000000");
        startup = new ButtonMapStartup(generation, () -> List.of(LVWAP, other));

        startup.start();

        assertTrue(second.await(5, TimeUnit.SECONDS));
    }

    /** A controller with no usable GUID has no VID/PID to match, so it is skipped rather than failing. */
    @Test
    void aControllerWithNoGuidIsSkipped() throws InterruptedException {
        List<Controller> seen = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        startup = new ButtonMapStartup(recording(seen, done), () -> List.of(NO_GUID, LVWAP));

        startup.start();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(1, seen.size());
    }

    private static Consumer<Controller> recording(List<Controller> seen, CountDownLatch done) {
        return controller -> {
            seen.add(controller);
            done.countDown();
        };
    }
}
