package elite.intel.companion.memory;

/**
 * Default {@link TokenEstimator}: a character-length heuristic. Uses a deliberately small
 * characters-per-token divisor so the estimate leans high for Cyrillic-heavy content, keeping memory
 * eviction conservative (it evicts slightly early rather than overrunning the budget).
 */
public final class HeuristicTokenEstimator implements TokenEstimator {

    /** Conservative characters-per-token ratio (English ~4, Cyrillic ~2-3; 3 over-estimates safely). */
    private static final int CHARS_PER_TOKEN = 3;

    @Override
    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // Ceiling division so any non-blank text costs at least one token.
        return (text.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN;
    }
}
