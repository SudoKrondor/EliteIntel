package elite.intel.eventbus;

import com.google.common.eventbus.EventBus;

/**
 * Dedicated synchronous event bus for game controller (hands) events.
 * <p>
 * Kept separate from {@link GameEventBus} so that keystroke dispatch never
 * blocks or is blocked by STT/TTS/LLM/journal traffic.
 * <p>
 * Synchronous (not async) by design: keystroke sequences rely on the caller
 * blocking between publishing while sleeping, so fire-and-forget would break timing.
 */
public class GameControllerBus {
    private static final EventBus bus = new EventBus("game-controller");

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
