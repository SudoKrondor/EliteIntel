package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.DockSRVEvent;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

public class DockSRVEventSubscriber {

    @Subscribe
    public void onDockSRVEvent(DockSRVEvent event) {
        GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.srv.welcomeBack", PlayerSession.getInstance().getVariablePlayerName())));
    }
}
