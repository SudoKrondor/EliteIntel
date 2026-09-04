package elite.intel.ai.mouth.subscribers.events;

import elite.intel.ai.mouth.VocalisationHandle;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VocalisationRequestEvent extends BaseVoxEvent {

    private final Class<? extends BaseVoxEvent> originType;
    private final String voiceName;
    private final boolean canBeInterrupted;
    private final boolean isRadio;
    private final String speaker;
    private final Set<String> reservedVoices;
    private final VocalisationHandle handle;

    public VocalisationRequestEvent(String textToVoice, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted) {
        this(UUID.randomUUID().toString(), textToVoice, null, originType, canBeInterrupted, false, null, null, Set.of());
    }

    public VocalisationRequestEvent(String textToVoice, String voiceName, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted) {
        this(UUID.randomUUID().toString(), textToVoice, voiceName, originType, canBeInterrupted, false, null, null, Set.of());
    }

    public VocalisationRequestEvent(String textToVoice, String voiceName, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted, boolean isRadio, @Nullable String speaker) {
        this(textToVoice, voiceName, originType, canBeInterrupted, isRadio, speaker, Set.of());
    }

    public VocalisationRequestEvent(String textToVoice, String voiceName, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted, boolean isRadio, @Nullable String speaker, Set<String> reservedVoices) {
        this(UUID.randomUUID().toString(), textToVoice, voiceName, originType, canBeInterrupted, isRadio, speaker, null, reservedVoices);
    }

    /**
     * Used when a completion signal is required (e.g. when routing a customCommand SPEAK request).
     */
    public VocalisationRequestEvent(String textToVoice, Class<? extends BaseVoxEvent> originType, boolean canBeInterrupted, @Nullable CompletableFuture<Void> completionFuture) {
        this(UUID.randomUUID().toString(), textToVoice, null, originType, canBeInterrupted, false, null, completionFuture, Set.of());
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
                requestId, textToVoice, null, originType, canBeInterrupted, false, null, completionFuture, Set.of());
    }

    private VocalisationRequestEvent(
            String requestId,
            String textToVoice,
            String voiceName,
            Class<? extends BaseVoxEvent> originType,
            boolean canBeInterrupted,
            boolean isRadio,
            @Nullable String speaker,
            @Nullable CompletableFuture<Void> completionFuture,
            Set<String> reservedVoices
    ) {
        super(textToVoice, false);
        this.voiceName = voiceName;
        this.reservedVoices = reservedVoices;
        this.originType = originType;
        this.canBeInterrupted = canBeInterrupted;
        this.isRadio = isRadio;
        this.speaker = speaker;
        this.handle = new VocalisationHandle(requestId, canBeInterrupted, completionFuture);
    }

    public Class<? extends BaseVoxEvent> getOriginType() {
        return originType;
    }

    public boolean canBeInterrupted() {
        return canBeInterrupted;
    }

    /** Voice identifier for the active provider (enum name or provider-native ShortName); null uses the default. */
    public String getVoiceName() {
        return voiceName;
    }

    /**
     * True when this vocalisation should be processed through the radio transmission filter.
     */
    public boolean isRadio() {
        return isRadio;
    }

    /**
     * Who the text is attributed to in the chat log and on the HUD overlay: the localized source of
     * a radio transmission. Null - the normal case - means the AI is speaking, and the UI names it
     * after the commander's ship.
     */
    public @Nullable String getSpeaker() {
        return speaker;
    }

    /**
     * Voices already spoken for by a named speaker, which the random radio draw must skip. Empty for
     * everything that is not a radio transmission, and for a transmission with a voice of its own.
     */
    public Set<String> getReservedVoices() {
        return reservedVoices;
    }

    /** The request-scoped lifecycle claimed and settled by the active Mouth. */
    public VocalisationHandle handle() {
        return handle;
    }

    public CompletableFuture<Void> getCompletionFuture() {
        return handle.completion();
    }
}
