package elite.intel.diagnostics;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.eventbus.GameEventBus;

/**
 * Paces file-injected phrases to conversation speed so the companion LLM and TTS finish a turn before the
 * next phrase is fed - if phrases were dumped in faster than the model can answer, the run would not reflect
 * real use. Tracks TTS state via {@link IsSpeakingEvent} and turn activity via {@link #markActivity()}
 * (pinged by {@link DiagnosticsExecutionGateway} on each dispatched tool, so a silent command turn still
 * signals progress). {@link #awaitTurnSettled()} then blocks the tailer until the turn has both started and
 * gone quiet.
 * <p>
 * Singleton because the boot-time tailer and the companion-time execution gateway are created in different
 * places yet must share one turn state.
 */
public final class DiagnosticsPacer {

    private static final long POLL_MS = 100;
    private static final long START_GRACE_MS = 15_000; // max wait for a (possibly cold-LLM) turn to begin
    private static final long TURN_TIMEOUT_MS = 90_000; // hard cap so a stuck turn cannot hang the tailer
    private static final long QUIET_MS = 500; // short settle after TTS stops before the next phrase - keep gaps tight

    private static final DiagnosticsPacer INSTANCE = new DiagnosticsPacer();

    private volatile boolean speaking;
    private volatile long lastActivityAt;
    private volatile long injectedAt;

    private DiagnosticsPacer() {
    }

    public static DiagnosticsPacer getInstance() {
        return INSTANCE;
    }

    /** Subscribes to the speech-state stream; call once at startup in diagnostics mode. */
    public void start() {
        GameEventBus.register(this);
    }

    @Subscribe
    public void onSpeaking(IsSpeakingEvent event) {
        speaking = event.isSpeaking();
        lastActivityAt = System.currentTimeMillis();
    }

    /** Records that a tool was dispatched this turn, so a non-speaking turn still counts as activity. */
    public void markActivity() {
        lastActivityAt = System.currentTimeMillis();
    }

    /** Marks the moment a phrase was injected; the start grace and settle window are measured from here. */
    public void markInjected() {
        injectedAt = System.currentTimeMillis();
        lastActivityAt = injectedAt;
    }

    /**
     * Blocks until the turn started by the last injected phrase has begun (speech or a dispatch after
     * injection, else the {@link #START_GRACE_MS} grace) and then stayed quiet for {@link #QUIET_MS}
     * (no speech, no dispatch), bounded overall by {@link #TURN_TIMEOUT_MS}.
     */
    public void awaitTurnSettled() {
        long startBy = injectedAt + START_GRACE_MS;
        // Phase 1: wait for the turn to start, or give up after the grace (mirrors the routing harness).
        while (!speaking && lastActivityAt <= injectedAt && System.currentTimeMillis() < startBy) {
            if (!sleep()) return;
        }
        // Phase 2: wait for the turn to finish - not speaking and quiet long enough - or hit the hard timeout.
        long deadline = System.currentTimeMillis() + TURN_TIMEOUT_MS;
        while ((speaking || System.currentTimeMillis() - lastActivityAt < QUIET_MS)
                && System.currentTimeMillis() < deadline) {
            if (!sleep()) return;
        }
    }

    /** Sleeps one poll interval; returns false if the thread was interrupted (caller should stop waiting). */
    private boolean sleep() {
        try {
            Thread.sleep(POLL_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
