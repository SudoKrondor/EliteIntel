package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.CompanionRuntimeGeneration;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.model.llm.*;
import elite.intel.ai.brain.vega.model.memory.MemoryEntry;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.ai.brain.vega.tools.SpeakFunction;
import elite.intel.util.json.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Compresses every oversized entry of one completed {@link MemoryRecord} off the thought lanes, then re-writes
 * the whole record atomically. Provider failures fall back to a deterministic bounded copy; shutdown drops work
 * because session memory belongs to the runtime generation being closed.
 */
public final class OversizedMemoryCompressor implements OversizedMemoryListener, AutoCloseable {

    private static final Logger log = LogManager.getLogger(OversizedMemoryCompressor.class);
    private static final List<LlmToolDefinition> OUTPUT_TOOLS = outputTools();

    private final MemoryGateway memoryGateway;
    private final LlmGateway llmGateway;
    private final CompanionRuntimeGeneration runtimeGeneration;
    private final Executor executor;
    private final CompressionPromptComposer promptComposer = new CompressionPromptComposer();
    private final AtomicBoolean closed = new AtomicBoolean();

    /** Production constructor with one ordered daemon worker for record compression. */
    public OversizedMemoryCompressor(
            MemoryGateway memoryGateway,
            LlmGateway llmGateway,
            CompanionRuntimeGeneration runtimeGeneration
    ) {
        this(memoryGateway, llmGateway, runtimeGeneration, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-memory-compressor");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** Test seam with caller-controlled execution. */
    OversizedMemoryCompressor(
            MemoryGateway memoryGateway,
            LlmGateway llmGateway,
            CompanionRuntimeGeneration runtimeGeneration,
            Executor executor
    ) {
        this.memoryGateway = Objects.requireNonNull(memoryGateway, "memoryGateway");
        this.llmGateway = Objects.requireNonNull(llmGateway, "llmGateway");
        this.runtimeGeneration = Objects.requireNonNull(runtimeGeneration, "runtimeGeneration");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public boolean onOversized(MemoryRecord record) {
        Objects.requireNonNull(record, "record");
        if (!acceptsWork()) {
            return false;
        }
        try {
            executor.execute(() -> compress(record));
            return true;
        } catch (RejectedExecutionException rejected) {
            if (acceptsWork()) {
                log.warn("Oversized memory compression was rejected; using the bounded storage fallback", rejected);
            }
            return false;
        }
    }

    private void compress(MemoryRecord record) {
        List<MemoryEntry> entries = new ArrayList<>(record.entries().size());
        for (MemoryEntry entry : record.entries()) {
            if (!acceptsWork()) {
                return;
            }
            String content = entry.content();
            if (content.length() > CompanionMemoryPolicy.entryMaxChars()) {
                content = compressEntry(entry);
                if (content == null) {
                    return;
                }
            }
            entries.add(new MemoryEntry(entry.source(), content));
        }

        MemoryRecord compressed = record.withEntries(entries);
        runtimeGeneration.runIfActive(() -> {
            if (!closed.get()) {
                memoryGateway.write(compressed);
            }
        });
    }

    /** Returns null only when shutdown interrupted the compression; other failures use the bounded source text. */
    private String compressEntry(MemoryEntry entry) {
        LlmResult result = null;
        boolean failed = false;
        try {
            result = llmGateway.submit(compressionRequest(entry.content())).get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception failure) {
            failed = true;
            if (acceptsWork()) {
                log.warn("Memory compression failed; storing a bounded copy of the original entry", failure);
            }
        }

        String gist = speakTextOf(result);
        boolean hasGist = gist != null && !gist.isBlank();
        String candidate = hasGist ? oneLine(gist) : entry.content();
        if (!hasGist && !failed) {
            log.warn("Memory compression produced no usable speak.text; storing a bounded copy of the original entry");
        } else if (hasGist && candidate.length() > CompanionMemoryPolicy.entryMaxChars()) {
            log.warn("Memory compression returned {} characters; bounding the gist to {}",
                    candidate.length(), CompanionMemoryPolicy.entryMaxChars());
        }
        return MemoryTextBounds.entry(candidate);
    }

    private static String speakTextOf(LlmResult result) {
        if (result == null || !result.isValid() || result.toolInvocations().size() != 1) {
            return null;
        }
        LlmToolInvocation invocation = result.toolInvocations().getFirst();
        if (!SpeakFunction.ID.equals(invocation.name())) {
            return null;
        }
        return JsonUtils.getAsStringOrEmpty(invocation.arguments(), SpeakFunction.PARAM_TEXT);
    }

    private static String oneLine(String text) {
        return text.strip().replaceAll("\\s+", " ");
    }

    private LlmRequest compressionRequest(String content) {
        return new LlmRequest(UUID.randomUUID().toString(),
                promptComposer.composeLineCompression(content),
                OUTPUT_TOOLS, PromptCacheProfile.COMPRESSION);
    }

    private static List<LlmToolDefinition> outputTools() {
        SpeakFunction speak = new SpeakFunction();
        return List.of(new LlmToolDefinition(
                speak.id(), speak.llmDescription(), "", speak.parameters()));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (executor instanceof ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }

    private boolean acceptsWork() {
        return !closed.get() && runtimeGeneration.isActive();
    }
}
