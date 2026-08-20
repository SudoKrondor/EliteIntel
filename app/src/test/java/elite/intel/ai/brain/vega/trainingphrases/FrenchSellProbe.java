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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * "Où vendre X" asks where to SELL. Nothing we have answers that, and find_commodity searches where to BUY,
 * so the honest outcome is that a sell question offers no market search at all. Deleting the sell aliases is
 * only half of that: embeddings capture topic and blur polarity, so buying and selling gold look alike to
 * the reducer. This prints what actually survives, for both the sell question and the buy question that
 * must keep working.
 */
@Tag("embedding-manual")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrenchSellProbe {

    private static final Set<IntelActionCategory> ALL = EnumSet.allOf(IntelActionCategory.class);
    private SemanticPhraseMatcher matcher;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        SystemSession.getInstance().setLanguage(Language.FR);
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        matcher = SemanticSearchProvider.matcher();
    }

    @Test
    void scoresFrenchBuyAndSellQuestions() {
        SystemSession.getInstance().setLanguage(Language.FR);
        List<String> utterances = List.of(
                "où vendre l'or",
                "où puis-je vendre mon or",
                "où vendre l'or dans un rayon de deux cents années-lumière",
                "où acheter de l'or",
                "où puis-je acheter de l'or dans un rayon de deux cents années-lumière");

        List<GameToolCandidates.Candidate> candidates = new GameToolCandidates().collect(ALL);
        for (String utterance : utterances) {
            float[] query = matcher.embedQuery(utterance);
            List<double[]> scored = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                GameToolCandidates.Candidate candidate = candidates.get(i);
                scored.add(new double[]{matcher.bestSimilarity(query, AliasEmbeddingText.phrases(
                        candidate.localizedAliasGroup(), candidate.tool().parameters())), i});
                ids.add(candidate.id());
            }
            scored.sort((a, b) -> Double.compare(b[0], a[0]));
            double best = scored.get(0)[0];
            System.out.printf("%n\"%s\"%n   floor 0.85 -> %s%n", utterance,
                    best < 0.85 ? "NO GAME TOOLS" : "offered:");
            for (int i = 0; i < 3; i++) {
                double[] row = scored.get(i);
                System.out.printf("     %.3f  %-24s %s%n", row[0], ids.get((int) row[1]),
                        best >= 0.85 && row[0] >= best - 0.04 ? "OFFERED" : "");
            }
        }
    }
}
