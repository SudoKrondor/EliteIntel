package elite.intel.companion.confirm;

import elite.intel.companion.model.llm.LlmToolInvocation;

/**
 * Classifies whether a tool-call requires the commander's confirmation before it may run (§2.13). The
 * classification lives in code, never in the prompt: the LLM cannot decide that an action is safe.
 * <p>
 * Single abstract method, so a thought can be given a trivial "nothing is dangerous" policy in tests.
 */
@FunctionalInterface
public interface DangerousActionPolicy {

    /** Whether this tool invocation is a dangerous action that must be confirmed before execution. */
    boolean isDangerous(LlmToolInvocation invocation);
}
