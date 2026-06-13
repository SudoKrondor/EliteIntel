package elite.intel.ui.telemetry;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.ui.event.LlmSessionStatsChangedEvent;
import elite.intel.ui.event.LlmUsageEvent;
import elite.intel.ui.event.RestartBrainEvent;
import elite.intel.ui.event.ServicesStateEvent;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton accumulator for LLM session statistics.
 *
 * <p>Subscribes to {@link LlmUsageEvent}, {@link RestartBrainEvent}, and
 * {@link ServicesStateEvent}; publishes {@link LlmSessionStatsChangedEvent} after each
 * state change. Call {@link #getInstance()} early (e.g. from a panel constructor) to ensure
 * the tracker registers with the EventBus before AI events start flowing.</p>
 *
 * <p>Thread safety: single-word volatile fields for last-request values; {@link AtomicInteger}
 * for accumulators; {@code seenModels} guarded by {@code synchronized(this)}.</p>
 */
public class LlmSessionStatsTracker {

    private static volatile LlmSessionStatsTracker INSTANCE;

    private volatile Instant sessionStart = Instant.now();
    private volatile String lastProvider;
    private volatile String lastModel;
    private volatile int lastPromptTokens;
    private volatile int lastCompletionTokens;
    private volatile int lastCachedTokens;
    private volatile int lastCacheWrittenTokens;
    private volatile double lastTps;

    private final AtomicInteger totalPrompt = new AtomicInteger();
    private final AtomicInteger totalCompletion = new AtomicInteger();
    private final AtomicInteger totalCachedHits = new AtomicInteger();
    private final AtomicInteger totalCacheWritten = new AtomicInteger();

    // Preserves insertion order so multiple models display in chronological order.
    private final Set<String> seenModels = new LinkedHashSet<>();

    private LlmSessionStatsTracker() {
        EventBusManager.register(this);
    }

    /** Returns the singleton tracker, creating and registering it on first call. */
    public static LlmSessionStatsTracker getInstance() {
        if (INSTANCE == null) {
            synchronized (LlmSessionStatsTracker.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LlmSessionStatsTracker();
                }
            }
        }
        return INSTANCE;
    }

    @Subscribe
    public void onServicesState(ServicesStateEvent event) {
        if (event.isRunning()) {
            reset();
        }
    }

    @Subscribe
    public void onRestartBrain(RestartBrainEvent event) {
        reset();
    }

    @Subscribe
    public void onLlmUsage(LlmUsageEvent event) {
        lastProvider = event.provider();
        lastModel = event.model();
        lastPromptTokens = event.promptTokens();
        lastCompletionTokens = event.completionTokens();
        lastCachedTokens = event.cachedTokens();
        lastCacheWrittenTokens = event.cacheWrittenTokens();
        lastTps = event.tps();
        totalPrompt.addAndGet(event.promptTokens());
        totalCompletion.addAndGet(event.completionTokens());
        totalCachedHits.addAndGet(event.cachedTokens());
        totalCacheWritten.addAndGet(event.cacheWrittenTokens());
        synchronized (this) {
            seenModels.add(event.provider() + "  [" + event.model() + "]");
        }
        EventBusManager.publish(new LlmSessionStatsChangedEvent(snapshot()));
    }

    /** Returns an immutable snapshot of the current accumulated session state. */
    public LlmSessionStatsSnapshot getSnapshot() {
        return snapshot();
    }

    private void reset() {
        lastProvider = null;
        lastModel = null;
        lastPromptTokens = 0;
        lastCompletionTokens = 0;
        lastCachedTokens = 0;
        lastCacheWrittenTokens = 0;
        lastTps = 0.0;
        totalPrompt.set(0);
        totalCompletion.set(0);
        totalCachedHits.set(0);
        totalCacheWritten.set(0);
        synchronized (this) {
            seenModels.clear();
        }
        sessionStart = Instant.now();
        EventBusManager.publish(new LlmSessionStatsChangedEvent(snapshot()));
    }

    private LlmSessionStatsSnapshot snapshot() {
        String modelDisplay;
        boolean hasData;
        synchronized (this) {
            hasData = !seenModels.isEmpty();
            modelDisplay = hasData ? String.join(" / ", seenModels) : null;
        }
        return new LlmSessionStatsSnapshot(
                sessionStart,
                modelDisplay,
                lastProvider,
                lastModel,
                lastPromptTokens,
                lastCompletionTokens,
                lastCachedTokens,
                lastCacheWrittenTokens,
                lastTps,
                totalPrompt.get(),
                totalCompletion.get(),
                totalCachedHits.get(),
                totalCacheWritten.get(),
                hasData
        );
    }
}
