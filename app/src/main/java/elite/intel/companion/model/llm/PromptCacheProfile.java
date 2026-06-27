package elite.intel.companion.model.llm;

/**
 * Identifies which stable prompt prefix an {@code LlmRequest} uses, and therefore which prompt cache
 * it belongs to. Each profile owns a stable {@link #cacheKey()} (sent as Mistral {@code prompt_cache_key})
 * so requests of the same profile share a cached prefix.
 * <p>
 * The two prompt-composing thoughts have different stable prefixes: COMMANDER (full consciousness) and
 * NARRATION (lean subscriber-narration prompt). Compression is its own profile.
 */
public enum PromptCacheProfile {

    /** Commander consciousness turn. */
    COMMANDER("companion-commander"),
    /** Subscriber-prepared narration turn (its own lean prompt prefix). */
    NARRATION("companion-narration"),
    /** Mid-term -> long-term memory compression. */
    COMPRESSION("companion-compression");

    private final String cacheKey;

    PromptCacheProfile(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    /** Stable application-level cache key for this profile (Mistral {@code prompt_cache_key}). */
    public String cacheKey() {
        return cacheKey;
    }
}
