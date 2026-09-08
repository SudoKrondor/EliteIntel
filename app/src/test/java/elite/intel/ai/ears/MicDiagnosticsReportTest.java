package elite.intel.ai.ears;

import elite.intel.ai.ears.MicDiagnosticsReport.VoiceGate;
import elite.intel.devices.model.Device;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The report exists to be read by whoever answers a bug report, so what is checked here is that each failure
 * shape reaches that reader as its own verdict. The five below are the five different things "the app does not
 * hear me" turns out to be, and they are indistinguishable from the commander's chair.
 */
class MicDiagnosticsReportTest {

    private static final List<String> ONE_DEVICE = List.of("Chosen Mic");
    private static final VoiceGate HANDS_FREE = new VoiceGate(false, null, 0, false);

    private static MicLevelRecorder.Capture capture(String requested, String resolved, boolean gateClears) {
        return new MicLevelRecorder.Capture(
                requested, resolved, 48000, 16, 1, false, 9600,
                10.0, gateClears ? 100.0 : 11.0, 55.0, gateClears, true, Instant.EPOCH);
    }

    private static MicLevelRecorder.Snapshot snapshot(
            MicLevelRecorder.Capture capture, long frames, double maxRms, long framesOverGate) {
        return new MicLevelRecorder.Snapshot(
                capture, null, frames, (int) Math.min(frames, MicLevelRecorder.RECENT_FRAMES),
                0, maxRms / 2, maxRms, maxRms, framesOverGate,
                frames == 0 ? null : Instant.EPOCH, frames == 0 ? null : Instant.EPOCH,
                framesOverGate == 0 ? null : Instant.EPOCH);
    }

    private static String verdictOf(String report) {
        return report.lines().filter(line -> line.startsWith("Verdict:")).findFirst().orElseThrow();
    }

    @Test
    void saysSoWhenVoiceInputNeverRan() {
        String report = MicDiagnosticsReport.render(
                snapshot(null, 0, 0, 0), ONE_DEVICE, HANDS_FREE, List.of());

        assertTrue(verdictOf(report).contains("has not been started"));
        // No gate section: there is no calibration to report, and printing zeros would invite a reader to
        // diagnose a capture that never happened.
        assertFalse(report.contains("Gate ("));
    }

    @Test
    void aLineThatDeliveredNoFramesIsTheHeadline() {
        String report = MicDiagnosticsReport.render(
                snapshot(capture("Chosen Mic", "Chosen Mic", true), 0, 0, 0),
                ONE_DEVICE, HANDS_FREE, List.of());

        assertTrue(verdictOf(report).contains("delivered no audio at all"));
        assertTrue(report.contains("*** no audio frames were delivered"));
    }

    @Test
    void framesOfPureSilenceReadAsAMutedOrWrongInput() {
        String report = MicDiagnosticsReport.render(
                snapshot(capture("Chosen Mic", "Chosen Mic", true), 4000, 0.0, 0),
                ONE_DEVICE, HANDS_FREE, List.of());

        assertTrue(verdictOf(report).contains("every frame is silent"));
        assertTrue(report.contains("*** every frame is silence"));
        // Silence outranks the gate: recalibrating a line that carries nothing fixes nothing.
        assertFalse(verdictOf(report).contains("reached the gate"));
    }

    @Test
    void audioThatNeverReachesTheGatePointsAtCalibration() {
        String report = MicDiagnosticsReport.render(
                snapshot(capture("Chosen Mic", "Chosen Mic", true), 4000, 40.0, 0),
                ONE_DEVICE, HANDS_FREE, List.of());

        assertTrue(verdictOf(report).contains("no frame ever reached the gate"));
        assertTrue(report.contains("Last time the gate was reached: never"));
    }

    @Test
    void aSavedDeviceThatIsGoneIsCalledOutRatherThanLeftToBeCompared() {
        String report = MicDiagnosticsReport.render(
                snapshot(capture("Unplugged Headset", null, true), 4000, 900.0, 120),
                List.of("Built-in Microphone"), HANDS_FREE, List.of());

        assertTrue(report.contains("*** the chosen device was not found"));
        assertTrue(report.contains("Chosen in settings: Unplugged Headset"));
        assertTrue(report.contains("Built-in Microphone"), "the devices present now must be listed");
    }

    @Test
    void aGateTooCloseToTheNoiseFloorIsReportedEvenWhenSpeechGetsThrough() {
        String report = MicDiagnosticsReport.render(
                snapshot(capture("Chosen Mic", "Chosen Mic", false), 4000, 900.0, 120),
                ONE_DEVICE, HANDS_FREE, List.of());

        assertTrue(verdictOf(report).contains("too close to the room noise"));
        assertTrue(report.contains("*** the gate does not clear the noise floor"));
    }

    @Test
    void aHealthyMicrophoneUnderPushToTalkPointsAtTheButtonNotTheAudio() {
        String report = MicDiagnosticsReport.render(
                snapshot(capture("Chosen Mic", "Chosen Mic", true), 4000, 900.0, 120),
                ONE_DEVICE, new VoiceGate(true, "Missing HOTAS", 7, false),
                List.of(new Device(1, "Some Other Stick", 4, 12, "usb-1", "guid-1")));

        assertTrue(verdictOf(report).contains("push-to-talk is the gate"));
        assertTrue(report.contains("*** that controller is not connected"));
        assertTrue(report.contains("Button:     7"));
        assertTrue(report.contains("Some Other Stick (12 buttons)"));
    }

    @Test
    void aHealthyHandsFreeMicrophoneSaysSoAndNamesSleep() {
        String report = MicDiagnosticsReport.render(
                snapshot(capture("Chosen Mic", "Chosen Mic", true), 4000, 900.0, 120),
                ONE_DEVICE, new VoiceGate(false, null, 0, true), List.of());

        assertTrue(verdictOf(report).contains("healthy"));
        assertTrue(report.contains("currently asleep"));
    }

    @Test
    void neverCarriesAudio() {
        String report = MicDiagnosticsReport.render(
                snapshot(capture("Chosen Mic", "Chosen Mic", true), 4000, 900.0, 120),
                ONE_DEVICE, HANDS_FREE, List.of());

        assertTrue(report.contains("no audio is recorded or included"));
        // The whole report is a few hundred bytes of numbers; a regression that started attaching samples
        // would show up here long before it showed up as a complaint about bundle size.
        assertTrue(report.length() < 4096, "the report must stay a summary: " + report.length() + " bytes");
    }
}
