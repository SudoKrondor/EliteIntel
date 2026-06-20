package elite.intel.ui.event;

@SuppressWarnings("ClassCanBeRecord")
public class PttButtonStateEvent {

    private final boolean held;

    public PttButtonStateEvent(boolean held) {
        this.held = held;
    }

    public boolean isHeld() {
        return held;
    }
}