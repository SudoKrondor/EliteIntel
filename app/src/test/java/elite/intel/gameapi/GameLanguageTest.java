package elite.intel.gameapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client language decides whether radio transmissions can be voiced at all, so the two ways it is learned
 * - read off the newest journal at startup, handed over live by a session that starts afterwards - both have
 * to land on the same answer, and an install we cannot read must not silence a channel that works.
 */
class GameLanguageTest {

    private static final String RUSSIAN_HEADER =
            "{ \"timestamp\":\"2026-07-25T05:49:56Z\", \"event\":\"Fileheader\", \"part\":1,"
                    + " \"language\":\"Russian/RU\", \"Odyssey\":true, \"gameversion\":\"4.4.0.3\", \"build\":\"r330683/r0 \" }";
    private static final String ENGLISH_HEADER =
            "{ \"timestamp\":\"2026-07-25T05:49:56Z\", \"event\":\"Fileheader\", \"part\":1,"
                    + " \"language\":\"English/UK\", \"Odyssey\":true, \"gameversion\":\"4.4.0.3\", \"build\":\"r330683/r0 \" }";

    @Test
    void aRussianClientIsReadOffTheNewestJournal(@TempDir Path dir) throws IOException {
        writeJournal(dir, "Journal.2026-07-25T054956.01.log", RUSSIAN_HEADER);
        assertTrue(new GameLanguage(() -> dir).isCyrillicScript());
    }

    @Test
    void anEnglishClientIsNotCyrillic(@TempDir Path dir) throws IOException {
        writeJournal(dir, "Journal.2026-07-25T054956.01.log", ENGLISH_HEADER);
        assertFalse(new GameLanguage(() -> dir).isCyrillicScript());
    }

    /**
     * A commander who started the app first has no journal to read; the header arrives on the bus instead, and
     * must be believed over the nothing that was read.
     */
    @Test
    void aLiveHeaderNamesTheSessionThatJustStarted(@TempDir Path dir) {
        GameLanguage language = new GameLanguage(() -> dir);
        assertFalse(language.isCyrillicScript());

        language.onGameSessionStarted("Russian/RU");
        assertTrue(language.isCyrillicScript());

        language.onGameSessionStarted("English/UK");
        assertFalse(language.isCyrillicScript(), "relaunching in English must give the radio back");
    }

    /**
     * Unknown is not Cyrillic: an unreadable header, or a journal folder that is not there yet, must leave the
     * radio exactly as the commander set it.
     */
    @Test
    void anUnreadableInstallLeavesTheRadioAlone(@TempDir Path dir) throws IOException {
        assertFalse(new GameLanguage(() -> dir).isCyrillicScript(), "empty folder");
        assertFalse(new GameLanguage(() -> dir.resolve("nowhere")).isCyrillicScript(), "no folder");

        writeJournal(dir, "Journal.2026-07-25T054956.01.log", "not json at all");
        assertFalse(new GameLanguage(() -> dir).isCyrillicScript(), "unreadable header");
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

        assertTrue(new GameLanguage(() -> dir).isCyrillicScript());
    }

    private static Path writeJournal(Path dir, String name, String header) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, header + "\n", StandardCharsets.UTF_8);
        return file;
    }
}
