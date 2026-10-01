package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.ShutdownEvent;
import elite.intel.session.Status;

/**
 * The game has closed, so the stored game state goes back to "not in game". A replayed Shutdown at app start
 * is handled the same way: the newest journal only ends in one when the game really did close.
 */
@SuppressWarnings("unused")
public class ShutdownSubscriber {

    @Subscribe
    public void onShutdown(ShutdownEvent event) {
        Status.getInstance().markGameClosed(event.getTimestamp());
    }
}
