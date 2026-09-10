package elite.intel.setup;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.JournalHeader;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Warns the commander that the game is not Odyssey. Everything this app does on foot - suits, backpacks,
 * settlements, exobiology, the on-foot half of {@code Status.json} - exists only in Odyssey, so on Horizons
 * it is not merely reduced, it is broken, and saying so is far kinder than letting them work that out.
 * <p>
 * <b>How the edition is known.</b> The journal's {@code Fileheader} line carries {@code "Odyssey":true|false},
 * and so does {@code LoadGame}. That flag is the only reliable discriminator: {@code Horizons} is true under
 * Odyssey as well (it means the commander <em>owns</em> Horizons content), and {@code gameversion} 4.x is
 * shared by the Odyssey and Horizons 4.0 clients. See {@link FileheaderEvent}.
 * <p>
 * <b>Why two entry points.</b> {@link elite.intel.gameapi.journal.events.BaseEvent#isReplay()} drops every
 * journal line stamped before the app started, so the live header never arrives when the app is started while
 * the game is already running - which is the common case. So {@link #check()} reads the newest journal's
 * header off disk at service start, and {@link #onGameSessionStarted(boolean)} handles the other order, where
 * the game is launched afterwards and writes a fresh header onto the live bus.
 * <p>
 * <b>Silence means unknown, not Odyssey.</b> A missing or unreadable header says nothing about the edition and
 * is left alone: only a header that states {@code Odyssey:false} is worth interrupting a commander over. The
 * missing-journal case belongs to {@link SetupCheck}, which can say something useful about it.
 * <p>
 * Warned at most once per edition: a commander who has been told is not told again on every service restart,
 * and a session that comes up on Odyssey re-arms the warning for the next one. Spoken straight to TTS rather
 * than through VEGA, for the reason {@code LocalLlmModelCheck} gives.
 */
public class GameEditionCheck {

    private static final Logger log = LogManager.getLogger(GameEditionCheck.class);

    private static final String ODYSSEY_FLAG = "Odyssey";

    private static volatile GameEditionCheck instance;

    private final Supplier<Path> journalDir;
    private final AtomicBoolean warned = new AtomicBoolean();

    private GameEditionCheck() {
        this(() -> PlayerSession.getInstance().getJournalPath());
    }

    /**
     * Seam for tests: the journal folder is the only thing this reads about the install.
     */
    GameEditionCheck(Supplier<Path> journalDir) {
        this.journalDir = journalDir;
    }

    public static synchronized GameEditionCheck getInstance() {
        if (instance == null) instance = new GameEditionCheck();
        return instance;
    }

    /**
     * Startup path: judges the edition from the newest journal file already on disk, which is the session the
     * commander is playing right now whenever the app was started second. Says nothing when there is no
     * journal to read or its header does not state the flag.
     */
    public void check() {
        readNewestHeaderFlag().ifPresent(odyssey -> evaluate(odyssey, "the newest journal file"));
    }

    /**
     * Live path: a new game session announced itself on the bus while the app was already running.
     */
    public void onGameSessionStarted(boolean odyssey) {
        evaluate(odyssey, "the journal header of the session that just started");
    }

    private void evaluate(boolean odyssey, String source) {
        if (odyssey) {
            warned.set(false); // the next session that is not Odyssey gets told about it
            return;
        }
        if (!warned.compareAndSet(false, true)) {
            log.info("Game is still not Odyssey ({}); already warned this run", source);
            return;
        }
        GameEventBus.publish(new VocalisationRequestEvent(
                StringUtls.localizedSpeech("speech.notOdyssey"),
                MissionCriticalAnnouncementEvent.class, false));
        UiBus.publish(new AppLogEvent(
                "This game is not Odyssey (according to " + source + "). EliteIntel is built for Odyssey"
                        + " and much of it will not work on Horizons."));
    }

    /**
     * The Odyssey flag from the first line of the newest journal, or empty when there is nothing to read it
     * from. The newest {@code .log} is the same file {@code JournalParser} picks, so this judges the session
     * the app is actually following.
     */
    private Optional<Boolean> readNewestHeaderFlag() {
        return JournalHeader.ofNewestJournal(journalDir.get())
                .flatMap(header -> JournalHeader.bool(header, ODYSSEY_FLAG));
    }

    /**
     * Reads {@code Odyssey} out of a journal's first line, or empty when that line is not a {@code Fileheader}
     * stating the flag.
     */
    static Optional<Boolean> odysseyFlag(String headerLine) {
        return JournalHeader.parse(headerLine).flatMap(header -> JournalHeader.bool(header, ODYSSEY_FLAG));
    }
}
