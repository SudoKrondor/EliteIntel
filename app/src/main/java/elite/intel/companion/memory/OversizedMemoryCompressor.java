package elite.intel.companion.memory;

import elite.intel.companion.CompanionConfig;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Shrinks an over-long memory entry off the write path and re-writes a short gist, so a single long line never
 * bloats the prompt. Wired into the gateway as its {@link OversizedMemoryListener} at subsystem start.
 * <p>
 * Threading: each compression runs on its own single dedicated daemon executor (never a thought lane,
 * mirroring {@link MidTermToLongTermConsolidator}), so it never blocks the memory write path nor a
 * spoken-narration lane. The gist is re-written under the entry's original source, topic, importance and time;
 * a failed, empty, or still-oversized compression drops the entry (it was prompt-bloat) with a logged warning.
 */
public final class OversizedMemoryCompressor implements OversizedMemoryListener {

    private static final Logger log = LogManager.getLogger(OversizedMemoryCompressor.class);

    private final MemoryGateway memoryGateway;
    private final LlmGateway llmGateway;
    private final Executor executor;
    private final CompressionPromptComposer promptComposer = new CompressionPromptComposer();

    /** Production: a single-thread daemon executor serializes compressions off the write and narration paths. */
    public OversizedMemoryCompressor(MemoryGateway memoryGateway, LlmGateway llmGateway) {
        this(memoryGateway, llmGateway, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-memory-compressor");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** Test seam: inject a synchronous executor. */
    OversizedMemoryCompressor(MemoryGateway memoryGateway, LlmGateway llmGateway, Executor executor) {
        this.memoryGateway = memoryGateway;
        this.llmGateway = llmGateway;
        this.executor = executor;
    }

    @Override
    public void onOversized(MemoryEntry entry) {
        executor.execute(() -> compress(entry));
    }

    /**
     * One compression pass off the lane: shrink to a gist and re-write it. When the model fails to produce a
     * usable within-cap gist, a <b>linked</b> entry (a tool CALL/RESULT) falls back to a hard-truncated copy so
     * its pair is never orphaned as {@code "(no textual result)"}; an unlinked entry is dropped as before.
     */
    private void compress(MemoryEntry entry) {
        String gist = null;
        boolean failed = false;
        try {
            gist = llmGateway.compressMidTermMemory(compressionRequest(entry.content())).get();
        } catch (Exception failure) { // provider error / interruption; fall through to the fallback below
            log.warn("Memory compression failed for an over-long entry", failure);
            failed = true;
        }
        int max = CompanionConfig.memoryEntryMaxChars();
        boolean hasGist = gist != null && !gist.isBlank();
        String stored;
        if (hasGist && gist.strip().length() <= max) {
            stored = gist.strip();
        } else if (entry.toolLink() != null) {
            // A tool CALL/RESULT must stay paired. Prefer the model's summary attempt (truncated) over the raw
            // head of the original - a purpose-built gist is more useful even cut - and fall back to the original
            // only when the model returned nothing usable. Either way the pair is never orphaned.
            stored = truncateToCap(hasGist ? gist.strip() : entry.content(), max);
        } else {
            // Unlinked over-long entry with no usable gist: drop it (it was prompt-bloat). Log the reason unless a
            // provider failure was already logged above.
            if (!failed) {
                log.warn("Memory compression produced unusable output ({}); dropping the over-long entry",
                        gist == null ? "null" : gist.strip().length() + " chars");
            }
            return;
        }
        // Re-write under the original provenance - source, topic, importance, canonical fact AND tool linkage -
        // so a compressed tool result stays paired with its call. The stored text is within the cap.
        memoryGateway.write(new MemoryEntry(entry.timestamp(), entry.topic(), entry.source(), stored,
                entry.importance(), null, entry.canonicalFact(), entry.toolLink()));
    }

    /** Hard-trims to the entry cap (with an ellipsis); the fallback when the model cannot shorten within it. */
    private static String truncateToCap(String text, int max) {
        int end = Math.min(text.length(), Math.max(0, max - 1));
        return text.substring(0, end).strip() + "…";
    }

    private LlmRequest compressionRequest(String content) {
        return new LlmRequest(UUID.randomUUID().toString(), promptComposer.composeLineCompression(content),
                List.of(), PromptCacheProfile.COMPRESSION);
    }
}
