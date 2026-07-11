package elite.intel.ai.mouth.subscribers.events;

import elite.intel.ai.mouth.VocalisationHandle;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public class VocalisationRequestEvent extends BaseVoxEvent {

    private final Class<? extends BaseVoxEvent> originType;
    private final String voiceName;
    private final boolean canBeInterrupted;
    private final boolean isRadio;
    private final VocalisationHandle handle;

    public VocalisationRequestEvent(String textToVoice, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted) {
        this(UUID.randomUUID().toString(), textToVoice, null, originType, canBeInterrupted, false, null);
    }

    public VocalisationRequestEvent(String textToVoice, String voiceName, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted) {
        this(UUID.randomUUID().toString(), textToVoice, voiceName, originType, canBeInterrupted, false, null);
    }

    public VocalisationRequestEvent(String textToVoice, String voiceName, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted, boolean isRadio) {
        this(UUID.randomUUID().toString(), textToVoice, voiceName, originType, canBeInterrupted, isRadio, null);
    }

    /**
     * Used when a completion signal is required (e.g. when routing a customCommand SPEAK request).
     */
    public VocalisationRequestEvent(String textToVoice, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted, @Nullable CompletableFuture<Void> completionFuture) {
        this(UUID.randomUUID().toString(), textToVoice, null, originType, canBeInterrupted, false, completionFuture);
    }

    /** Creates a tracked companion request while preserving its correlation id through the Mouth pipeline. */
    public static VocalisationRequestEvent tracked(
            String requestId,
            String textToVoice,
            Class<? extends BaseVoxEvent> originType,
            boolean canBeInterrupted,
            CompletableFuture<Void> completionFuture
    ) {
        return new VocalisationRequestEvent(
                requestId, textToVoice, null, originType, canBeInterrupted, false, completionFuture);
    }

    private VocalisationRequestEvent(
            String requestId,
            String textToVoice,
            String voiceName,
            Class<? extends BaseVoxEvent> originType,
            boolean canBeInterrupted,
            boolean isRadio,
            @Nullable CompletableFuture<Void> completionFuture
    ) {
        super(textToVoice, false);
        this.voiceName = voiceName;
        this.originType = originType;
        this.canBeInterrupted = canBeInterrupted;
        this.isRadio = isRadio;
        this.handle = new VocalisationHandle(requestId, canBeInterrupted, completionFuture);
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

    /** The request-scoped lifecycle claimed and settled by the active Mouth. */
    public VocalisationHandle handle() {
        return handle;
    }

    public CompletableFuture<Void> getCompletionFuture() {
        return handle.completion();
    }
}
