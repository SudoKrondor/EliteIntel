package elite.intel.companion.model.llm;

/**
 * Identifies which stable prompt prefix an {@code LlmRequest} uses, and therefore which prompt cache
 * it belongs to. Each profile owns a stable {@link #cacheKey()} (sent as Mistral {@code prompt_cache_key})
 * so requests of the same profile share a cached prefix, and its sampling {@link #temperature()}.
 * <p>
 * The two prompt-composing thoughts have different stable prefixes: COMMANDER (full consciousness) and
 * NARRATION (lean subscriber-narration prompt). Compression is its own profile. COMMANDER runs slightly
 * warmer (0.5) for livelier conversation; narration and compression stay cooler (0.3) for fidelity. Command
 * and query selection held accurate at 0.5 in the evals, so the warmer setting costs no routing precision.
 */
public enum PromptCacheProfile {

    /** Commander consciousness turn. */
    COMMANDER("companion-commander", 0.5),
    /** Subscriber-prepared narration turn (its own lean prompt prefix). */
    NARRATION("companion-narration", 0.3),
    /** Mid-term -> long-term memory compression. */
    COMPRESSION("companion-compression", 0.3);

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
