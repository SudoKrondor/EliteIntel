package elite.intel.ui.event;

public class PttModeChangedEvent {

    private final boolean active;

    public PttModeChangedEvent(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
