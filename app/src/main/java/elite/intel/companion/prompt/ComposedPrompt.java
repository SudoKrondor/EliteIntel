package elite.intel.companion.prompt;

import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.PromptCacheProfile;

import java.util.List;

/**
 * Output of {@link PromptComposer} for a consciousness turn: the seed message flow (system + user),
 * the native tool set, and the prompt cache profile.
 * <p>
 * The stable-prefix vs. dynamic-context sectioning and the cache-friendly ordering are handled
 * privately inside the composer when it assembles the {@code system} message content (stable text
 * first, so Mistral can reuse the cached prefix); they are not carried here.
 *
 * @param messages  initial message flow (typically [system, user]); the thought grows it across tool rounds
 * @param tools     native tool definitions offered this turn (reduced game tools + system functions)
 * @param profile   prompt cache profile for the resulting request
 */
public record ComposedPrompt(
        List<LlmMessage> messages,
        List<LlmToolDefinition> tools,
        PromptCacheProfile profile
) {}
