package elite.intel.ai.ears;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The recorder is the only thing standing between "the app does not hear me" and a guess, so what is checked
 * here is that its numbers survive the shapes that report actually arrives in: no audio at all, audio that is
 * all silence, audio that never reaches the gate, and a session long enough to wrap the recent window.
 */
class MicLevelRecorderTest {

    private final MicLevelRecorder recorder = MicLevelRecorder.getInstance();

    /**
     * The recorder is a singleton on a process-wide bus, so every test starts by declaring a capture, which
     * is what clears the levels a previous test left behind.
     */
    private void startCapture() {
        recorder.captureStarted(MicLevelRecorder.describeCapture(
                "Chosen Mic", "Chosen Mic",
                new AudioFormat(48000, 16, 1, true, false), 9600,
                10.0, 100.0, 55.0, true));
    }

    private void frame(double rms, double gate) {
        recorder.onAudioFrame(new AudioMonitorEvent(new byte[]{1, 2}, 2, rms, 10.0, gate));
    }

    @Test
    void aCaptureThatDeliveredNothingReportsZeroFrames() {
        startCapture();

        MicLevelRecorder.Snapshot snapshot = recorder.snapshot();

        // The whole finding in one field: the line opened and produced no audio.
        assertFalse(snapshot.heardAnything());
        assertEquals(0, snapshot.frames());
        assertEquals(0, snapshot.recentFrameCount());
        assertNull(snapshot.firstFrameAt());
        assertNull(snapshot.lastGateCrossingAt());
        assertNotNull(snapshot.capture());
    }

    @Test
    void countsOnlyTheFramesThatReachedTheGate() {
        startCapture();
        frame(5, 100);
        frame(150, 100);
        frame(100, 100);   // exactly at the gate counts as reaching it
        frame(20, 100);

        MicLevelRecorder.Snapshot snapshot = recorder.snapshot();

        assertEquals(4, snapshot.frames());
        assertEquals(2, snapshot.framesOverGate());
        assertNotNull(snapshot.lastGateCrossingAt());
        assertEquals(150, snapshot.maxRms(), 1e-9);
        assertEquals(5, snapshot.minRms(), 1e-9);
    }

    @Test
    void aMicrophoneHeardButNeverLoudEnoughLeavesNoCrossing() {
        startCapture();
        for (int i = 0; i < 50; i++) frame(30, 100);

        MicLevelRecorder.Snapshot snapshot = recorder.snapshot();

        // Frames arrived, so the device is fine; nothing reached the gate, so the calibration is not.
        assertTrue(snapshot.heardAnything());
        assertEquals(0, snapshot.framesOverGate());
        assertNull(snapshot.lastGateCrossingAt());
    }

    @Test
    void theRecentWindowWrapsWhileTheRunningTotalsDoNot() {
        startCapture();
        // Loud frames first, then a full window of quiet ones: the distribution must describe the quiet
        // present, while the peak and the crossing count still describe the whole capture.
        for (int i = 0; i < 20; i++) frame(900, 100);
        for (int i = 0; i < MicLevelRecorder.RECENT_FRAMES; i++) frame(7, 100);

        MicLevelRecorder.Snapshot snapshot = recorder.snapshot();

        assertEquals(MicLevelRecorder.RECENT_FRAMES + 20, snapshot.frames());
        assertEquals(MicLevelRecorder.RECENT_FRAMES, snapshot.recentFrameCount());
        assertEquals(7, snapshot.medianRms(), 1e-9);
        assertEquals(7, snapshot.p95Rms(), 1e-9);
        assertEquals(900, snapshot.maxRms(), 1e-9, "the session peak must outlive the recent window");
        assertEquals(20, snapshot.framesOverGate());
    }

    @Test
    void aNewCaptureStartsTheLevelsOver() {
        startCapture();
        for (int i = 0; i < 10; i++) frame(900, 100);

        startCapture();

        MicLevelRecorder.Snapshot snapshot = recorder.snapshot();
        // A commander who switched device is asking about the new one; the old distribution would answer
        // for a microphone that is no longer open.
        assertEquals(0, snapshot.frames());
        assertEquals(0, snapshot.framesOverGate());
        assertEquals(0, snapshot.maxRms(), 1e-9);
        assertNull(snapshot.stoppedAt());
    }

    @Test
    void stoppingKeepsTheLevelsSoALaterBundleStillReportsThem() {
        startCapture();
        frame(150, 100);

        recorder.captureStopped();

        MicLevelRecorder.Snapshot snapshot = recorder.snapshot();
        assertNotNull(snapshot.stoppedAt());
        assertEquals(1, snapshot.frames());
        assertEquals(1, snapshot.framesOverGate());
    }

    @Test
    void aGateOfZeroCountsNoCrossings() {
        startCapture();
        // An uncalibrated install stores zero, and every frame is trivially "at or over" it. Counting those
        // would report a healthy microphone on the one setup that has never been set up.
        for (int i = 0; i < 5; i++) frame(500, 0);

        assertEquals(0, recorder.snapshot().framesOverGate());
    }

    @Test
    void describeCaptureUnpacksTheNegotiatedFormat() {
        MicLevelRecorder.Capture capture = MicLevelRecorder.describeCapture(
                "Kraken Chat", "Kraken Chat",
                new AudioFormat(44100, 24, 2, true, false), 21168,
                40.0, 45.0, 42.5, true);

        assertEquals(44100, capture.sampleRate());
        assertEquals(24, capture.sampleSizeBits());
        assertEquals(2, capture.channels());
        assertFalse(capture.bigEndian());
        // 45 is nowhere near far enough above a floor of 40; the same judgement the startup warning speaks.
        assertFalse(capture.gateClearsNoiseFloor());
    }

    @Test
    void percentileIsNearestRankAndSurvivesAnEmptyWindow() {
        assertEquals(0, MicLevelRecorder.percentile(new double[0], 0.5), 1e-9);
        assertEquals(1, MicLevelRecorder.percentile(new double[]{1}, 0.95), 1e-9);
        assertEquals(3, MicLevelRecorder.percentile(new double[]{1, 2, 3, 4, 5}, 0.50), 1e-9);
        assertEquals(5, MicLevelRecorder.percentile(new double[]{1, 2, 3, 4, 5}, 0.95), 1e-9);
    }
}
