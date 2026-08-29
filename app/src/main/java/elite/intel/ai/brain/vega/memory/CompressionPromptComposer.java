package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.vega.model.llm.LlmMessage;
import elite.intel.ai.brain.vega.model.llm.LlmMessageRole;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.List;

/**
 * Builds the LLM request that shrinks one oversized memory line to its main point.
 */
public final class CompressionPromptComposer {

    /** Returns the proven single-line prompt for shrinking one oversized entry to its main point. */
    public List<LlmMessage> composeLineCompression(String content) {
        String instruction = "Rewrite the crew memory line below as ONE short sentence (about 15 words) that keeps "
                + "only its single most important point and drops secondary details, enumerations and coordinates. "
                + "Keep every fact and number you include exactly as in the source - never invent, change, "
                + "re-estimate or exaggerate it. Write numbers as digits, not spelled-out words. "
                + "Call speak exactly once with that sentence in speak.text; do not return free text. "
                + languageRule()
                + " Example of the target form: \"Lembava: independent high-tech system with bounty-hunting sites "
                + "and a conflict zone.\"";
        return List.of(
                LlmMessage.of(LlmMessageRole.SYSTEM, instruction),
                LlmMessage.of(LlmMessageRole.USER, content == null ? "" : content.strip()));
    }

    private static String languageRule() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return "The memory content is in " + language.displayName()
                + "; write the summary in " + language.displayName() + ".";
    }
}
