package elite.intel.gameapi;

import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The language the game client itself is running in, as its journal header states it
 * ({@code "language":"Russian/RU"}). This is not the commander's command language: one can play an English
 * client and speak German to us, or the other way round.
 * <p>
 * Everything the game writes as prose - the NPC lines behind radio transmissions, and the localized names of
 * materials and commodities - is in the client's language, so a radio transmission has to be read in the
 * client's language whatever the commander set here, and a learned name filed under it.
 * <p>
 * Russian is the one client language that is not also a language of this app. It has no {@link Language} and
 * reads as unknown to {@link #language()}, but {@link #isRussian()} still names it, because its radio chatter
 * is Cyrillic and Kokoro has no Cyrillic phonemizer: Supertonic voices it instead (see
 * {@code elite.intel.ai.mouth.RadioVoicing}). That is the only place a Russian client is treated differently.
 * <p>
 * Read lazily and cached: the header is the first line of the newest journal, it does not change while a game
 * session runs, and a live {@code Fileheader} replaces it when the commander starts a new one (see
 * {@link elite.intel.gameapi.journal.subscribers.FileheaderEventSubscriber}). Unknown - no journal yet, an
 * unreadable header, a language this app does not speak - is {@link Optional#empty()}, and the caller decides
 * what to fall back on.
 */
public class GameLanguage {

    private static final Logger log = LogManager.getLogger(GameLanguage.class);

    private static final String LANGUAGE_FIELD = "language";
    /**
     * The header spells the client language {@code "<Name>/<REGION>"} - {@code "Russian/RU"},
     * {@code "English/UK"}, {@code "Portuguese/BR"} - and the name is what identifies it. Frontier ships the
     * client in these, and Russian; Portuguese is the Brazilian edition only.
     */
    private static final Map<String, Language> CLIENT_LANGUAGES = Map.of(
            "english", Language.EN,
            "french", Language.FR,
            "german", Language.DE,
            "spanish", Language.ES,
            "portuguese", Language.PTBZ);

    private static final String RUSSIAN = "russian";

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
     * The language the game client writes its prose in, or empty when it is not known or is not one this app
     * speaks (a Russian client - see {@link #isRussian()}).
     */
    public Optional<Language> language() {
        return clientName(current()).map(CLIENT_LANGUAGES::get);
    }

    /**
     * Whether the game client is the Russian edition, whose radio chatter only Supertonic can voice.
     */
    public boolean isRussian() {
        return clientName(current()).filter(RUSSIAN::equals).isPresent();
    }

    /**
     * A live journal header names the client language of the session that just started, replacing whatever an
     * earlier journal said. Whatever was decided on the old answer (which engine voices radio, and in what
     * language) is the caller's to decide again - see {@code FileheaderEventSubscriber}.
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

    /**
     * The client language's name, lower case and without its region: {@code "Russian/RU"} is {@code "russian"}.
     */
    private static Optional<String> clientName(String headerLanguage) {
        if (headerLanguage == null) return Optional.empty();
        String name = headerLanguage.toLowerCase(Locale.ROOT);
        int region = name.indexOf('/');
        if (region >= 0) name = name.substring(0, region);
        return Optional.of(name.trim());
    }
}
