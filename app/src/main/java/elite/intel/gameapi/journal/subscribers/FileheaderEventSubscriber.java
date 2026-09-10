package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.GameLanguage;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.setup.GameEditionCheck;

/**
 * A game session started while the app was already running: hands its edition to {@link GameEditionCheck},
 * which warns when it is not Odyssey, and its client language to {@link GameLanguage}, which decides whether
 * radio transmissions can be voiced at all. The other order - app started while the game runs - never reaches
 * this subscriber, because the header is then a replay; both of those read the header off disk instead.
 */
public class FileheaderEventSubscriber {

    @Subscribe
    public void onEvent(FileheaderEvent event) {
        GameEditionCheck.getInstance().onGameSessionStarted(event.isOdyssey());
        GameLanguage.getInstance().onGameSessionStarted(event.getLanguage());
    }
}
