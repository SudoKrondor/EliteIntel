package elite.intel.ui.event;

import java.time.LocalTime;

public class AppLogEvent {

    private final LocalTime timestamp;
    private final String data;

    /** Captures current time automatically; producers pass only the message text. */
    public AppLogEvent(String data) {
        this.timestamp = LocalTime.now();
        this.data = data;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }

    public String getData() {
        return data;
    }
}
