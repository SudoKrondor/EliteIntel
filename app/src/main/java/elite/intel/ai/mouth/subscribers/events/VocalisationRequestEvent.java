package elite.intel.ai.mouth.subscribers.events;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class VocalisationRequestEvent extends BaseVoxEvent {

    private final Class<? extends BaseVoxEvent> originType;
    private final String voiceName;
    private boolean canBeInterrupted;
    private final boolean isRadio;
    /**
     * Non-null only when a caller (e.g. customCommand SPEAK) needs to block until playback finishes.
     * The TTS implementation must call {@link CompletableFuture#complete} after the last sentence plays.
     */
    @Nullable private final CompletableFuture<Void> completionFuture;

    public VocalisationRequestEvent(String textToVoice, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted) {
        this(textToVoice, null, originType, canBeInterrupted, false, null);
    }

    public VocalisationRequestEvent(String textToVoice, String voiceName, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted) {
        this(textToVoice, voiceName, originType, canBeInterrupted, false, null);
    }

    public VocalisationRequestEvent(String textToVoice, String voiceName, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted, boolean isRadio) {
        this(textToVoice, voiceName, originType, canBeInterrupted, isRadio, null);
    }

    /**
     * Used when a completion signal is required (e.g. when routing a customCommand SPEAK request).
     */
    public VocalisationRequestEvent(String textToVoice, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted, @Nullable CompletableFuture<Void> completionFuture) {
        this(textToVoice, null, originType, canBeInterrupted, false, completionFuture);
    }

    private VocalisationRequestEvent(String textToVoice, String voiceName, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted, boolean isRadio, @Nullable CompletableFuture<Void> completionFuture) {
        super(textToVoice, false);
        this.voiceName = voiceName;
        this.originType = originType;
        this.canBeInterrupted = canBeInterrupted;
        this.isRadio = isRadio;
        this.completionFuture = completionFuture;
    }

    public Class<? extends BaseVoxEvent> getOriginType() {
        return originType;
    }

    public boolean canBeInterrupted() {
        return canBeInterrupted;
    }

    /**
     * Voice name (enum name). Null means use the session default.
     */
    public String getVoiceName() {
        return voiceName;
    }

    /**
     * True when this vocalisation should be processed through the radio transmission filter.
     */
    public boolean isRadio() {
        return isRadio;
    }

    @Nullable
    public CompletableFuture<Void> getCompletionFuture() {
        return completionFuture;
    }
}
