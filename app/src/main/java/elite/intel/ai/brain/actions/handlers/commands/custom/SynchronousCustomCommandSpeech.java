package elite.intel.ai.brain.actions.handlers.commands.custom;

import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
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
 * Speaking state belongs to the active Mouth; this executor only waits on the request's completion handle.
 */
class SynchronousCustomCommandSpeech implements CustomCommandSpeakExecutor {

    static final CustomCommandSpeakExecutor DEFAULT = new SynchronousCustomCommandSpeech();

    private static final Logger log = LogManager.getLogger(SynchronousCustomCommandSpeech.class);
    private static final int TIMEOUT_SECONDS = 30;

    private SynchronousCustomCommandSpeech() {}

    @Override
    public void speak(String text) throws InterruptedException {
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
        }
        // InterruptedException propagates to CustomCommandHandler, which will interrupt custom command execution
    }
}
