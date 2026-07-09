package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.eventbus.GameEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Default {@link CustomCommandSpeakExecutor}: voices a custom-command SPEAK step through the companion speech
 * gateway and blocks until the gateway reports playback finished, or until the 30-second guard timeout elapses.
 * The companion owns speech, so this no longer detours through {@code AiVoxResponseEvent} (a system-only channel).
 * <p>
 * Publishes {@link IsSpeakingEvent}{@code (true)} before speech begins and {@link IsSpeakingEvent}{@code (false)}
 * in a {@code finally} block so STT is suppressed for the full duration of playback, even on exception/timeout.
 */
class SynchronousCustomCommandSpeech implements CustomCommandSpeakExecutor {

    static final CustomCommandSpeakExecutor DEFAULT = new SynchronousCustomCommandSpeech();

    private static final Logger log = LogManager.getLogger(SynchronousCustomCommandSpeech.class);
    private static final int TIMEOUT_SECONDS = 30;

    private SynchronousCustomCommandSpeech() {}

    @Override
    public void speak(String text) throws InterruptedException {
        GameEventBus.publish(new IsSpeakingEvent(true));
        try {
            // The companion speech gateway's future completes when playback of this request ends, so blocking on
            // it makes the SPEAK step wait exactly until the line has been spoken.
            CompanionRuntime.speech()
                    .submit(new SpeechRequest(UUID.randomUUID().toString(), text, Urgency.NORMAL))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("CustomCommand SPEAK timed out after {}s for: '{}'", TIMEOUT_SECONDS, text);
        } catch (ExecutionException e) {
            log.warn("CustomCommand SPEAK completed exceptionally: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        } finally {
            GameEventBus.publish(new IsSpeakingEvent(false));
        }
        // InterruptedException propagates to CustomCommandHandler, which will interrupt custom command execution
    }
}
