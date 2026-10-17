package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.gameapi.journal.events.WingInviteEvent;

/**
 * Tells the commander another commander has invited them into a wing, phrased by VEGA in its own personality.
 * <p>
 * WHY the payload is one labelled name and nothing more: the journal gives the name alone, and a bare player name
 * reads to a small model like anything - the Friends announcement once came back with a friend's name as a place.
 * Saying it is a commander's name, and asking for nothing the payload lacks, leaves it nothing to invent.
 */
@SuppressWarnings("unused")
public class WingInviteSubscriber {

    @Subscribe
    public void onWingInvite(WingInviteEvent event) {
        String name = event.getName();
        if (name == null || name.isBlank()) return;
        VegaRuntime.narrator().narrate(
                "Wing invitation received from another player. Inviting commander's name: " + name,
                "Tell the commander that Commander " + name + " has invited them to join a wing. "
                        + "Say the name exactly as written. Keep it to one short sentence.");
    }
}
