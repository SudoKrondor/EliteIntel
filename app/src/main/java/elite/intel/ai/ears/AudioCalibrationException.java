package elite.intel.ai.ears;

/**
 * Thrown when audio calibration cannot complete because the input device
 * could not be opened or produced no audio. Signals a hard failure so the
 * caller can announce it and avoid persisting invalid (zero) thresholds.
 */
public class AudioCalibrationException extends RuntimeException {
    public AudioCalibrationException(String message) {
        super(message);
    }

    public AudioCalibrationException(String message, Throwable cause) {
        super(message, cause);
    }
}