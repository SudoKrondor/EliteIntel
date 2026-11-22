package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.GameLanguage;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.setup.GameEditionCheck;

/**
 * A game session started while the app was already running: hands its edition to {@link GameEditionCheck},
 * which warns when it is not Odyssey, and its client language to {@link GameLanguage}, which decides the
 * language radio transmissions are read in. The other order - app started while the game runs -
 * never reaches this subscriber, because the header is then a replay; both of those read the header off disk
 * instead.
 * <p>
 * A client relaunched in another language needs nothing more: the radio engine reads the transmission language
 * on every request, so the next transmission is read in the new one without restarting the mouth.
 */
public class FileheaderEventSubscriber {

    @Subscribe
    public void onEvent(FileheaderEvent event) {
        GameEditionCheck.getInstance().onGameSessionStarted(event.isOdyssey());
        GameLanguage.getInstance().onGameSessionStarted(event.getLanguage());
    }
}
