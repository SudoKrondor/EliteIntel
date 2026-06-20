package elite.intel.ai.mouth;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.ui.controller.ManagedService;

public interface MouthInterface extends ManagedService {

    void interruptAndClear();
    /**
     * Use elite.intel.gameapi.SubscriberRegistration to subscribe to these events
     * or add GameEventBus.register(this); in the constructor if your class is a singleton
     */
    @Subscribe void onVoiceProcessEvent(VocalisationRequestEvent event);
}
