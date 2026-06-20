package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.CommanderEvent;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.CommanderChangedEvent;

@SuppressWarnings("unused")
public class CommanderEventSubscriber {

    @Subscribe
    public void onEvent(CommanderEvent event) {
        PlayerSession.getInstance().setInGameName(event.getName());
        UiBus.publish(new CommanderChangedEvent(event.getName()));
    }
}
