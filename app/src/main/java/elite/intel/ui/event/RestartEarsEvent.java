package elite.intel.ui.event;

/**
 * Requests a restart of just the STT (EARS) service. Published when a setting that only affects the
 * speech-input pipeline changes at runtime - e.g. toggling push-to-talk on/off - so the change takes
 * effect without a full service rebuild.
 */
public class RestartEarsEvent {
}