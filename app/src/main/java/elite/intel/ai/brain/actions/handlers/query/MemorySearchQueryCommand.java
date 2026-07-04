package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.companion.CompanionRuntime;
import elite.intel.util.json.JsonUtils;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Explicit, comprehensive recall over the companion's session memory. Unlike the answer-fact candidates that
 * are auto-injected each turn (a tier-2-filtered top few, meant to answer a single recall question), this
 * gathers EVERY matching memory entry for a subject the commander explicitly asks to look up - so a request
 * like "which stations did we dock at, and how many" has the complete set to answer from, which the capped
 * auto-candidates cannot give.
 * <p>
 * Read-only. Like the other queries it does not hand-format its own line: it hands the matches plus an
 * instruction to {@link BaseQueryAnalyzer#process}, so the analysis model composes the enumeration/count in
 * character and in the commander's language (the retrieval is deterministic; the wording is voiced the same
 * way cargo/biome results are). Companion-mode only - it reads {@link CompanionRuntime#memory()}.
 */
@RegisterQuery
public final class MemorySearchQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    public static final String ID = "memory_search";
    private static final String PARAM_QUERY = "query";
    /** Generous cap so an enumeration ("all stations") gets the full set, not the auto-candidate top few. */
    private static final int RECALL_LIMIT = 25;

    private static final String INSTRUCTIONS = """
            Answer the commander's question from the remembered entries below.
            Rules:
            - The 'remembered' list holds everything found in memory for this question. Report the entries
              relevant to what was asked, as a natural spoken answer.
            - If the commander asks how many, state the exact count of the relevant entries.
            - If the 'remembered' list is empty, say you have nothing in memory about that.
            """;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Search the companion's own session memory and report EVERY matching thing it remembers about a "
                + "subject. Use when the commander explicitly asks to recall, list, or count things from memory "
                + "(e.g. every station we docked at, everything said about a plan) - it returns the full set, "
                + "unlike the few facts already shown. Pass the subject to look up as the query.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(new ActionParameterSpec(
                PARAM_QUERY, "string", true,
                "The subject to recall from memory (a word or short phrase).",
                List.of(),
                "Extract the subject the commander wants recalled, in their own language; do not translate."));
    }

    /** Recalls every matching entry and lets the analysis model voice the enumeration/count (see class doc). */
    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) {
        String query = JsonUtils.getAsStringOrEmpty(params, PARAM_QUERY);
        List<String> matches;
        try {
            matches = CompanionRuntime.memory().recallMatching(query, RECALL_LIMIT);
        } catch (IllegalStateException companionNotInstalled) {
            // WHY: this query is companion-only; in the legacy router CompanionRuntime.memory() is not
            // installed and throws. Degrade to an empty result there. Any other failure must propagate.
            matches = List.of();
        }
        return process(new AiDataStruct(INSTRUCTIONS, new Remembered(strip(matches))), originalUserInput);
    }

    /** Strips the leading {@code [SOURCE]} label from each recalled entry so the model sees clean facts. */
    private static List<String> strip(List<String> matches) {
        return matches.stream().map(m -> m.replaceFirst("^\\[[^\\]]*\\]\\s*", "").strip()).collect(Collectors.toList());
    }

    /** The recalled entries, serialized to YAML for the analysis model. */
    record Remembered(List<String> remembered) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
