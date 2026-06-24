package elite.intel.eventbus;

import com.google.common.eventbus.AsyncEventBus;

import java.util.concurrent.Executors;

/**
 * Separate async event bus for joystick/HOTAS/gamepad/pedal input events.
 * <p>
 * Uses Guava AsyncEventBus backed by a single daemon thread so that publishing
 * from DeviceService's 60 Hz poll loop never blocks the main GameEventBus
 * (which is synchronous on the caller's thread).
 */
public class DeviceBus {

    private static final AsyncEventBus bus = new AsyncEventBus(
            "device-input",
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Device-Input-Bus");
                t.setDaemon(true);
                return t;
            })
    );

    public static void publish(Object event) {
        bus.post(event);
    }

    public static void register(Object subscriber) {
        bus.register(subscriber);
    }

    public static void unregister(Object subscriber) {
        bus.unregister(subscriber);
    }
}
