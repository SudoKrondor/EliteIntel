package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.vega.diag.VegaDiagnostics;
import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.session.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Supplier;

/**
 * Loads the embedding model and embeds the whole alias catalog before the commander speaks.
 *
 * <p>WHY it exists: {@link SemanticActionReducer} embeds each alias phrase the first time it scores it, and the
 * model itself loads on first use. So the first command after start paid for all of it - about 1.4 s to load the
 * model and ~3 ms for each of the visible tools' ~1,000 phrases - before any LLM request left: 4.4 s of "reduce"
 * against 15 ms on every later turn, whichever LLM was configured. A situation change (supercruise, SRV, on foot)
 * or a language switch paid again for the tools it brought into view.
 *
 * <p>Warms every tool regardless of visibility, in exactly the text the reducer embeds ({@link AliasEmbeddingText}),
 * so later cache hits are exact. Runs on one background thread; a turn arriving mid-warm interleaves with it phrase
 * by phrase and still benefits from whatever is already cached. If the model cannot load, nothing changes: the
 * reducer degrades to word matching exactly as it would have on its first turn.
 */
public final class SemanticCatalogWarmer {

    private static final Logger log = LogManager.getLogger(SemanticCatalogWarmer.class);

    private final Supplier<List<GameToolCandidates.Candidate>> candidateSource;
    private final Supplier<SemanticPhraseMatcher> matcherSupplier;

    /**
     * Production: every tool in the configured language, and the shared process-wide embedder.
     */
    public SemanticCatalogWarmer() {
        this(() -> new GameToolCandidates(Status.getInstance())
                        .collectIgnoringVisibility(EnumSet.allOf(IntelActionCategory.class)),
                SemanticSearchProvider::matcher);
    }

    /**
     * Test seam: a fixed catalog and matcher.
     */
    SemanticCatalogWarmer(Supplier<List<GameToolCandidates.Candidate>> candidateSource,
                          Supplier<SemanticPhraseMatcher> matcherSupplier) {
        this.candidateSource = candidateSource;
        this.matcherSupplier = matcherSupplier;
    }

    /**
     * Warms the production catalog on a background thread and returns at once.
     */
    public static void warmInBackground() {
        Thread.ofPlatform().daemon().name("semantic-catalog-warmer").start(() -> {
            try {
                new SemanticCatalogWarmer().warm();
            } catch (RuntimeException failure) {
                // A failed warm only means the first turn embeds lazily, as it always used to.
                log.warn("Semantic catalog warm-up failed; phrases will be embedded on first use", failure);
            }
        });
    }

    /**
     * Loads the model and embeds every catalog phrase not cached yet, blocking until done.
     *
     * @return how many phrases were embedded, or {@code -1} when the embedding model is unavailable
     */
    int warm() {
        long started = System.nanoTime();
        SemanticPhraseMatcher matcher = matcherSupplier.get();
        if (matcher == null) {
            return -1;
        }
        long loaded = System.nanoTime();
        Set<String> phrases = new LinkedHashSet<>();
        for (GameToolCandidates.Candidate candidate : candidateSource.get()) {
            phrases.addAll(AliasEmbeddingText.phrases(candidate.localizedAliasGroup(), candidate.tool().parameters()));
        }
        int embedded = matcher.warm(phrases);
        long done = System.nanoTime();
        VegaDiagnostics.debug(VegaDiagnostics.SYSTEM, "warm", String.format(Locale.ROOT,
                "semantic catalog: model=%d ms, embedded %d of %d phrases in %d ms",
                (loaded - started) / 1_000_000, embedded, phrases.size(), (done - loaded) / 1_000_000));
        return embedded;
    }
}
