package elite.intel.gameapi;

import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The language the game client itself is running in, as its journal header states it
 * ({@code "language":"Russian/RU"}). This is not the commander's command language: one can play an English
 * client and speak German to us, or the other way round.
 * <p>
 * It matters for one thing only. Everything the game writes as prose - the NPC lines behind radio
 * transmissions - is written in the client's language, so a Russian client hands us Cyrillic to speak whatever
 * the commander set here. The local Supertonic engine, which voices radio everywhere, reads Cyrillic natively,
 * so a Russian client's NPC lines are spoken in Russian rather than dropped; the game ships no Ukrainian
 * client at all, so Russian is the only Cyrillic case there is.
 * <p>
 * Read lazily and cached: the header is the first line of the newest journal, it does not change while a game
 * session runs, and a live {@code Fileheader} replaces it when the commander starts a new one (see
 * {@link elite.intel.gameapi.journal.subscribers.FileheaderEventSubscriber}). Unknown - no journal yet, an
 * unreadable header - is treated as "not Cyrillic": a commander whose client we cannot identify keeps the
 * radio they had.
 */
public class GameLanguage {

    private static final Logger log = LogManager.getLogger(GameLanguage.class);

    private static final String LANGUAGE_FIELD = "language";
    /**
     * The header spells the client language {@code "<Name>/<REGION>"}, e.g. {@code "Russian/RU"}.
     */
    private static final String RUSSIAN_CLIENT = "russian";

    private static volatile GameLanguage instance;

    private final Supplier<Path> journalDir;
    private volatile String observed;
    private volatile boolean read;

    private GameLanguage() {
        this(() -> PlayerSession.getInstance().getJournalPath());
    }

    /**
     * Seam for tests: the journal folder is the only thing this reads about the install.
     */
    GameLanguage(Supplier<Path> journalDir) {
        this.journalDir = journalDir;
    }

    public static synchronized GameLanguage getInstance() {
        if (instance == null) instance = new GameLanguage();
        return instance;
    }

    /**
     * Whether the game client writes its prose in Cyrillic, which no local voice can read aloud.
     */
    public boolean isCyrillicScript() {
        String language = current();
        return language != null && language.toLowerCase(Locale.ROOT).startsWith(RUSSIAN_CLIENT);
    }

    /**
     * A live journal header names the client language of the session that just started, replacing whatever an
     * earlier journal said.
     */
    public void onGameSessionStarted(String headerLanguage) {
        if (headerLanguage == null || headerLanguage.isBlank()) return;
        read = true;
        if (headerLanguage.equals(observed)) return;
        observed = headerLanguage;
        log.info("Game client language is {}", headerLanguage);
    }

    private String current() {
        if (!read) {
            observed = readNewestHeaderLanguage().orElse(null);
            read = true;
            log.info("Game client language read from the newest journal: {}", observed);
        }
        return observed;
    }

    private Optional<String> readNewestHeaderLanguage() {
        return JournalHeader.ofNewestJournal(journalDir.get())
                .flatMap(header -> JournalHeader.string(header, LANGUAGE_FIELD));
    }
}
