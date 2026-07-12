package elite.intel.diagnostics;

import elite.intel.ai.ears.EarsInterface;

/**
 * Diagnostics-mode {@link EarsInterface}: a no-op speech-to-text. Diagnostics feeds commander input from a
 * file, never the microphone, so the real STT is pure startup cost — loading the Parakeet model is a large
 * chunk of launch time. Skipping it makes the app come up much faster. Kept as a real service (not omitted) so
 * the controller's EARS lookups/restarts stay type-safe.
 */
public final class DiagnosticsEars implements EarsInterface {

    @Override
    public void start() {
        // No STT in diagnostics: commander input comes from the diagnostics input file.
    }

    @Override
    public void stop() {
        // Nothing to stop.
    }
}
