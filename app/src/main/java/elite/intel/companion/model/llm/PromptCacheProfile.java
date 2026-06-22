package elite.intel.companion.model.llm;

/**
 * Identifies which stable prompt prefix an {@code LlmRequest} uses, and therefore which prompt cache
 * it belongs to. Each profile owns a stable {@link #cacheKey()} (sent as Mistral {@code prompt_cache_key})
 * so requests of the same profile share a cached prefix.
 * <p>
 * Consciousness splits into COMMANDER/EVENT (which have different stable prefixes); compression is its
 * own profile.
 */
public enum PromptCacheProfile {

    /** Commander consciousness turn. */
    COMMANDER("companion-commander"),
    /** Event consciousness turn. */
    EVENT("companion-event"),
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
