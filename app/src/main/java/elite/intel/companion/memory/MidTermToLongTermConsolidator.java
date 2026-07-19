package elite.intel.companion.memory;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.CompanionRuntimeGeneration;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consolidates pending DIALOGUE and EVENT records into separate long-term summaries. Failed batches remain
 * pending, retry automatically, and use a bounded local summary after repeated model failures.
 */
public final class MidTermToLongTermConsolidator implements PendingConsolidationListener, AutoCloseable {

    private static final Logger log = LogManager.getLogger(MidTermToLongTermConsolidator.class);
    private static final String FAILURE_NOTICE_KEY = "handler.common.memoryConsolidationFailed";
    private static final int MAX_MODEL_ATTEMPTS = 3;
    private static final long RETRY_BASE_DELAY_MILLIS = 250;

    private final MemoryGateway memoryGateway;
    private final LlmGateway llmGateway;
    private final SpeechGateway speechGateway;
    private final Executor executor;
    private final RetryScheduler retryScheduler;
    private final CompanionRuntimeGeneration runtimeGeneration;
    private final CompressionPromptComposer promptComposer = new CompressionPromptComposer();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object lock = new Object();
    private final Map<MemoryKind, List<MemoryRecord>> buffers = new EnumMap<>(MemoryKind.class);
    private final Set<MemoryKind> retryBlocked = EnumSet.noneOf(MemoryKind.class);
    private final Map<MemoryKind, Integer> retryAttempts = new EnumMap<>(MemoryKind.class);
    private boolean workerActive;

    /** Production constructor with an owned single background worker. */
    public MidTermToLongTermConsolidator(
            MemoryGateway memoryGateway,
            LlmGateway llmGateway,
            SpeechGateway speechGateway
    ) {
        this(memoryGateway, llmGateway, speechGateway, new CompanionRuntimeGeneration());
    }

    /** Production lifecycle constructor bound to the owning runtime generation. */
    public MidTermToLongTermConsolidator(
            MemoryGateway memoryGateway,
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            CompanionRuntimeGeneration runtimeGeneration
    ) {
        this(memoryGateway, llmGateway, speechGateway, runtimeGeneration,
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "companion-consolidator");
                    thread.setDaemon(true);
                    return thread;
                }), new ScheduledRetryScheduler());
    }

    /** Test seam with a caller-owned executor. */
    MidTermToLongTermConsolidator(
            MemoryGateway memoryGateway,
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            Executor executor
    ) {
        this(memoryGateway, llmGateway, speechGateway, new CompanionRuntimeGeneration(), executor,
                (task, ignoredDelay) -> task.run());
    }

    /** Test seam with both lifecycle and executor explicit. */
    MidTermToLongTermConsolidator(
            MemoryGateway memoryGateway,
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            CompanionRuntimeGeneration runtimeGeneration,
            Executor executor
    ) {
        this(memoryGateway, llmGateway, speechGateway, runtimeGeneration, executor,
                (task, ignoredDelay) -> task.run());
    }

    /** Test seam with lifecycle, worker, and retry scheduling explicit. */
    MidTermToLongTermConsolidator(
            MemoryGateway memoryGateway,
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            CompanionRuntimeGeneration runtimeGeneration,
            Executor executor,
            RetryScheduler retryScheduler
    ) {
        this.memoryGateway = Objects.requireNonNull(memoryGateway, "memoryGateway");
        this.llmGateway = Objects.requireNonNull(llmGateway, "llmGateway");
        this.speechGateway = Objects.requireNonNull(speechGateway, "speechGateway");
        this.runtimeGeneration = Objects.requireNonNull(runtimeGeneration, "runtimeGeneration");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.retryScheduler = Objects.requireNonNull(retryScheduler, "retryScheduler");
    }

    @Override
    public void onPending(MemoryRecord record) {
        if (!acceptsWork() || !record.kind().hasLongTermSummary()) {
            return;
        }
        synchronized (lock) {
            if (!acceptsWork()) {
                return;
            }
            buffers.computeIfAbsent(record.kind(), ignored -> new ArrayList<>()).add(record);
        }
        scheduleNext();
    }

    private void scheduleNext() {
        Batch batch = claimNext();
        if (batch == null) {
            return;
        }
        try {
            executor.execute(() -> consolidate(batch));
        } catch (RejectedExecutionException rejected) {
            restore(batch);
            if (acceptsWork()) {
                throw rejected;
            }
        }
    }

    private Batch claimNext() {
        synchronized (lock) {
            if (!acceptsWork() || workerActive) {
                return null;
            }
            for (MemoryKind kind : MemoryKind.values()) {
                List<MemoryRecord> buffer = buffers.get(kind);
                if (retryBlocked.contains(kind) || buffer == null
                        || buffer.size() < CompanionMemoryPolicy.consolidationBatchSize()) {
                    continue;
                }
                int size = CompanionMemoryPolicy.consolidationBatchSize();
                List<MemoryRecord> records = List.copyOf(buffer.subList(0, size));
                buffer.subList(0, size).clear();
                workerActive = true;
                int attempt = retryAttempts.getOrDefault(kind, 1);
                retryAttempts.remove(kind);
                return new Batch(kind, records, attempt);
            }
            return null;
        }
    }

    private void consolidate(Batch batch) {
        try {
            String summary = llmGateway.compressMidTermMemory(
                    compressionRequest(batch.kind(), memoryGateway.longTermSummary(batch.kind()), batch.records())).get();
            if (!acceptsWork()) {
                return;
            }
            if (summary == null || summary.isBlank()
                    || summary.length() > CompanionMemoryPolicy.summaryMaxChars()) {
                handleFailure(batch, "compression produced empty or oversized output ("
                        + (summary == null ? "null" : summary.length() + " chars") + ")");
                return;
            }
            String replacement = summary.strip();
            boolean committed = runtimeGeneration.runIfActive(() -> {
                if (!closed.get()) {
                    memoryGateway.commitConsolidation(batch.kind(), batch.records(), replacement);
                }
            });
            if (committed && !closed.get()) {
                completeBatch();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            if (acceptsWork()) {
                handleFailure(batch, "compression call interrupted");
            }
        } catch (Exception failure) {
            if (acceptsWork()) {
                handleFailure(batch, "compression call failed: " + failure.getMessage());
            }
        }
    }

    private void handleFailure(Batch batch, String reason) {
        fail(batch.kind(), reason, batch.attempt() == 1);
        if (batch.attempt() >= MAX_MODEL_ATTEMPTS && commitLocalFallback(batch)) {
            completeBatch();
            return;
        }
        restore(batch);
    }

    /** Commits a bounded record rendering so repeated model failures cannot block consolidation forever. */
    private boolean commitLocalFallback(Batch batch) {
        try {
            String fallback = localFallbackSummary(
                    memoryGateway.longTermSummary(batch.kind()), batch.records());
            boolean committed = runtimeGeneration.runIfActive(() -> {
                if (!closed.get()) {
                    memoryGateway.commitConsolidation(batch.kind(), batch.records(), fallback);
                }
            });
            if (committed && !closed.get()) {
                log.warn("{} memory consolidation used the local fallback after {} model attempts",
                        batch.kind(), batch.attempt());
                return true;
            }
        } catch (RuntimeException failure) {
            log.error("{} local memory-consolidation fallback failed", batch.kind(), failure);
        }
        return false;
    }

    private void completeBatch() {
        synchronized (lock) {
            workerActive = false;
        }
        scheduleNext();
    }

    private void restore(Batch batch) {
        synchronized (lock) {
            workerActive = false;
            if (!acceptsWork()) {
                return;
            }
            List<MemoryRecord> buffer = buffers.computeIfAbsent(batch.kind(), ignored -> new ArrayList<>());
            buffer.addAll(0, batch.records());
            retryBlocked.add(batch.kind());
            retryAttempts.put(batch.kind(), Math.min(MAX_MODEL_ATTEMPTS, batch.attempt() + 1));
        }
        scheduleRetry(batch);
        scheduleNext();
    }

    private void scheduleRetry(Batch batch) {
        long delay = RETRY_BASE_DELAY_MILLIS << Math.max(0, batch.attempt() - 1);
        try {
            retryScheduler.schedule(() -> {
                synchronized (lock) {
                    if (!acceptsWork()) {
                        return;
                    }
                    retryBlocked.remove(batch.kind());
                }
                scheduleNext();
            }, delay);
        } catch (RuntimeException rejected) {
            log.error("{} memory-consolidation retry could not be scheduled", batch.kind(), rejected);
        }
    }

    private void fail(MemoryKind kind, String reason, boolean notifyCommander) {
        if (!acceptsWork()) {
            return;
        }
        log.warn("{} memory consolidation failed; batch retained for retry: {}", kind, reason);
        if (!notifyCommander) {
            return;
        }
        runtimeGeneration.runIfActive(() -> {
            if (!closed.get()) {
                speechGateway.submit(new SpeechRequest(
                        UUID.randomUUID().toString(), failureNotice(), Urgency.NORMAL));
            }
        });
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (lock) {
            buffers.clear();
            retryBlocked.clear();
            retryAttempts.clear();
        }
        if (executor instanceof ExecutorService service) {
            service.shutdownNow();
        }
        retryScheduler.close();
    }

    private boolean acceptsWork() {
        return !closed.get() && runtimeGeneration.isActive();
    }

    private LlmRequest compressionRequest(MemoryKind kind, String summary, List<MemoryRecord> batch) {
        return new LlmRequest(UUID.randomUUID().toString(), promptComposer.compose(kind, summary, batch),
                List.of(), PromptCacheProfile.COMPRESSION);
    }

    private static String failureNotice() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return LlmTextProvider.getText(language, FAILURE_NOTICE_KEY);
    }

    private static String localFallbackSummary(String previous, List<MemoryRecord> records) {
        StringBuilder summary = new StringBuilder();
        if (previous != null && !previous.isBlank()) {
            summary.append(previous.strip()).append('\n');
        }
        for (MemoryRecord record : records) {
            summary.append(record.kind()).append(": ");
            for (int i = 0; i < record.entries().size(); i++) {
                if (i > 0) {
                    summary.append(" | ");
                }
                summary.append(record.entries().get(i).source()).append(": ")
                        .append(record.entries().get(i).content());
            }
            summary.append('\n');
        }
        String text = summary.toString().strip();
        int max = CompanionMemoryPolicy.summaryMaxChars();
        return text.length() <= max ? text : text.substring(0, max - 3).stripTrailing() + "...";
    }

    /** Schedules a retry without blocking the consolidation worker. */
    @FunctionalInterface
    interface RetryScheduler extends AutoCloseable {
        void schedule(Runnable task, long delayMillis);

        @Override
        default void close() {
        }
    }

    private static final class ScheduledRetryScheduler implements RetryScheduler {
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-consolidation-retry");
            thread.setDaemon(true);
            return thread;
        });

        @Override
        public void schedule(Runnable task, long delayMillis) {
            executor.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            executor.shutdownNow();
        }
    }

    private record Batch(MemoryKind kind, List<MemoryRecord> records, int attempt) {
    }
}
