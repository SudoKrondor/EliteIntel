package elite.intel.ai.brain.vega.model.llm;

/**
 * Identifies which stable prompt prefix an {@code LlmRequest} uses, and therefore which prompt cache
 * it belongs to. Each profile owns a stable {@link #cacheKey()} (sent as Mistral {@code prompt_cache_key})
 * so requests of the same profile share a cached prefix, and its sampling {@link #temperature()}.
 * <p>
 * The two prompt-composing thoughts have different stable prefixes: COMMANDER (full consciousness) and
 * NARRATION (lean subscriber-narration prompt). Compression is its own profile. COMMANDER runs at 0.4 for a
 * more natural conversation while narration and compression stay at 0.3 for faithful prose.
 */
public enum PromptCacheProfile {

    /** Commander consciousness turn. */
    COMMANDER("vega-commander", 0.4),
    /** Subscriber-prepared narration turn (its own lean prompt prefix). */
    NARRATION("vega-narration", 0.3),
    /**
     * Shortening one over-long completed memory record before it enters the replay window
     * ({@link elite.intel.ai.brain.vega.memory.OversizedMemoryCompressor}, whose only tool is {@code speak}).
     * Named for the tier it once served - mid-term to long-term summarisation - which went with the
     * retained history; per-record gist compression is all that still uses this profile.
     */
    COMPRESSION("vega-compression", 0.3),
    /**
     * Custom-command action-key generation: a short plain-text turn that maps trigger phrases (any
     * language) to an English snake_case routing identifier. Runs cold (its own low temperature) for a
     * stable, deterministic identifier and keeps its own cache key so it never pollutes the conversation
     * profiles' cached prefixes.
     */
    KEY_GENERATION("vega-keygen", 0.2);

    private final String cacheKey;
    private final double temperature;

    PromptCacheProfile(String cacheKey, double temperature) {
        this.cacheKey = cacheKey;
        this.temperature = temperature;
    }

    /** Stable application-level cache key for this profile (Mistral {@code prompt_cache_key}). */
    public String cacheKey() {
        return cacheKey;
    }

    /** Sampling temperature for this profile's requests (lower = more deterministic tool selection). */
    public double temperature() {
        return temperature;
    }
}
