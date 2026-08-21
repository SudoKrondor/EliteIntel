package elite.intel.ui.event;

/**
 * The commander's own instruction to open or close the Sleep/Wake gate, published by the AI tab button.
 * <p>
 * Separate from {@link SleepWakeStateChangedEvent} on purpose: this is the request, and only
 * {@code AppController} answers it — persisting the new state, saying it out loud, and then announcing the
 * result. Views ask; they do not write the setting themselves.
 *
 * @param sleeping the state being asked for: {@code true} closes the gate, {@code false} opens it
 */
public record ToggleSleepWakeEvent(boolean sleeping) {
}
