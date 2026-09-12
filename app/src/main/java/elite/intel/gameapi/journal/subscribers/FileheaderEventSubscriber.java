package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.RadioVoicing;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.GameLanguage;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.i18n.Language;
import elite.intel.setup.GameEditionCheck;
import elite.intel.ui.event.RestartMouthEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A game session started while the app was already running: hands its edition to {@link GameEditionCheck},
 * which warns when it is not Odyssey, and its client language to {@link GameLanguage}, which decides which
 * engine voices radio transmissions and in what language. The other order - app started while the game runs -
 * never reaches this subscriber, because the header is then a replay; both of those read the header off disk
 * instead.
 * <p>
 * The radio engine is decided once, when the mouth starts, so a client relaunched in another language means
 * the mouth is restarted: the same {@link RestartMouthEvent} a TTS setting change sends, and a no-op while the
 * services are stopped (they will read the new language when they start).
 */
public class FileheaderEventSubscriber {

    private static final Logger log = LogManager.getLogger(FileheaderEventSubscriber.class);

    @Subscribe
    public void onEvent(FileheaderEvent event) {
        GameEditionCheck.getInstance().onGameSessionStarted(event.isOdyssey());

        Language before = RadioVoicing.transmissionLanguage();
        GameLanguage.getInstance().onGameSessionStarted(event.getLanguage());
        Language after = RadioVoicing.transmissionLanguage();
        if (after != before) {
            log.info("Radio transmissions now read in {} (was {}); restarting the mouth", after, before);
            UiBus.publish(new RestartMouthEvent());
        }
    }
}
