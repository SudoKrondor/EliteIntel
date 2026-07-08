package elite.intel.ui.widget;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Locks in the two SYSTEM_LOG timestamp forms so the panel-vs-export distinction cannot silently regress:
 * the panel shows local {@code HH:mm:ss} while the exported transcript uses the UTC journal form.
 */
class LogTimestampFormatTest {

    private static final Instant SAMPLE = Instant.parse("2026-07-04T21:31:23Z");

    @Test
    void fileUsesUtcJournalForm() {
        // Fixed UTC regardless of the machine's zone, so a saved log matches the journal's timestamps.
        assertEquals("2026-07-04T21:31:23Z", LogTimestampFormat.file(SAMPLE));
    }

    @Test
    void screenIsLocalTimeOfDayOnly() {
        String expected = LocalTime.ofInstant(SAMPLE, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String screen = LogTimestampFormat.screen(SAMPLE);
        assertEquals(expected, screen);
        // Guard the distinction: the panel form is a bare local time, never the journal's dated UTC form.
        assertFalse(screen.contains("T") || screen.contains("Z"),
                "the panel timestamp must be local time-of-day, not the UTC journal form");
    }
}
