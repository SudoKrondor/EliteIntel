package elite.intel.ui.event;

public class ServicesStateEvent {

    public enum State {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING
    }

    private final State state;

    public ServicesStateEvent(State state) {
        this.state = state;
    }

    /** Convenience for the common "are services fully up?" check. */
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public State state() {
        return state;
    }
}
