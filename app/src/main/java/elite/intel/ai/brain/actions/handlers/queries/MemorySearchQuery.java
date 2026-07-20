package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.brain.vega.memory.MemorySearchResult;
import elite.intel.util.json.JsonUtils;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;

/**
 * Explicit record-level recall over every session-memory area. Exact counts are exposed only while matching
 * records still retain their individual form; the returned item list is always bounded.
 */
@RegisterQuery
public final class MemorySearchQuery extends BaseQueryAnalyzer implements IntelQuery {

    public static final String ID = "memory_search";
    private static final String PARAM_QUERY = "query";
    private static final int RECALL_LIMIT = 25;

    private static final String INSTRUCTIONS = """
            Answer the commander's question from the remembered entries below.
            Rules:
            - exactRecordCount is an exact count only when it is a number. If it is null, matching older records
              have been summarized and an exact historical count is unavailable.
            - matchingUnits is the number of matching retrieval units, including summaries. Never present it as
              a factual count of events or conversations.
            - items contains the highest-ranked matching units that fit the response limit. Preserve their
              provenance when answering.
            - If truncated is true, do not claim that items is a complete list.
            - If matchingUnits is zero, say you have nothing in memory about the subject.
            """;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Search session memory for a subject. Returns bounded matches and an exact count when records are not summarized.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return List.of(new ActionParameterSpec(
                PARAM_QUERY, "string", true,
                "The subject to recall from memory (a word or short phrase).",
                List.of(),
                "Extract the subject the commander wants recalled, in their own language; do not translate."));
    }

    /** Recalls matching records and lets the analysis model voice the result. */
    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) {
        String query = JsonUtils.getAsStringOrEmpty(params, PARAM_QUERY);
        MemorySearchResult result;
        try {
            result = CompanionRuntime.memory().recallMatching(query, RECALL_LIMIT);
        } catch (IllegalStateException companionNotInstalled) {
            result = MemorySearchResult.empty();
        }
        return process(new AiDataStruct(INSTRUCTIONS,
                new Remembered(result.exactRecordCount(), result.matchingUnits(), result.truncated(), result.items())),
                originalUserInput);
    }

    /** Structured recall data serialized for the analysis model. */
    record Remembered(
            Integer exactRecordCount,
            int matchingUnits,
            boolean truncated,
            List<String> items
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
