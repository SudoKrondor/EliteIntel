package elite.intel.ui.telemetry;

import java.time.Instant;

/**
 * Immutable snapshot of accumulated LLM session statistics.
 * Published via {@link elite.intel.ui.event.LlmSessionStatsChangedEvent} by
 * {@link LlmSessionStatsTracker} after every state change or reset.
 */
public record LlmSessionStatsSnapshot(
        Instant sessionStart,
        /** All seen provider+model strings joined with " / "; null until first event this session. */
        String modelDisplay,
        /** Provider from the most recent event; null until first event. */
        String lastProvider,
        /** Model from the most recent event; null until first event. */
        String lastModel,
        int lastPromptTokens,
        int lastCompletionTokens,
        int lastCachedTokens,
        int lastCacheWrittenTokens,
        double lastTps,
        int totalPromptTokens,
        int totalCompletionTokens,
        /** Cache hits — tokens served at reduced or no charge. */
        int totalCachedHits,
        int totalCacheWritten,
        /** True once at least one {@link elite.intel.ui.event.LlmUsageEvent} has been processed this session. */
        boolean hasData
) {
    /** Prompt + completion tokens chargeable at full rate (excludes cache hits). */
    public int totalChargeableTokens() {
        return totalPromptTokens + totalCompletionTokens;
    }
}
