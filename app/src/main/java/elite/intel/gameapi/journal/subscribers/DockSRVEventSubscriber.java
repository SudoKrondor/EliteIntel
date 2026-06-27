package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.gameapi.journal.events.DockSRVEvent;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

public class DockSRVEventSubscriber {

    @Subscribe
    public void onDockSRVEvent(DockSRVEvent event) {
        EventNarrator.say(localizedEvent("event.srv.welcomeBack", PlayerSession.getInstance().getVariablePlayerName()));
    }
}
