package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.RouteAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.NavRouteEvent;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

public class NavRouteSetSubscriber {

    @Subscribe
    public void onNavRouteSetEvent(NavRouteEvent event) {
        if (PlayerSession.getInstance().isRouteAnnouncementOn()) {
            EventBusManager.publish(new RouteAnnouncementEvent(localizedEvent("event.route.set")));
        }
    }
}
