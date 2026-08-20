package elite.intel.ai.brain.vega.trainingphrases;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.brain.vega.prompt.AliasEmbeddingText;
import elite.intel.ai.brain.vega.prompt.GameToolCandidates;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Scores the utterances from the 05:58 commander turns against the live catalog, to show whether the radius
 * aliases lift them over the reducer floor. Opt-in ({@code embedding-manual}); needs the embedding model.
 */
@Tag("embedding-manual")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RadiusUtteranceProbe {

    private static final Set<IntelActionCategory> ALL = EnumSet.allOf(IntelActionCategory.class);
    private SemanticPhraseMatcher matcher;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        SystemSession.getInstance().setLanguage(Language.EN);
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        matcher = SemanticSearchProvider.matcher();
    }

    @Test
    void scoresTheLoggedUtterances() {
        // The first two are verbatim from the 05:58 commander turns that scored 0.844 and 0.846 and were
        // answered with an invented refusal ("I can't plot a search radius for commodities").
        java.util.Map<String, String> expected = new java.util.LinkedHashMap<>();
        expected.put("find where we can find the neo fabric insulation within two hundred light years", "find_commodity");
        expected.put("find where we can buy neo fabric installation within two hundred light years", "find_commodity");
        expected.put("find where to buy gold", "find_commodity");
        expected.put("find where we can mine painite within three hundred light years", "find_mining_site");
        List<String> utterances = List.copyOf(expected.keySet());

        List<GameToolCandidates.Candidate> candidates = new GameToolCandidates().collect(ALL);
        for (String utterance : utterances) {
            float[] query = matcher.embedQuery(utterance);
            java.util.List<double[]> scored = new java.util.ArrayList<>();
            java.util.List<String> ids = new java.util.ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                GameToolCandidates.Candidate candidate = candidates.get(i);
                double score = matcher.bestSimilarity(query, AliasEmbeddingText.phrases(
                        candidate.localizedAliasGroup(), candidate.tool().parameters()));
                scored.add(new double[]{score, i});
                ids.add(candidate.id());
            }
            scored.sort((a, b) -> Double.compare(b[0], a[0]));
            double best = scored.get(0)[0];
            org.junit.jupiter.api.Assertions.assertTrue(best >= 0.85,
                    () -> String.format("'%s' scores %.3f, below the reducer floor - no game tool is offered "
                            + "and the model invents a refusal", utterance, best));
            org.junit.jupiter.api.Assertions.assertTrue(
                    scored.stream().limit(8).anyMatch(row -> ids.get((int) row[1]).equals(expected.get(utterance))
                            && row[0] >= best - 0.04),
                    () -> expected.get(utterance) + " is not in the offered band for '" + utterance + "'");
            System.out.printf("%n\"%s\"%n  floor 0.85, cutoff %.3f%n", utterance, best - 0.04);
            for (int i = 0; i < 4; i++) {
                double[] row = scored.get(i);
                System.out.printf("    %.3f  %-24s %s%n", row[0], ids.get((int) row[1]),
                        row[0] >= best - 0.04 && best >= 0.85 ? "OFFERED" : "");
            }
        }
    }
}
