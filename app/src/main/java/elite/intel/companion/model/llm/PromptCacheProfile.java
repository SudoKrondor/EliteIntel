package elite.intel.companion.model.llm;

/**
 * Identifies which stable prompt prefix an {@code LlmRequest} uses, and therefore which prompt cache
 * it belongs to. Each profile owns a stable {@link #cacheKey()} (sent as Mistral {@code prompt_cache_key})
 * so requests of the same profile share a cached prefix, and its sampling {@link #temperature()}.
 * <p>
 * The two prompt-composing thoughts have different stable prefixes: COMMANDER (full consciousness) and
 * NARRATION (lean subscriber-narration prompt). Compression is its own profile. COMMANDER runs cold to favor
 * deterministic function calling; narration and compression stay at 0.3 for faithful prose.
 */
public enum PromptCacheProfile {

    /** Commander consciousness turn. */
    COMMANDER("companion-commander", 0.2),
    /** Subscriber-prepared narration turn (its own lean prompt prefix). */
    NARRATION("companion-narration", 0.3),
    /** Mid-term -> long-term memory compression. */
    COMPRESSION("companion-compression", 0.3),
    /**
     * Custom-command action-key generation: a short plain-text turn that maps trigger phrases (any
     * language) to an English snake_case routing identifier. Runs cold (its own low temperature) for a
     * stable, deterministic identifier and keeps its own cache key so it never pollutes the conversation
     * profiles' cached prefixes.
     */
    KEY_GENERATION("companion-keygen", 0.2);

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
