package elite.intel.junit.prompt;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.commons.HandlerDispatchedEvent;
import elite.intel.gameapi.EventBusManager;

/**
 * Subscribes to {@link HandlerDispatchedEvent} on the main EventBus and captures
 * the most recent dispatch so tests can assert on it.
 * <p>
 * Register once and reuse across tests - call {@link #reset()} before each assertion.
 */
public class HandlerCapture {

    private volatile HandlerDispatchedEvent lastEvent;

    public HandlerCapture() {
        EventBusManager.register(this);
    }

    @Subscribe
    public void onDispatch(HandlerDispatchedEvent event) {
        this.lastEvent = event;
    }

    public HandlerDispatchedEvent getLastEvent() {
        return lastEvent;
    }

    public void reset() {
        lastEvent = null;
    }
}
