package elite.intel.ai.mouth;

/**
 * Coordinates the always-on Kokoro radio engine with the legacy main mouth so a radio transmission
 * ducks behind normal AI speech instead of playing over it (the "radio ducks/waits" behaviour).
 * <p>
 * The main mouth (Google, or Kokoro in its MAIN role) brackets each sentence's playback with
 * {@link #begin()}/{@link #end()}. The radio-role Kokoro engine calls {@link #awaitIdle(long)} before
 * playing, so it waits out the current main-voice sentence and then speaks. When Kokoro itself is the
 * main mouth there is a single engine and radio is serialised through its own queue, so the gate is
 * never contended.
 * <p>
 * Companion speech reaches the same active Mouth through {@code VocalisationRequestEvent}, so it is bracketed
 * here as main-voice playback too. A dedicated radio-role Kokoro engine therefore waits behind both system and
 * companion speech.
 */
public final class MainVoicePlaybackGate {

    /**
     * How long a radio transmission waits out ongoing main-voice speech before it plays anyway.
     */
    private static final long RADIO_MAIN_WAIT_MS = 15_000;

    private static final Object LOCK = new Object();
    private static int active = 0;

    private MainVoicePlaybackGate() {
    }

    /**
     * Marks the start of a main-voice playback segment.
     */
    public static void begin() {
        synchronized (LOCK) {
            active++;
        }
    }

    /**
     * Marks the end of a main-voice playback segment and wakes any waiting radio playback.
     */
    public static void end() {
        synchronized (LOCK) {
            if (active > 0) active--;
            LOCK.notifyAll();
        }
    }

    /**
     * Blocks a radio-role engine until the main voice falls silent, capped so a continuously speaking main
     * voice cannot strand a transmission indefinitely. Every radio engine ducks by the same rule, so the cap
     * lives here rather than in each of them.
     */
    public static void awaitIdleForRadio() {
        awaitIdle(RADIO_MAIN_WAIT_MS);
    }

    private static void awaitIdle(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (LOCK) {
            while (active > 0) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) return;
                try {
                    LOCK.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
