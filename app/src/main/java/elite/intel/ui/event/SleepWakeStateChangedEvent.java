package elite.intel.ui.event;

/**
 * Published whenever the sleep/wake gate changes.
 * {@code sleeping = true} means the voice gate is closed (app is sleeping);
 * {@code sleeping = false} means the gate is open and the app is listening.
 */
public record SleepWakeStateChangedEvent(boolean sleeping) {}
