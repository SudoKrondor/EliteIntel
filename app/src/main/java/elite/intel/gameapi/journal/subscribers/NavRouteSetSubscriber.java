package elite.intel.gameapi.journal.subscribers;

import elite.intel.companion.CompanionRuntime;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.NavRouteEvent;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

public class NavRouteSetSubscriber {

    @Subscribe
    public void onNavRouteSetEvent(NavRouteEvent event) {
        if (PlayerSession.getInstance().isRouteAnnouncementOn()) {
            CompanionRuntime.narrator().announce(localizedEvent("event.route.set"), false);
        }
    }
}
