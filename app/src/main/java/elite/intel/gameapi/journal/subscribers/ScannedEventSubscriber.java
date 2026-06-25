package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.gameapi.journal.events.ScannedEvent;
import elite.intel.session.SystemSession;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class ScannedEventSubscriber {

    @Subscribe
    public void onScannedEvent(ScannedEvent event) {
        SystemSession systemSession = SystemSession.getInstance();
        if ("cargo".equalsIgnoreCase(event.getScanType())) {
            EventNarrator.critical(localizedEvent("event.cargo.scanDetected"));
        }
    }
}
