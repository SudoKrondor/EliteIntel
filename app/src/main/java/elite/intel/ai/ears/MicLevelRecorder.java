package elite.intel.ai.ears;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.AudioMonitorBus;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.time.Instant;
import java.util.Arrays;

/**
 * The microphone's own account of itself, kept so a support bundle can answer "the app does not hear me".
 * <p>
 * That report is the one this application has no way to diagnose from its logs. Everything the commander can
 * see says the same thing whatever the cause - nothing happens when they speak - while the actual causes are
 * far apart: a saved input device that no longer exists, a line that opens but delivers silence because the
 * wrong channel is selected, a gate calibrated in a noisy room that speech never reaches, or push-to-talk
 * armed against a controller that is not plugged in. Each leaves a different fingerprint in the numbers here
 * and none of them leaves anything in the log.
 * <p>
 * <b>Levels, never audio.</b> Nothing captured is retained: the frames arrive carrying PCM and only their
 * root-mean-square amplitude is read off. A recording would answer more questions and is not ours to take -
 * it is the commander's room, it would carry whatever else was said in it, and it would dwarf the rest of the
 * bundle. The summary is a few hundred bytes and says enough.
 * <p>
 * Two halves, gathered differently. The <b>capture</b> half is what the pipeline decided when it opened the
 * microphone, handed over by {@code ParakeetSTTImpl} at the moment it decided it, because re-deriving it when
 * the bundle is written would probe the audio system afresh and could truthfully report something other than
 * what the running capture is using. The <b>level</b> half accumulates from {@link AudioMonitorEvent}, which
 * the capture loop already publishes once per frame for the meters, so this costs the loop nothing: the bus is
 * asynchronous on its own daemon thread and the work per frame is a handful of comparisons.
 */
public final class MicLevelRecorder {

    /**
     * How many recent frames the level distribution is drawn from - one minute at ~100 ms a frame.
     * <p>
     * A window rather than the whole session, because the question is always "what is the microphone doing
     * now": a commander who fixed their device an hour ago should not be diagnosed on the hour of silence
     * before it. Running totals cover the session, so nothing is lost by keeping the distribution recent.
     */
    static final int RECENT_FRAMES = 600;

    private static final MicLevelRecorder INSTANCE = new MicLevelRecorder();

    /**
     * Set once per capture, read by whichever thread writes the bundle.
     */
    private volatile Capture capture;
    private volatile Instant stoppedAt;

    /**
     * Written only by the audio-monitor bus thread and read by the bundle writer, so every read and write
     * goes through this monitor. The bus is single-threaded, so it is never contended in practice.
     */
    private final Object levelLock = new Object();
    private final double[] recentRms = new double[RECENT_FRAMES];
    private int recentIndex;
    private int recentCount;
    private long frames;
    private long framesOverGate;
    private double minRms = Double.NaN;
    private double maxRms = Double.NaN;
    private Instant firstFrameAt;
    private Instant lastFrameAt;
    private Instant lastGateCrossingAt;

    private MicLevelRecorder() {
        AudioMonitorBus.register(this);
    }

    public static MicLevelRecorder getInstance() {
        return INSTANCE;
    }

    /**
     * What the pipeline settled on when it opened the microphone.
     *
     * @param requestedDevice      the input device the commander chose, or null when they left it on the
     *                             system default; kept beside {@code resolvedDevice} because a saved name that
     *                             no longer matches any device on the machine is one of the two ways this
     *                             fails silently
     * @param resolvedDevice       the mixer actually opened, or null when the system default line was used
     * @param format               the capture format negotiated with that device
     * @param bufferSizeBytes      the line buffer, so a report can be read against the frame rate
     * @param noiseFloor           the calibrated room level
     * @param gateOpen             the level speech has to reach for the microphone to start collecting
     * @param gateClose            the derived level it falls back to before the utterance is closed
     * @param gateClearsNoiseFloor whether that gate still stands far enough above the floor to be usable,
     *                             decided by {@link AudioCalibrator#gateClearsNoiseFloor}, which is the same
     *                             judgement the startup warning speaks
     * @param calibrationWasStored whether those levels came from settings or from a calibration run now
     */
    public record Capture(
            @Nullable String requestedDevice,
            @Nullable String resolvedDevice,
            int sampleRate,
            int sampleSizeBits,
            int channels,
            boolean bigEndian,
            int bufferSizeBytes,
            double noiseFloor,
            double gateOpen,
            double gateClose,
            boolean gateClearsNoiseFloor,
            boolean calibrationWasStored,
            Instant startedAt
    ) {
    }

    /**
     * One consistent reading of both halves. Percentiles are computed here rather than by the caller so the
     * recent window is never handed out and cannot be read while the bus thread is writing into it.
     *
     * @param frames           every frame this capture has delivered, not just the recent window; zero is the
     *                         single most diagnostic value in the whole report, because it means the line
     *                         opened and produced nothing
     * @param recentFrameCount how many frames the distribution below was drawn from
     * @param framesOverGate   frames at or above the gate, over the whole capture; zero with a healthy frame
     *                         count is a microphone that is heard but never loud enough to open the gate
     */
    public record Snapshot(
            @Nullable Capture capture,
            @Nullable Instant stoppedAt,
            long frames,
            int recentFrameCount,
            double minRms,
            double medianRms,
            double p95Rms,
            double maxRms,
            long framesOverGate,
            @Nullable Instant firstFrameAt,
            @Nullable Instant lastFrameAt,
            @Nullable Instant lastGateCrossingAt
    ) {

        /**
         * Whether any audio has been seen at all, which is the first fork in reading this report.
         */
        public boolean heardAnything() {
            return frames > 0;
        }
    }

    /**
     * Records what the capture negotiated, and starts the levels over.
     * <p>
     * The levels reset because they describe one capture: a commander who changes device and restarts the
     * pipeline is asking about the new one, and carrying the old distribution across would answer for a
     * microphone that is no longer open.
     */
    public void captureStarted(Capture started) {
        synchronized (levelLock) {
            Arrays.fill(recentRms, 0);
            recentIndex = 0;
            recentCount = 0;
            frames = 0;
            framesOverGate = 0;
            minRms = Double.NaN;
            maxRms = Double.NaN;
            firstFrameAt = null;
            lastFrameAt = null;
            lastGateCrossingAt = null;
        }
        stoppedAt = null;
        capture = started;
    }

    /**
     * Marks the capture closed while keeping the levels, so a bundle saved after the pipeline was stopped
     * still reports what the microphone was doing while it ran.
     */
    public void captureStopped() {
        stoppedAt = Instant.now();
    }

    /**
     * Reads one frame's amplitude and drops the frame.
     * <p>
     * The gate is taken from the event rather than from {@link #capture} so the crossing count cannot drift
     * from the threshold the capture loop actually compared against on that frame.
     */
    @Subscribe
    public void onAudioFrame(AudioMonitorEvent event) {
        double rms = event.getRms();
        double gate = event.getRmsHigh();
        Instant now = Instant.now();
        synchronized (levelLock) {
            frames++;
            if (firstFrameAt == null) firstFrameAt = now;
            lastFrameAt = now;
            if (Double.isNaN(minRms) || rms < minRms) minRms = rms;
            if (Double.isNaN(maxRms) || rms > maxRms) maxRms = rms;
            if (gate > 0 && rms >= gate) {
                framesOverGate++;
                lastGateCrossingAt = now;
            }
            recentRms[recentIndex] = rms;
            recentIndex = (recentIndex + 1) % RECENT_FRAMES;
            if (recentCount < RECENT_FRAMES) recentCount++;
        }
    }

    public Snapshot snapshot() {
        double[] recent;
        long seenFrames;
        long overGate;
        double min;
        double max;
        Instant first;
        Instant last;
        Instant crossing;
        synchronized (levelLock) {
            recent = Arrays.copyOf(recentRms, recentCount);
            seenFrames = frames;
            overGate = framesOverGate;
            min = minRms;
            max = maxRms;
            first = firstFrameAt;
            last = lastFrameAt;
            crossing = lastGateCrossingAt;
        }
        Arrays.sort(recent);
        return new Snapshot(
                capture, stoppedAt, seenFrames, recent.length,
                Double.isNaN(min) ? 0 : min,
                percentile(recent, 0.50), percentile(recent, 0.95),
                Double.isNaN(max) ? 0 : max,
                overGate, first, last, crossing);
    }

    /**
     * The value at {@code fraction} through an ascending array, or zero when it is empty. Nearest-rank rather
     * than interpolated: these are amplitudes read to describe a microphone, not measurements to compare.
     */
    static double percentile(double[] ascending, double fraction) {
        if (ascending.length == 0) return 0;
        int index = (int) Math.round(fraction * (ascending.length - 1));
        return ascending[Math.min(Math.max(index, 0), ascending.length - 1)];
    }

    /**
     * Builds the capture half from the format the detector negotiated, so the field-by-field unpacking lives
     * next to the record rather than in the capture loop.
     */
    public static Capture describeCapture(
            @Nullable String requestedDevice,
            @Nullable String resolvedDevice,
            AudioFormat format,
            int bufferSizeBytes,
            double noiseFloor,
            double gateOpen,
            double gateClose,
            boolean calibrationWasStored
    ) {
        return new Capture(
                requestedDevice,
                resolvedDevice,
                (int) format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.getChannels(),
                format.isBigEndian(),
                bufferSizeBytes,
                noiseFloor,
                gateOpen,
                gateClose,
                AudioCalibrator.gateClearsNoiseFloor(noiseFloor, gateOpen),
                calibrationWasStored,
                Instant.now());
    }
}
