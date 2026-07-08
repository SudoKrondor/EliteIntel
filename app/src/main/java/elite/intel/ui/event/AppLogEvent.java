package elite.intel.ui.event;

import java.time.Instant;

public class AppLogEvent {

    private final Instant timestamp;
    private final String data;

    /**
     * Captures the current instant automatically; producers pass only the message text. The instant is rendered
     * as local {@code HH:mm:ss} in the SYSTEM LOG panel and as a UTC (journal-style) timestamp when the log is
     * exported to a file, so a saved log lines up line-by-line with the game journal for debugging.
     */
    public AppLogEvent(String data) {
        this.timestamp = Instant.now();
        this.data = data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getData() {
        return data;
    }
}
