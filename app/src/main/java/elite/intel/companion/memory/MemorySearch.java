package elite.intel.companion.memory;

import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticQuery;
import elite.intel.ai.embed.VectorMath;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemorySearchMatch;
import elite.intel.companion.model.memory.MemoryRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
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

/** Read-only relevance and recency ranking across record-based companion memory. */
final class MemorySearch {

    private static final Logger log = LogManager.getLogger(MemorySearch.class);
    private static final int RRF_K = 60;

    private MemorySearch() {
    }

    static MemorySearchResult recall(
            String query,
            int limit,
            List<MemoryRecord> recent,
            List<MemoryRecord> retained,
            List<MemorySearchMatch> summaries,
            List<MemoryRecord> savedTexts,
            Supplier<SemanticPhraseMatcher> matcherSource
    ) {
        List<RecordScored> ranked = rankRecords(query, recent, retained, summaries, savedTexts, matcherSource);
        Integer exactRecordCount = ranked.stream().anyMatch(candidate -> candidate.document().summary())
                ? null : ranked.size();
        if (limit <= 0 || ranked.isEmpty()) {
            return new MemorySearchResult(ranked.size(), exactRecordCount, List.of());
        }
        List<String> items = boundedItems(ranked, limit);
        return new MemorySearchResult(ranked.size(), exactRecordCount, items);
    }

    static List<MemorySearchMatch> recallMatches(
            String query,
            int limit,
            List<MemoryRecord> recent,
            List<MemoryRecord> retained,
            List<MemorySearchMatch> summaries,
            List<MemoryRecord> savedTexts,
            Supplier<SemanticPhraseMatcher> matcherSource,
            SemanticQuery semanticQuery,
            Predicate<MemorySearchMatch> included
    ) {
        return emitMatches(rankEntries(
                query, recent, retained, summaries, savedTexts, matcherSource, semanticQuery, included), limit);
    }

    /** Whether two entries have semantic vectors above the configured duplicate threshold. */
    static boolean sameMeaning(MemoryEntry first, MemoryEntry second, double floor) {
        return first.embedding() != null && second.embedding() != null
                && VectorMath.cosine(first.embedding(), second.embedding()) >= floor;
    }

    private static List<RecordScored> rankRecords(
            String query,
            List<MemoryRecord> recent,
            List<MemoryRecord> retained,
            List<MemorySearchMatch> summaries,
            List<MemoryRecord> savedTexts,
            Supplier<SemanticPhraseMatcher> matcherSource
    ) {
        List<SearchDocument> documents = new ArrayList<>();
        collectDocuments(documents, recent);
        collectDocuments(documents, retained);
        for (MemorySearchMatch summary : summaries) {
            documents.add(new SearchDocument(
                    summary.kind(), summary.timestamp(), List.of(summary.entry()), true));
        }
        collectDocuments(documents, savedTexts);
        if (documents.isEmpty()) {
            return List.of();
        }

        Set<String> queryTokens = explicitTokens(query);
        boolean blank = query == null || query.isBlank();
        SemanticPhraseMatcher matcher = blank ? null : safeMatcher(matcherSource);
        float[] queryVector = matcher == null ? null : safeEmbedQuery(matcher, query);
        List<RecordScored> scored = documents.stream()
                .map(document -> score(document, queryTokens, blank, queryVector))
                .filter(candidate -> eligible(candidate, blank, queryVector != null))
                .toList();
        List<RecordScored> ranked = new ArrayList<>(scored);
        ranked.sort(queryVector == null ? RECORD_WORD_RANK : fusedRecordRank(ranked));
        return List.copyOf(ranked);
    }

    private static void collectDocuments(List<SearchDocument> out, List<MemoryRecord> records) {
        for (MemoryRecord record : records) {
            out.add(new SearchDocument(record.kind(), record.timestamp(), record.entries(), false));
        }
    }

    /** Applies both per-item and total character budgets without changing the number of matching units. */
    private static List<String> boundedItems(List<RecordScored> ranked, int limit) {
        List<String> items = new ArrayList<>();
        int remaining = CompanionMemoryPolicy.searchResultMaxChars();
        for (RecordScored candidate : ranked) {
            if (items.size() == limit || remaining <= 0) {
                break;
            }
            String item = bound(candidate.document().render(), CompanionMemoryPolicy.searchItemMaxChars());
            if (item.length() > remaining) {
                item = bound(item, remaining);
            }
            if (!item.isBlank()) {
                items.add(item);
                remaining -= item.length();
            }
        }
        return List.copyOf(items);
    }

    private static String bound(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, Math.max(0, maxChars));
        }
        return text.substring(0, maxChars - 3).stripTrailing() + "...";
    }

    private static RecordScored score(
            SearchDocument document,
            Set<String> queryTokens,
            boolean blank,
            float[] queryVector
    ) {
        int wordScore = blank ? 0 : explicitOverlap(queryTokens, document.searchText());
        double semanticScore = Double.NaN;
        if (queryVector != null) {
            for (MemoryEntry entry : document.entries()) {
                if (entry.embedding() != null) {
                    semanticScore = maxScore(semanticScore, VectorMath.cosine(queryVector, entry.embedding()));
                }
            }
        }
        return new RecordScored(document, wordScore, semanticScore);
    }

    private static boolean eligible(RecordScored candidate, boolean blank, boolean semantic) {
        return blank || candidate.wordScore() > 0
                || (semantic && !Double.isNaN(candidate.semanticScore())
                && candidate.semanticScore() >= CompanionMemoryPolicy.semanticRecallFloor());
    }

    private static List<Scored> rankEntries(
            String query,
            List<MemoryRecord> recent,
            List<MemoryRecord> retained,
            List<MemorySearchMatch> summaries,
            List<MemoryRecord> savedTexts,
            Supplier<SemanticPhraseMatcher> matcherSource,
            SemanticQuery semanticQuery,
            Predicate<MemorySearchMatch> included
    ) {
        if (recent.isEmpty() && retained.isEmpty() && summaries.isEmpty() && savedTexts.isEmpty()) {
            return List.of();
        }
        Set<String> queryTokens = tokens(query);
        boolean blank = query == null || query.isBlank();
        SemanticPhraseMatcher matcher = blank ? null : safeMatcher(matcherSource);
        float[] queryVector = matcher == null || semanticQuery == null
                ? null : semanticQuery.vectorFor(query, matcher);
        if (queryVector == null && matcher != null) {
            queryVector = safeEmbedQuery(matcher, query);
        }

        List<Scored> scored = new ArrayList<>();
        collect(scored, recent, queryTokens, blank, queryVector, included);
        collect(scored, retained, queryTokens, blank, queryVector, included);
        collectMatches(scored, summaries, queryTokens, blank, queryVector, included);
        collect(scored, savedTexts, queryTokens, blank, queryVector, included);

        boolean semantic = queryVector != null;
        List<Scored> eligible = deduplicate(filter(scored, blank, semantic));
        eligible.sort(semantic ? fusedRank(eligible) : WORD_RANK);
        return eligible;
    }

    private static void collect(
            List<Scored> out,
            List<MemoryRecord> records,
            Set<String> queryTokens,
            boolean blank,
            float[] queryVector,
            Predicate<MemorySearchMatch> included
    ) {
        for (MemoryRecord record : records) {
            for (MemoryEntry entry : record.entries()) {
                MemorySearchMatch match = new MemorySearchMatch(record.kind(), record.timestamp(), entry);
                if (!included.test(match)) {
                    continue;
                }
                int wordScore = blank ? 0 : overlap(queryTokens, entry.content());
                double semanticScore = queryVector != null && entry.embedding() != null
                        ? VectorMath.cosine(queryVector, entry.embedding()) : Double.NaN;
                out.add(new Scored(match, wordScore, semanticScore));
            }
        }
    }

    private static void collectMatches(
            List<Scored> out,
            List<MemorySearchMatch> matches,
            Set<String> queryTokens,
            boolean blank,
            float[] queryVector,
            Predicate<MemorySearchMatch> included
    ) {
        for (MemorySearchMatch match : matches) {
            if (!included.test(match)) {
                continue;
            }
            MemoryEntry entry = match.entry();
            int wordScore = blank ? 0 : overlap(queryTokens, entry.content());
            double semanticScore = queryVector != null && entry.embedding() != null
                    ? VectorMath.cosine(queryVector, entry.embedding()) : Double.NaN;
            out.add(new Scored(match, wordScore, semanticScore));
        }
    }

    private static List<Scored> filter(List<Scored> scored, boolean blank, boolean semantic) {
        double floor = CompanionMemoryPolicy.semanticRecallFloor();
        return scored.stream()
                .filter(candidate -> blank || candidate.wordScore() > 0
                        || (semantic && !Double.isNaN(candidate.semanticScore())
                        && candidate.semanticScore() >= floor))
                .toList();
    }

    private static List<MemorySearchMatch> emitMatches(List<Scored> ranked, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<MemorySearchMatch> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Scored candidate : ranked) {
            MemorySearchMatch match = candidate.match();
            String identity = match.kind() + "\u0000" + match.entry().content();
            if (!seen.add(identity)) {
                continue;
            }
            out.add(match);
            if (out.size() == limit) {
                break;
            }
        }
        return List.copyOf(out);
    }

    private static final Comparator<Scored> WORD_RANK = Comparator
            .comparingInt(Scored::wordScore).reversed()
            .thenComparing(candidate -> candidate.match().timestamp(), Comparator.reverseOrder());

    private static final Comparator<RecordScored> RECORD_WORD_RANK = Comparator
            .comparingInt(RecordScored::wordScore).reversed()
            .thenComparing(candidate -> candidate.document().timestamp(), Comparator.reverseOrder());

    private static Comparator<Scored> fusedRank(List<Scored> eligible) {
        double floor = CompanionMemoryPolicy.semanticRecallFloor();
        Map<Scored, Double> fused = new IdentityHashMap<>();
        accumulateRank(fused, eligible, candidate -> candidate.wordScore() > 0, Scored::wordScore);
        accumulateRank(fused, eligible, candidate -> candidate.semanticScore() >= floor, Scored::semanticScore);
        return Comparator.<Scored>comparingDouble(candidate -> fused.getOrDefault(candidate, 0.0)).reversed()
                .thenComparing(candidate -> candidate.match().timestamp(), Comparator.reverseOrder());
    }

    private static Comparator<RecordScored> fusedRecordRank(List<RecordScored> eligible) {
        double floor = CompanionMemoryPolicy.semanticRecallFloor();
        Map<RecordScored, Double> fused = new IdentityHashMap<>();
        accumulateRecordRank(fused, eligible, candidate -> candidate.wordScore() > 0, RecordScored::wordScore);
        accumulateRecordRank(
                fused, eligible, candidate -> candidate.semanticScore() >= floor, RecordScored::semanticScore);
        return Comparator.<RecordScored>comparingDouble(candidate -> fused.getOrDefault(candidate, 0.0)).reversed()
                .thenComparing(candidate -> candidate.document().timestamp(), Comparator.reverseOrder());
    }

    private static void accumulateRank(
            Map<Scored, Double> fused,
            List<Scored> candidates,
            Predicate<Scored> included,
            ToDoubleFunction<Scored> score
    ) {
        List<Scored> ordered = candidates.stream()
                .filter(included)
                .sorted(Comparator.comparingDouble(score).reversed())
                .toList();
        int rank = 0;
        double previous = Double.NaN;
        for (int i = 0; i < ordered.size(); i++) {
            Scored candidate = ordered.get(i);
            double current = score.applyAsDouble(candidate);
            if (i > 0 && current != previous) {
                rank = i;
            }
            fused.merge(candidate, 1.0 / (RRF_K + rank), Double::sum);
            previous = current;
        }
    }

    private static void accumulateRecordRank(
            Map<RecordScored, Double> fused,
            List<RecordScored> candidates,
            Predicate<RecordScored> included,
            ToDoubleFunction<RecordScored> score
    ) {
        List<RecordScored> ordered = candidates.stream()
                .filter(included)
                .sorted(Comparator.comparingDouble(score).reversed())
                .toList();
        int rank = 0;
        double previous = Double.NaN;
        for (int i = 0; i < ordered.size(); i++) {
            RecordScored candidate = ordered.get(i);
            double current = score.applyAsDouble(candidate);
            if (i > 0 && current != previous) {
                rank = i;
            }
            fused.merge(candidate, 1.0 / (RRF_K + rank), Double::sum);
            previous = current;
        }
    }

    /** De-duplication stays within one kind, so saved text or EVENT evidence cannot be hidden by dialogue. */
    private static List<Scored> deduplicate(List<Scored> candidates) {
        double floor = CompanionMemoryPolicy.semanticDedupFloor();
        List<Scored> survivors = new ArrayList<>();
        for (Scored candidate : candidates) {
            int duplicate = duplicateOf(survivors, candidate, floor);
            if (duplicate < 0) {
                survivors.add(candidate);
            } else {
                survivors.set(duplicate, merge(survivors.get(duplicate), candidate));
            }
        }
        return survivors;
    }

    private static int duplicateOf(List<Scored> survivors, Scored candidate, double floor) {
        for (int i = 0; i < survivors.size(); i++) {
            Scored survivor = survivors.get(i);
            if (survivor.match().kind() == candidate.match().kind()
                    && sameMeaning(survivor.match().entry(), candidate.match().entry(), floor)) {
                return i;
            }
        }
        return -1;
    }

    private static Scored merge(Scored first, Scored second) {
        Scored representative = first.match().timestamp().isAfter(second.match().timestamp()) ? first : second;
        return new Scored(representative.match(), Math.max(first.wordScore(), second.wordScore()),
                maxScore(first.semanticScore(), second.semanticScore()));
    }

    private static double maxScore(double first, double second) {
        if (Double.isNaN(first)) {
            return second;
        }
        if (Double.isNaN(second)) {
            return first;
        }
        return Math.max(first, second);
    }

    private static float[] safeEmbedQuery(SemanticPhraseMatcher matcher, String query) {
        try {
            return matcher.embedQuery(query);
        } catch (RuntimeException failure) {
            log.warn("Query embedding failed; falling back to word-only recall", failure);
            return null;
        }
    }

    private static SemanticPhraseMatcher safeMatcher(Supplier<SemanticPhraseMatcher> matcherSource) {
        try {
            return matcherSource.get();
        } catch (RuntimeException failure) {
            log.warn("Semantic matcher is unavailable; falling back to word-only recall", failure);
            return null;
        }
    }

    private static int overlap(Set<String> queryTokens, String content) {
        Set<String> contentTokens = tokens(content);
        int score = 0;
        for (String token : queryTokens) {
            if (contentTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private static int explicitOverlap(Set<String> queryTokens, String content) {
        Set<String> contentTokens = explicitTokens(content);
        int score = 0;
        for (String token : queryTokens) {
            if (contentTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    /** Explicit recall keeps short words and stop words because they may be the exact remembered subject. */
    private static Set<String> explicitTokens(String text) {
        if (text == null) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+")) {
            if (!word.isBlank()) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private static Set<String> tokens(String text) {
        if (text == null) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+")) {
            if (word.length() > 2 && !InputNormalizerLocalizations.stopWords().contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private record Scored(MemorySearchMatch match, int wordScore, double semanticScore) {
    }

    private record RecordScored(SearchDocument document, int wordScore, double semanticScore) {
    }

    private record SearchDocument(
            MemoryKind kind,
            Instant timestamp,
            List<MemoryEntry> entries,
            boolean summary
    ) {

        private SearchDocument {
            entries = List.copyOf(entries);
        }

        private String searchText() {
            return entries.stream().map(MemoryEntry::content).collect(java.util.stream.Collectors.joining("\n"));
        }

        private String render() {
            if (summary) {
                return "[" + kind.name().toLowerCase(Locale.ROOT) + "_summary] "
                        + entries.getFirst().content();
            }
            return entries.stream()
                    .map(entry -> "[" + entry.source().displayLabel(CompanionConfig.companionName()) + "] "
                            + entry.content())
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
    }
}
