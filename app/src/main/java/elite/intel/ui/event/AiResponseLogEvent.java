package elite.intel.ui.event;

public class AiResponseLogEvent {
    private String data;

    public AiResponseLogEvent(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
