package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.GameLanguage;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.i18n.Language;
import elite.intel.ui.event.RestartMouthEvent;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The radio engine reads the transmission language on every request, so a client relaunched in another language
 * while the app runs is picked up from {@link GameLanguage} alone. Restarting the mouth for it would only re-greet
 * the commander.
 * <p>
 * Synchronous on purpose: {@link FileheaderEventSubscriber#onEvent} is called directly, so nothing here waits on
 * a bus thread.
 */
class FileheaderEventSubscriberTest {

    private final AtomicInteger mouthRestarts = new AtomicInteger();
    private final Object ui = new Object() {
        @Subscribe
        public void onRestart(RestartMouthEvent event) {
            mouthRestarts.incrementAndGet();
        }
    };
    private final FileheaderEventSubscriber subscriber = new FileheaderEventSubscriber();

    @BeforeEach
    void setUp() {
        UiBus.register(ui);
        // Known starting point: the singleton keeps what an earlier test, or the machine's own journal, said.
        GameLanguage.getInstance().onGameSessionStarted("English/UK");
        mouthRestarts.set(0);
    }

    @AfterEach
    void tearDown() {
        UiBus.unregister(ui);
    }

    @Test
    void aClientRelaunchedInAnotherLanguageIsReadInItWithoutRestartingTheMouth() {
        subscriber.onEvent(header("German/DE"));

        assertEquals(Optional.of(Language.DE), GameLanguage.getInstance().language(), "German chatter is read as German");
        assertEquals(0, mouthRestarts.get(), "the next transmission reads the new language - no restart, no re-greeting");
    }

    private static FileheaderEvent header(String language) {
        JsonObject json = GsonFactory.getGson().fromJson(
                "{ \"timestamp\":\"2026-07-25T05:49:56Z\", \"event\":\"Fileheader\", \"part\":1,"
                        + " \"language\":\"" + language + "\", \"Odyssey\":true, \"gameversion\":\"4.4.0.3\","
                        + " \"build\":\"r330683/r0 \" }", JsonObject.class);
        return new FileheaderEvent(json);
    }
}
