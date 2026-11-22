package elite.intel.gameapi;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client language decides which engine voices radio transmissions and in what language, so the two ways
 * it is learned - read off the newest journal at startup, handed over live by a session that starts afterwards
 * - both have to land on the same answer, and an install we cannot read must say so rather than guess.
 */
class GameLanguageTest {

    private static final String RUSSIAN_HEADER = header("Russian/RU");
    private static final String ENGLISH_HEADER = header("English/UK");

    private static String header(String language) {
        return "{ \"timestamp\":\"2026-07-25T05:49:56Z\", \"event\":\"Fileheader\", \"part\":1,"
                + " \"language\":\"" + language + "\", \"Odyssey\":true, \"gameversion\":\"4.4.0.3\", \"build\":\"r330683/r0 \" }";
    }

    @Test
    void aRussianClientIsReadOffTheNewestJournal(@TempDir Path dir) throws IOException {
        writeJournal(dir, "Journal.2026-07-25T054956.01.log", RUSSIAN_HEADER);
        assertEquals(Optional.of(Language.RU), new GameLanguage(() -> dir).language());
    }

    @Test
    void anEnglishClientIsEnglish(@TempDir Path dir) throws IOException {
        writeJournal(dir, "Journal.2026-07-25T054956.01.log", ENGLISH_HEADER);
        assertEquals(Optional.of(Language.EN), new GameLanguage(() -> dir).language());
    }

    /**
     * Frontier ships the client in six languages, spelled {@code <Name>/<REGION>} in the header; the name is
     * what identifies it, and the only Portuguese client is the Brazilian one.
     */
    @Test
    void everyClientLanguageFrontierShipsMapsToOneOfOurs(@TempDir Path dir) {
        GameLanguage language = new GameLanguage(() -> dir);
        language.onGameSessionStarted("English/UK");
        assertEquals(Optional.of(Language.EN), language.language());
        language.onGameSessionStarted("French/FR");
        assertEquals(Optional.of(Language.FR), language.language());
        language.onGameSessionStarted("German/DE");
        assertEquals(Optional.of(Language.DE), language.language());
        language.onGameSessionStarted("Spanish/ES");
        assertEquals(Optional.of(Language.ES), language.language());
        language.onGameSessionStarted("Portuguese/BR");
        assertEquals(Optional.of(Language.PTBZ), language.language());
        language.onGameSessionStarted("Russian/RU");
        assertEquals(Optional.of(Language.RU), language.language());
    }

    /**
     * A commander who started the app first has no journal to read; the header arrives on the bus instead, and
     * must be believed over the nothing that was read.
     */
    @Test
    void aLiveHeaderNamesTheSessionThatJustStarted(@TempDir Path dir) {
        GameLanguage language = new GameLanguage(() -> dir);
        assertTrue(language.language().isEmpty());

        language.onGameSessionStarted("Russian/RU");
        assertEquals(Optional.of(Language.RU), language.language());

        language.onGameSessionStarted("English/UK");
        assertEquals(Optional.of(Language.EN), language.language(), "relaunching in English must move the radio back");
    }

    /**
     * Unknown is unknown, not a guess: an unreadable header, a journal folder that is not there yet, or a
     * language this app does not ship, and the caller decides what stands in.
     */
    @Test
    void anUnreadableInstallIsUnknown(@TempDir Path dir) throws IOException {
        assertTrue(new GameLanguage(() -> dir).language().isEmpty(), "empty folder");
        assertTrue(new GameLanguage(() -> dir.resolve("nowhere")).language().isEmpty(), "no folder");

        writeJournal(dir, "Journal.2026-07-25T054956.01.log", "not json at all");
        assertTrue(new GameLanguage(() -> dir).language().isEmpty(), "unreadable header");

        GameLanguage language = new GameLanguage(() -> dir);
        language.onGameSessionStarted("Klingon/QO");
        assertTrue(language.language().isEmpty(), "a client language this app does not ship");
    }

    /**
     * Two journals, one session: the newest file is the one the parser follows, so it is the one that speaks
     * for the client.
     */
    @Test
    void theNewestJournalWins(@TempDir Path dir) throws IOException {
        Path older = writeJournal(dir, "Journal.2026-07-24T101010.01.log", ENGLISH_HEADER);
        Path newer = writeJournal(dir, "Journal.2026-07-25T054956.01.log", RUSSIAN_HEADER);
        Files.setLastModifiedTime(older, java.nio.file.attribute.FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(newer, java.nio.file.attribute.FileTime.fromMillis(2_000_000));

        assertEquals(Optional.of(Language.RU), new GameLanguage(() -> dir).language());
    }

    private static Path writeJournal(Path dir, String name, String header) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, header + "\n", StandardCharsets.UTF_8);
        return file;
    }
}
