package elite.intel.companion.trainingphrases;

import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.ai.embed.VectorMath;
import elite.intel.companion.model.IntelActionCategory;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic, LLM-free diagnostics for the semantic reducer / training-phrase work. Boots only the
 * registries and the embedding model (no LM Studio) and dumps three things used to author and audit training
 * phrases and probes:
 * <ul>
 *   <li>{@link #dumpsCatalog} - the whole RU catalog (id, category, aliases, English purpose);</li>
 *   <li>{@link #dumpsParams} - each command's parameter schema, so probes can be audited for required values;</li>
 *   <li>{@link #dumpsAntonymCosines} - cosines for antonym/near pairs, illustrating that embeddings capture
 *       topic but blur polarity.</li>
 * </ul>
 * Opt-in ({@code embedding-manual}); requires the embedding model in {@code distribution/embed}.
 */
@Tag("embedding-manual")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SemanticReducerProbe {

    private static final Set<IntelActionCategory> ALL = EnumSet.allOf(IntelActionCategory.class);

    private SemanticPhraseMatcher matcher;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        SystemSession.getInstance().setLanguage(Language.RU);
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        matcher = SemanticSearchProvider.matcher();
    }

    /** Dumps the whole RU catalog (id, category, aliases, English purpose) so probe phrases can be authored. */
    @Test
    void dumpsCatalog() throws Exception {
        SystemSession.getInstance().setLanguage(Language.RU);
        List<GameToolCandidates.Candidate> all = new GameToolCandidates().collect(ALL);
        Set<String> commandIds = CommandRegistry.getInstance().byId().keySet();
        Set<String> queryIds = QueryRegistry.getInstance().byId().keySet();
        StringBuilder b = new StringBuilder("# id\tcategory\taliases\tpurpose\n");
        for (GameToolCandidates.Candidate c : all) {
            String cat = commandIds.contains(c.id()) ? "ACTION" : queryIds.contains(c.id()) ? "QUERY" : "MACRO";
            b.append(c.id()).append('\t').append(cat).append('\t')
                    .append(c.phraseKey().replace('\n', ' ')).append('\t')
                    .append(c.tool().description().replace('\n', ' ')).append('\n');
        }
        write("catalog-ru.txt", b.toString());
    }

    /** Dumps each command's parameter schema (name, type, required, enum) so probes can be audited for values. */
    @Test
    void dumpsParams() throws Exception {
        SystemSession.getInstance().setLanguage(Language.RU);
        List<GameToolCandidates.Candidate> all = new GameToolCandidates().collect(ALL);
        StringBuilder b = new StringBuilder();
        for (GameToolCandidates.Candidate c : all) {
            var params = c.tool().parameters();
            if (params == null || params.isEmpty()) {
                continue;
            }
            List<String> specs = new java.util.ArrayList<>();
            for (var p : params) {
                String enums = p.getEnumValues().isEmpty() ? "" : "=" + String.join("/", p.getEnumValues());
                specs.add(p.getName() + ":" + p.getType() + ":" + (p.isRequired() ? "req" : "opt") + enums);
            }
            b.append(c.id()).append('\t').append(String.join(", ", specs)).append('\n');
        }
        write("params-ru.txt", b.toString());
    }

    /** Prints cosine similarity for antonym/near pairs, showing that embeddings capture topic but blur polarity. */
    @Test
    void dumpsAntonymCosines() throws Exception {
        if (matcher == null) {
            return;
        }
        String[][] pairs = {
                {"уменьши скорость на", "увеличь скорость на"},
                {"медленнее", "быстрее"},
                {"включи свет", "выключи свет"},
                {"открой грузовой люк", "закрой грузовой люк"},
                {"добавь цель добычи", "удали цель добычи"},
                {"уменьши скорость на", "открой карту галактики"}, // unrelated baseline
        };
        StringBuilder b = new StringBuilder("==== ANTONYM / POLARITY COSINES ====\n");
        for (String[] p : pairs) {
            double cos = VectorMath.cosine(matcher.embedQuery(p[0]), matcher.embedQuery(p[1]));
            b.append(String.format(Locale.ROOT, "  %.3f   \"%s\"  vs  \"%s\"%n", cos, p[0], p[1]));
        }
        System.out.println(b);
        write("antonym-cosines.txt", b.toString());
    }

    private static void write(String name, String content) throws Exception {
        Path file = Paths.get("build", name).toAbsolutePath();
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        System.out.println("wrote " + file);
    }
}
