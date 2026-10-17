package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.gameapi.journal.events.WingInviteEvent;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * Tells the commander another commander has invited them into a wing.
 * <p>
 * WHY the line is fixed, as {@link FriendsEventSubscriber}'s are: the journal gives a name and nothing else, and a bare
 * player name reads to a small model like anything - the Friends announcement, phrased by the LLM, once came back
 * with a friend's name as a place. A finished, localized line cannot invent anything (review, 2026-10-17).
 */
public class WingInviteSubscriber {

    @Subscribe
    public void onWingInvite(WingInviteEvent event) {
        String name = event.getName();
        if (name == null || name.isBlank()) return;
        VegaRuntime.narrator().announce(localizedEvent("event.wingInvite", name), false);
    }
}
