package elite.intel.gameapi;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A service restart builds a new parser; starting it at byte 0 re-published every event since app start.
 */
class JournalReadMarkTest {

    private static final Path JOURNAL = Path.of("Journal.2026-10-18T163428.01.log");

    @Test
    void aRestartResumesTheSameJournalWhereTheLastParserStopped() {
        assertEquals(68401, JournalReadMark.startPosition(new JournalReadMark(JOURNAL, 68401), JOURNAL));
    }

    @Test
    void aJournalTheLastParserNeverReadStartsFromTheTop() {
        Path newer = Path.of("Journal.2026-10-18T190000.01.log");
        assertEquals(0, JournalReadMark.startPosition(new JournalReadMark(JOURNAL, 68401), newer));
    }

    @Test
    void theFirstParserStartsFromTheTop() {
        assertEquals(0, JournalReadMark.startPosition(null, JOURNAL));
    }
}
