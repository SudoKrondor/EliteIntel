package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.setup.GameEditionCheck;

/**
 * A game session started while the app was already running: hands its edition to {@link GameEditionCheck},
 * which warns when it is not Odyssey. The other order - app started while the game runs - never reaches this
 * subscriber, because the header is then a replay; see that class.
 */
public class FileheaderEventSubscriber {

    @Subscribe
    public void onEvent(FileheaderEvent event) {
        GameEditionCheck.getInstance().onGameSessionStarted(event.isOdyssey());
    }
}
