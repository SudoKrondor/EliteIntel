package elite.intel.ui.event;

import elite.intel.util.StringUtls;

public class AiResponseLogEvent {
    private final String data;

    public AiResponseLogEvent(String data) {
        // Format the UI copy only; TTS keeps its ungrouped numeric representation.
        this.data = StringUtls.formatNumbersForDisplay(data);
    }

    public String getData() {
        return data;
    }
}
