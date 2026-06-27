package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.gameapi.SensorDataEvent;

/**
 * Companion-mode bridge for the legacy sensor-analysis channel. SensorDataEvent is emitted by gameplay
 * subscribers after they apply their own filtering, settings, calculations, and narration instructions;
 * this bridge turns that trusted subscriber output into an EVENT thought.
 */
public final class CompanionSensorDataBridge {

    private final ThoughtDispatcher dispatcher;

    public CompanionSensorDataBridge(ThoughtDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /** Routes subscriber-prepared sensor narration into the companion event lane. */
    @Subscribe
    public void onSensorData(SensorDataEvent event) {
        dispatcher.submitSensorData(event);
    }
}
