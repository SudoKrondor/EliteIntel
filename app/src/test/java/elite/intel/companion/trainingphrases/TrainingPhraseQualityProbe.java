package elite.intel.companion.trainingphrases;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.prompt.AliasMatchSurface;
import elite.intel.companion.prompt.GameToolCandidates;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic (no LLM) training-phrase quality probe. Reads authored probe phrases from
 * {@code src/test/resources/trainingphrases/probe-phrases-<lang>.json} ({id: [phrases]}), runs each through the
 * alias-only semantic matcher (via {@link AliasMatchSurface}), and produces a per-command diagnosis: the probes
 * with their outcome, the existing training phrases and their gap to each probe, the competing phrases that
 * outrank the tool, the dominant conflict, and a concrete suggestion of which training phrases to add. Written
 * as CSV (weak-first) to {@code build/training-phrase-quality-<lang>.csv} for the Excel summary
 * ({@code scripts/build_training_xlsx.py}). Opt-in ({@code embedding-manual}); requires the embedding model.
 */
@Tag("embedding-manual")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainingPhraseQualityProbe {

    private static final Set<IntelActionCategory> ALL = EnumSet.allOf(IntelActionCategory.class);
    private static final double SEM_FLOOR = 0.80;
    private static final double SEM_MARGIN = 0.04;
    private static final int SEM_MAX = 8;

    private List<GameToolCandidates.Candidate> candidates;
    private SemanticPhraseMatcher matcher;
    private Set<String> commandIds;
    private Set<String> queryIds;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        matcher = SemanticSearchProvider.matcher();
        commandIds = CommandRegistry.getInstance().byId().keySet();
        queryIds = QueryRegistry.getInstance().byId().keySet();
    }

    /**
     * A scored tool. The per-probe detail is split into four line-aligned columns ({@code probe}, {@code rank},
     * {@code own}, {@code competitor}) so each probe reads across one line instead of one crammed cell.
     */
    private record Row(int offered, int hit1, String id, String category, String verdict, String score,
                       String conflictGroup, String probe, String rank, String own, String competitor,
                       String existing, String suggestions) {}

    @Test
    void scoresRussianTrainingPhraseQualityToCsv() throws Exception {
        scoreLanguage(Language.RU, "ru", false);
    }

    @Test
    void scoresEnglishTrainingPhraseQualityToCsv() throws Exception {
        scoreLanguage(Language.EN, "en", true);
    }

    private void scoreLanguage(Language language, String suffix, boolean allowAliasSeed) throws Exception {
        SystemSession.getInstance().setLanguage(language);
        candidates = new GameToolCandidates().collect(ALL);
        Path input = Paths.get("src", "test", "resources", "trainingphrases",
                "probe-phrases-" + suffix + ".json").toAbsolutePath();
        if (matcher == null) {
            System.out.println("skipped " + suffix + ": matcher=false");
            return;
        }
        Map<String, List<String>> probes = loadProbes(input, allowAliasSeed);
        if (probes.isEmpty()) {
            System.out.println("skipped " + suffix + ": phrasesFile=" + Files.exists(input));
            return;
        }

        List<Row> rows = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : probes.entrySet()) {
            int index = indexOf(entry.getKey());
            if (index >= 0 && !entry.getValue().isEmpty()) {
                rows.add(scoreTool(entry.getKey(), index, entry.getValue()));
            }
        }
        rows.sort(Comparator.comparingInt(Row::offered).thenComparingInt(Row::hit1)); // weak first

        StringBuilder csv = new StringBuilder(
                "id,category,verdict,score,conflict_group,probe,rank,own_match,competitor,existing_phrases,suggested_additions\n");
        for (Row r : rows) {
            csv.append(String.join(",", List.of(csv(r.id()), csv(r.category()), csv(r.verdict()), csv(r.score()),
                    csv(r.conflictGroup()), csv(r.probe()), csv(r.rank()), csv(r.own()), csv(r.competitor()),
                    csv(r.existing()), csv(r.suggestions())))).append('\n');
        }
        Path out = Paths.get("build", "training-phrase-quality-" + suffix + ".csv").toAbsolutePath();
        Files.writeString(out, csv.toString(), StandardCharsets.UTF_8);
        System.out.printf("scored %s %d tools -> %s%n", suffix, rows.size(), out);
    }

    private Map<String, List<String>> loadProbes(Path input, boolean allowAliasSeed) throws Exception {
        if (Files.exists(input)) {
            return new Gson().fromJson(Files.readString(input),
                    new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
        }
        if (!allowAliasSeed) {
            return Map.of();
        }
        System.out.println("probe seed: " + input + " missing; using localized aliases as EN probe seed");
        Map<String, List<String>> probes = new LinkedHashMap<>();
        for (GameToolCandidates.Candidate candidate : candidates) {
            List<String> phrases = AliasMatchSurface.phrases(candidate.phraseKey(), candidate.tool().parameters())
                    .stream()
                    .map(TrainingPhraseQualityProbe::clean)
                    .distinct()
                    .limit(10)
                    .toList();
            if (!phrases.isEmpty()) {
                probes.put(candidate.id(), phrases);
            }
        }
        return probes;
    }

    private Row scoreTool(String id, int index, List<String> phrases) {
        List<String> ownAliases = aliasesOf(index);
        int hit1 = 0;
        int offeredCount = 0;
        List<String> probeCol = new ArrayList<>();
        List<String> rankCol = new ArrayList<>();
        List<String> ownCol = new ArrayList<>();
        List<String> compCol = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        Map<String, Integer> competitorCounts = new LinkedHashMap<>();

        for (String phrase : phrases) {
            float[] query = matcher.embedQuery(phrase);
            double[] score = new double[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                score[i] = matcher.bestSimilarity(query, aliasesOf(i));
            }
            List<Integer> order = order(score);
            int rank = 1 + order.indexOf(index);
            Set<String> offered = offered(score, order);

            probeCol.add(phrase);
            rankCol.add(String.valueOf(rank));
            ownCol.add(String.format(Locale.ROOT, "%s (%.2f)", clean(bestPhrase(query, ownAliases)), score[index]));
            if (rank == 1) {
                hit1++;
                compCol.add("—");
            } else {
                int topIndex = order.get(0);
                String compId = candidates.get(topIndex).id();
                compCol.add(String.format(Locale.ROOT, "%s: %s (%.2f)", compId,
                        clean(bestPhrase(query, aliasesOf(topIndex))), score[topIndex]));
                competitorCounts.merge(compId, 1, Integer::sum);
                suggestions.add(phrase);
            }
            if (offered.contains(id)) {
                offeredCount++;
            }
        }

        int n = phrases.size();
        String category = commandIds.contains(id) ? "ACTION" : queryIds.contains(id) ? "QUERY" : "MACRO";
        // OK = always offered; WATCH = missed once; WEAK = missed twice or more (correct command hidden from the LLM).
        String verdict = offeredCount >= n ? "OK" : offeredCount >= n - 1 ? "WATCH" : "WEAK";
        String score = String.format(Locale.ROOT, "hit@1 %d/%d, offered %d/%d", hit1, n, offeredCount, n);
        String existing = String.join("\n", ownAliases.stream().map(TrainingPhraseQualityProbe::clean).toList());
        String suggestionsStr = suggestions.isEmpty() ? "—"
                : String.join("\n", suggestions.stream().map(p -> "+ " + p).toList());
        String conflictGroup = competitorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " ×" + e.getValue()).orElse("—");
        return new Row(offeredCount, hit1, id, category, verdict, score, conflictGroup,
                String.join("\n", probeCol), String.join("\n", rankCol),
                String.join("\n", ownCol), String.join("\n", compCol), existing, suggestionsStr);
    }

    /** The embedding-ready alias surface for a candidate (param placeholders substituted / stripped). */
    private List<String> aliasesOf(int index) {
        GameToolCandidates.Candidate candidate = candidates.get(index);
        return AliasMatchSurface.phrases(candidate.phraseKey(), candidate.tool().parameters());
    }

    /** The phrase that best matches the query among the given aliases, or "". */
    private String bestPhrase(float[] query, List<String> aliases) {
        SemanticPhraseMatcher.Match match = matcher.bestMatch(query, aliases);
        return match.index() >= 0 ? aliases.get(match.index()) : "";
    }

    private List<Integer> order(double[] score) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < score.length; i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble((Integer i) -> score[i]).reversed());
        return order;
    }

    private Set<String> offered(double[] score, List<Integer> order) {
        Set<String> ids = new LinkedHashSet<>();
        double best = score[order.get(0)];
        if (best < SEM_FLOOR) {
            return ids;
        }
        double cutoff = best - SEM_MARGIN;
        for (int idx : order) {
            if (ids.size() >= SEM_MAX || score[idx] < cutoff) {
                break;
            }
            ids.add(candidates.get(idx).id());
        }
        return ids;
    }

    private int indexOf(String id) {
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** Strips {@code {key:X}} parameter annotations from an alias for readable display. */
    private static String clean(String alias) {
        return alias.replaceAll("\\{[^}]*}", "").replaceAll("\\s{2,}", " ").strip();
    }

    /** Minimal CSV escaping: quote fields containing a comma, quote, or newline. */
    private static String csv(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return '"' + field.replace("\"", "\"\"") + '"';
        }
        return field;
    }
}
