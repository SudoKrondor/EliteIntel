package elite.intel.ui.event;

/**
 * Stops the service running the app's own LM Studio, if it runs, before that LM Studio is uninstalled. Handled on
 * the publishing thread, so LM Studio has stopped by the time {@code UiBus.publish} returns.
 */
public class StopLocalLlmHostEvent {
}
