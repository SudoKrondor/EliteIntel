package elite.intel.companion.memory;

/**
 * Replaceable estimator of how many LLM tokens a piece of text costs. Memory bounds use it only as a
 * cheap backstop, so an approximation is acceptable; a provider-accurate tokenizer can be substituted
 * without touching memory logic.
 */
public interface TokenEstimator {

    /**
     * Estimates the token count of the given text.
     *
     * @param text text to measure; {@code null}/blank counts as zero
     * @return non-negative estimated token count
     */
    int estimate(String text);
}
