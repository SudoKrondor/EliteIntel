package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.gameapi.bookmarks.PendingSurfaceTarget;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.ApproachBodyEvent;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Hands a bookmarked surface spot to surface guidance when the ship reaches its planet, and forgets it when the
 * commander sets off somewhere else.
 * <p>
 * ApproachBody is the moment: it fires on entering orbital cruise, which is when guidance has a position to
 * steer from and still has the height to plan the descent.
 */
@SuppressWarnings("unused") //registered in SubscriberRegistration
public class BookmarkSurfaceTargetSubscriber {

    private final PendingSurfaceTarget pending = PendingSurfaceTarget.getInstance();

    @Subscribe
    public void onApproachBody(ApproachBodyEvent event) {
        if (event.isReplay()) return;
        pending.takeIfOn(event.getStarSystem(), event.getBody()).ifPresent(bookmark -> {
            PendingSurfaceTarget.startGuidance(bookmark);
            VegaRuntime.narrator().announce(
                    StringUtls.localizedResponse("handler.bookmark.guidanceEngaged", bookmark.planetName()), false);
        });
    }

    /**
     * A route plotted to another system is the commander changing their mind. A route to the bookmark's own
     * system - the one the bookmark command plotted - leaves it waiting.
     */
    @Subscribe
    public void onRoutePlotted(GameEvents.NavRouteEvent event) {
        List<GameEvents.NavRouteEvent.RouteEntry> route = event.getRoute();
        if (route == null || route.size() < 2) return;
        pending.routePlottedTo(route.getLast().getStarSystem());
    }
}
