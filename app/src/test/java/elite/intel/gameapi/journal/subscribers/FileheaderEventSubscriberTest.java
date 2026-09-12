package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.GameLanguage;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.ui.event.RestartMouthEvent;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The radio engine and the language it reads transmissions in are decided when the mouth starts, from the game
 * client's language. A client relaunched in another language while the app runs must therefore restart the
 * mouth - and a client relaunched in the same one must not, or every game start would re-greet the commander.
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
    void aClientRelaunchedInAnotherLanguageRestartsTheMouth() {
        subscriber.onEvent(header("Russian/RU"));
        assertEquals(1, mouthRestarts.get(), "Russian chatter needs Supertonic, so the radio engine must be redecided");

        subscriber.onEvent(header("English/UK"));
        assertEquals(2, mouthRestarts.get(), "and back to Kokoro when the client goes back to English");
    }

    @Test
    void aClientRelaunchedInTheSameLanguageLeavesTheMouthAlone() {
        subscriber.onEvent(header("English/UK"));
        subscriber.onEvent(header("English/UK"));
        assertEquals(0, mouthRestarts.get(), "the same language decides the same engine - no restart, no re-greeting");
    }

    /**
     * Frontier's language strings carry a region; only the language moves the radio, so a header naming the
     * same language from another region is the same session as far as the mouth is concerned.
     */
    @Test
    void aRegionChangeWithinOneLanguageIsNotAChange() {
        subscriber.onEvent(header("English/US"));
        assertEquals(0, mouthRestarts.get());
    }

    private static FileheaderEvent header(String language) {
        JsonObject json = GsonFactory.getGson().fromJson(
                "{ \"timestamp\":\"2026-07-25T05:49:56Z\", \"event\":\"Fileheader\", \"part\":1,"
                        + " \"language\":\"" + language + "\", \"Odyssey\":true, \"gameversion\":\"4.4.0.3\","
                        + " \"build\":\"r330683/r0 \" }", JsonObject.class);
        return new FileheaderEvent(json);
    }
}
