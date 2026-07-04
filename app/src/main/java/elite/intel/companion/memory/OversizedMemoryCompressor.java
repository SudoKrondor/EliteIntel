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

    /** One compression pass off the lane: shrink to a gist, then re-write it; drop on any unusable result. */
    private void compress(MemoryEntry entry) {
        String gist;
        try {
            gist = llmGateway.compressMidTermMemory(compressionRequest(entry.content())).get();
        } catch (Exception failure) { // provider error / interruption; the over-long entry is simply dropped
            log.warn("Memory compression failed; dropping an over-long entry", failure);
            return;
        }
        int max = CompanionConfig.memoryEntryMaxChars();
        if (gist == null || gist.isBlank() || gist.strip().length() > max) {
            log.warn("Memory compression produced unusable output ({}); dropping the over-long entry",
                    gist == null ? "null" : gist.strip().length() + " chars");
            return;
        }
        // Re-write under the original provenance; the gist is within the cap, so this stores normally.
        memoryGateway.write(new MemoryEntry(entry.timestamp(), entry.topic(), entry.source(), gist.strip(),
                entry.importance()));
    }

    private LlmRequest compressionRequest(String content) {
        return new LlmRequest(UUID.randomUUID().toString(), promptComposer.composeLineCompression(content),
                List.of(), PromptCacheProfile.COMPRESSION);
    }
}
