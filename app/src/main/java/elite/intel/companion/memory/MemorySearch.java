package elite.intel.companion.memory;

import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.VectorMath;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.memory.MemoryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * Read-only recall over the companion's memory areas (the {@code search_in_memory} ranking). Scores each
 * candidate by word-overlap and by meaning (cosine of the query vector to the entry's vector), collapses
 * near-duplicate paraphrases into one, fuses the two signals by reciprocal rank, and returns the top entries as
 * labelled text. Pure: it never mutates the stores. Split out of {@link SessionMemoryGateway} so the gateway
 * owns storage and eviction while this owns the ranking; stateless, so the methods are static.
 */
final class MemorySearch {

    private static final Logger log = LogManager.getLogger(MemorySearch.class);

    /** Reciprocal-rank-fusion constant: dampens how much a top rank dominates (the standard k). */
    private static final int RRF_K = 60;

    private MemorySearch() {
    }

    /**
     * Ranks the memory areas against {@code query} and returns at most {@code limit} matches as labelled
     * text. A given entry lives in exactly one of short-term / mid-term, so the two never double-count.
     *
     * @param shortTerm     the hot timeline entries
     * @param midTerm       mid-term entries across all topics
     * @param summary       the session long-term summary as searchable entries (empty when none consolidated)
     * @param archive       pinned MAX facts (capped by {@link CompanionMemoryLimits#ARCHIVE_RECALL_LIMIT})
     * @param matcherSource supplies the shared semantic matcher, or null when semantic search is unavailable;
     *                      consulted only for a non-blank query, so a blank query never loads the model
     */
    static List<String> recall(String query, int limit, List<MemoryEntry> shortTerm, List<MemoryEntry> midTerm,
                               List<MemoryEntry> summary, List<MemoryEntry> archive,
                               Supplier<SemanticPhraseMatcher> matcherSource) {
        if (limit <= 0) {
            return List.of();
        }
        Set<String> queryTokens = tokens(query);
        boolean blank = queryTokens.isEmpty();
        // Semantic search runs only for a real query with a loaded model; a blank query keeps the old behaviour
        // (every entry matches with relevance 0, so it degenerates to importance-then-recency). The dedup step
        // below still runs on every path, collapsing near-identical entries that carry vectors.
        SemanticPhraseMatcher matcher = blank ? null : matcherSource.get();
        float[] queryVector = matcher == null ? null : safeEmbedQuery(matcher, query);
        boolean semantic = queryVector != null;

        List<Scored> scored = new ArrayList<>();
        collectScored(scored, false, shortTerm, queryTokens, blank, queryVector);
        collectScored(scored, false, midTerm, queryTokens, blank, queryVector);
        collectScored(scored, false, summary, queryTokens, blank, queryVector);
        collectScored(scored, true, archive, queryTokens, blank, queryVector);

        List<Scored> eligible = dedupByMeaning(filterEligible(scored, blank, semantic));
        // With both signals present, fuse the two rankings by position (reciprocal rank) so neither scale's raw
        // numbers dominate; with words only, keep the original relevance-then-importance-then-recency order.
        eligible.sort(semantic ? byFusedRank(eligible) : BY_WORD_RANK);
        return emit(eligible, limit);
    }

    /** Whether two entries mean the same thing (cosine &ge; {@code floor}); false if either lacks a vector. */
    static boolean sameMeaning(MemoryEntry a, MemoryEntry b, double floor) {
        return a.embedding() != null && b.embedding() != null
                && VectorMath.cosine(a.embedding(), b.embedding()) >= floor;
    }

    /**
     * Keeps an entry if it matches by words (exact lexical hit - always reliable, e.g. a name or code), or
     * (when semantic search is on) is at or above the absolute meaning floor. The floor is the only semantic
     * gate: recall must be free to return several distinct facts (a compound question needs both), so a
     * relative "within a margin of the best match" cut is deliberately avoided - it would drop the weaker but
     * still-relevant second fact and leave the model to guess it.
     */
    private static List<Scored> filterEligible(List<Scored> scored, boolean blank, boolean semantic) {
        double floor = CompanionConfig.semanticSearchInMemoryFloor();
        List<Scored> eligible = new ArrayList<>();
        for (Scored s : scored) {
            boolean semanticHit = semantic && !Double.isNaN(s.semScore()) && s.semScore() >= floor;
            if (blank || s.wordScore() > 0 || semanticHit) {
                eligible.add(s);
            }
        }
        return eligible;
    }

    /**
     * Emits the ranked results as labelled text, capping how many archive (pinned MAX) facts enter so an
     * accumulating archive cannot crowd out the more relevant short/mid-term matches, de-duplicating identical
     * content, and stopping at the limit.
     */
    private static List<String> emit(List<Scored> ranked, int limit) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int archiveUsed = 0;
        for (Scored s : ranked) {
            if (s.archive() && archiveUsed >= CompanionMemoryLimits.ARCHIVE_RECALL_LIMIT) {
                continue;
            }
            String content = "[" + s.entry().source().displayLabel(CompanionConfig.companionName()) + "] "
                    + s.entry().content();
            if (seen.add(content)) {
                if (s.archive()) {
                    archiveUsed++;
                }
                out.add(content);
                if (out.size() >= limit) {
                    break;
                }
            }
        }
        return out;
    }

    /**
     * Scores every entry of one memory area against the query and adds it as a candidate. Word-overlap is 0 for
     * a blank query (all match); meaning-closeness is {@code NaN} when there is no query vector or the entry was
     * never embedded, so such an entry can still match by words but never by an accidental low cosine.
     */
    private static void collectScored(List<Scored> out, boolean archive, List<MemoryEntry> entries,
                                      Set<String> queryTokens, boolean blank, float[] queryVector) {
        for (MemoryEntry entry : entries) {
            int wordScore = blank ? 0 : overlap(queryTokens, entry.content());
            double semScore = (queryVector != null && entry.embedding() != null)
                    ? VectorMath.cosine(queryVector, entry.embedding())
                    : Double.NaN;
            out.add(new Scored(entry, archive, wordScore, semScore));
        }
    }

    /** A candidate entry with its two recall scores: word-overlap count and meaning-closeness (cosine). */
    private record Scored(MemoryEntry entry, boolean archive, int wordScore, double semScore) {}

    /** Word relevance first, then importance, then recency - the recall ranking when there is no query vector. */
    private static final Comparator<Scored> BY_WORD_RANK = Comparator
            .comparingInt(Scored::wordScore).reversed()
            .thenComparing(s -> s.entry().importance(), Comparator.reverseOrder())
            .thenComparing(s -> s.entry().timestamp(), Comparator.reverseOrder());

    /**
     * Reciprocal-rank fusion of the word-overlap and meaning rankings, then importance, then recency. Each
     * entry's fused score is the sum of {@code 1/(k+rank)} over the two rankings it appears in, so an entry
     * ranked high by either signal surfaces and one ranked high by both surfaces strongest, without comparing
     * the two incompatible scales (an overlap count vs a cosine) directly.
     */
    private static Comparator<Scored> byFusedRank(List<Scored> eligible) {
        double floor = CompanionConfig.semanticSearchInMemoryFloor();
        Map<Scored, Double> fused = new IdentityHashMap<>();
        accumulateReciprocalRank(fused, eligible, s -> s.wordScore() > 0, Scored::wordScore);
        accumulateReciprocalRank(fused, eligible, s -> s.semScore() >= floor, Scored::semScore);
        return Comparator.<Scored>comparingDouble(s -> fused.getOrDefault(s, 0.0)).reversed()
                .thenComparing(s -> s.entry().importance(), Comparator.reverseOrder())
                .thenComparing(s -> s.entry().timestamp(), Comparator.reverseOrder());
    }

    /**
     * Adds each entry's reciprocal-rank contribution for one ranking. Rank is competition-style: entries with
     * an equal score share a rank, so a tie in one signal stays a tie (broken by the other signal, then by
     * importance/recency) rather than by arbitrary list position.
     */
    private static void accumulateReciprocalRank(Map<Scored, Double> fused, List<Scored> eligible,
                                                 Predicate<Scored> ranked, ToDoubleFunction<Scored> score) {
        List<Scored> ordered = eligible.stream()
                .filter(ranked)
                .sorted(Comparator.comparingDouble(score).reversed())
                .toList();
        int rank = 0;
        double prev = Double.NaN;
        for (int i = 0; i < ordered.size(); i++) {
            Scored s = ordered.get(i);
            double sc = score.applyAsDouble(s);
            if (i > 0 && sc != prev) {
                rank = i; // standard competition ranking: skip ranks after a group of ties
            }
            fused.merge(s, 1.0 / (RRF_K + rank), Double::sum);
            prev = sc;
        }
    }

    /**
     * Collapses candidate results that mean the same thing into one each: the representative is the most
     * important (newest when tied) entry of the group, carrying the best of the group's two relevance scores so
     * the group still surfaces at its strongest match. Entries without a vector are never merged.
     */
    private static List<Scored> dedupByMeaning(List<Scored> eligible) {
        double floor = CompanionConfig.semanticDedupFloor();
        List<Scored> survivors = new ArrayList<>();
        for (Scored candidate : eligible) {
            int cluster = clusterOf(survivors, candidate.entry(), floor);
            if (cluster < 0) {
                survivors.add(candidate);
            } else {
                survivors.set(cluster, mergeScored(survivors.get(cluster), candidate));
            }
        }
        return survivors;
    }

    /** Index of the survivor whose meaning matches {@code entry}, or -1 (and -1 when the entry has no vector). */
    private static int clusterOf(List<Scored> survivors, MemoryEntry entry, double floor) {
        for (int i = 0; i < survivors.size(); i++) {
            if (sameMeaning(entry, survivors.get(i).entry(), floor)) {
                return i;
            }
        }
        return -1;
    }

    /** Merges two same-meaning results: the more important (newer when tied) entry, with the group's best scores. */
    private static Scored mergeScored(Scored a, Scored b) {
        Scored representative = isMoreImportant(a, b) ? a : b;
        var freshest = a.entry().timestamp().isAfter(b.entry().timestamp())
                ? a.entry().timestamp() : b.entry().timestamp();
        int wordScore = Math.max(a.wordScore(), b.wordScore());
        double semScore = maxScore(a.semScore(), b.semScore());
        return new Scored(representative.entry().withTimestamp(freshest), representative.archive(), wordScore, semScore);
    }

    /** Whether {@code a} is the better representative: more important, or newer at equal importance. */
    private static boolean isMoreImportant(Scored a, Scored b) {
        int byImportance = a.entry().importance().compareTo(b.entry().importance());
        if (byImportance != 0) {
            return byImportance > 0;
        }
        return !a.entry().timestamp().isBefore(b.entry().timestamp());
    }

    /** The larger of two relevance scores, treating {@code NaN} (not applicable) as the smaller. */
    private static double maxScore(double x, double y) {
        if (Double.isNaN(x)) {
            return y;
        }
        if (Double.isNaN(y)) {
            return x;
        }
        return Math.max(x, y);
    }

    /** Embeds the query, returning {@code null} on a transient embed failure so recall degrades to word-only. */
    private static float[] safeEmbedQuery(SemanticPhraseMatcher matcher, String query) {
        try {
            return matcher.embedQuery(query);
        } catch (RuntimeException e) {
            // WHY: a transient embed failure must not abort recall; degrade to word-only for this query. The
            // matcher exists only after a successful model load, so a throw here is unexpected - log, not hide.
            log.warn("Query embedding failed; falling back to word-only recall for this query", e);
            return null;
        }
    }

    /**
     * The lexical relevance score of an entry: how many distinct query tokens appear verbatim in the content.
     * Matching is exact (token equality), not inflection-tolerant: word forms, paraphrases and cross-lingual
     * meaning are recalled by the semantic vector instead (see {@link #recall}), while exact word-overlap keeps
     * the proper nouns and codes (names, callsigns, docking codes) that embeddings can rank weakly. The earlier
     * fuzzy rule made short stems spuriously match unrelated words ("код" matched "кодовое"), surfacing the
     * wrong facts; recall no longer fuzzes letters now that meaning is carried by a vector.
     */
    private static int overlap(Set<String> queryTokens, String content) {
        Set<String> contentTokens = tokens(content);
        int score = 0;
        for (String q : queryTokens) {
            if (contentTokens.contains(q)) {
                score++;
            }
        }
        return score;
    }

    /** Meaningful lower-cased word tokens: length > 2 and not a stop word (same filter as the action reducer). */
    private static Set<String> tokens(String text) {
        if (text == null) {
            return Set.of();
        }
        Set<String> set = new HashSet<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+")) {
            if (word.length() > 2 && !InputNormalizerLocalizations.stopWords().contains(word)) {
                set.add(word);
            }
        }
        return set;
    }
}
