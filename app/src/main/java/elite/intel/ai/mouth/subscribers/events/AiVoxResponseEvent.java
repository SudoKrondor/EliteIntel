package elite.intel.ai.mouth.subscribers.events;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class AiVoxResponseEvent extends BaseVoxEvent {

    @Nullable private final CompletableFuture<Void> completionFuture;

    public AiVoxResponseEvent(String textToVoice) {
        this(textToVoice, null);
    }

    /**
     * Carries a completion signal; the TTS pipeline must complete {@code completionFuture} after
     * audio playback finishes. Used by customCommand SPEAK steps to block until TTS is done.
     */
    public AiVoxResponseEvent(String textToVoice, @Nullable CompletableFuture<Void> completionFuture) {
        super(textToVoice, false);
        this.completionFuture = completionFuture;
    }

    @Nullable
    public CompletableFuture<Void> getCompletionFuture() {
        return completionFuture;
    }
}
