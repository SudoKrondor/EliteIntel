package elite.intel.ui.event;

/**
 * A calibration asked for with {@link RecalibrateAudioEvent} is over, whether it succeeded, failed or never
 * got to run, and voice input has been restarted if it was running.
 */
public record AudioCalibrationFinishedEvent() {
}
