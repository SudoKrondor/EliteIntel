package elite.intel.ui.widget;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formats a SYSTEM_LOG entry's {@link Instant} timestamp two ways: the local wall-clock {@code HH:mm:ss} shown
 * in the panel (unchanged from what the app has always displayed), and the game journal's ISO-8601 UTC form
 * ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) written to the exported transcript so a saved log correlates line-by-line
 * with the running journal file for debugging. Pure static helpers so the two forms can be unit-tested without
 * standing up the {@link HudLogArea} Swing widget.
 */
final class LogTimestampFormat {

    private static final DateTimeFormatter SCREEN_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    // UTC is fixed, so binding the zone once here is safe (unlike the local screen form, which resolves the
    // default zone per call so it tracks a runtime zone/DST change).
    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private LogTimestampFormat() {
    }

    /** Local wall-clock {@code HH:mm:ss} for the on-screen panel; resolves the default zone per call. */
    static String screen(Instant timestamp) {
        return SCREEN_FMT.format(LocalTime.ofInstant(timestamp, ZoneId.systemDefault()));
    }

    /** UTC {@code yyyy-MM-dd'T'HH:mm:ss'Z'} journal form for the exported transcript. */
    static String file(Instant timestamp) {
        return FILE_FMT.format(timestamp);
    }
}
