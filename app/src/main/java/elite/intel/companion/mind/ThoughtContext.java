package elite.intel.companion.mind;

import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.CompanionNarrationPolicy;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.SystemFunctionProvider;

/**
 * Shared collaborators handed to every {@code Thought} by the {@code ThoughtDispatcher}. Bundles the
 * gateways, prompt/tool selection services, shared runtime state and the dangerous-action safety pair so
 * a thought has a single, stable dependency surface (and stays unit-testable without the static
 * {@code CompanionRuntime}).
 */
public record ThoughtContext(
        LlmGateway llmGateway,
        SpeechGateway speechGateway,
        ExecutionGateway executionGateway,
        MemoryGateway memoryGateway,
        PromptComposer promptComposer,
        IntelActionAccessPolicy intelActionAccessPolicy,
        SystemFunctionProvider systemFunctionProvider,
        CompanionActionReducer reducer,
        CompanionState state,
        DangerousActionPolicy dangerousActionPolicy,
        ConfirmationCoordinator confirmationCoordinator,
        CompanionNarrationPolicy narrationPolicy
) {
    /**
     * Backward-compatible constructor for call sites predating the narration policy; defaults it to the
     * registry-backed {@link CompanionNarrationPolicy}. New code may pass an explicit policy (e.g. a test seam).
     */
    public ThoughtContext(
            LlmGateway llmGateway,
            SpeechGateway speechGateway,
            ExecutionGateway executionGateway,
            MemoryGateway memoryGateway,
            PromptComposer promptComposer,
            IntelActionAccessPolicy intelActionAccessPolicy,
            SystemFunctionProvider systemFunctionProvider,
            CompanionActionReducer reducer,
            CompanionState state,
            DangerousActionPolicy dangerousActionPolicy,
            ConfirmationCoordinator confirmationCoordinator) {
        this(llmGateway, speechGateway, executionGateway, memoryGateway, promptComposer,
                intelActionAccessPolicy, systemFunctionProvider, reducer, state,
                dangerousActionPolicy, confirmationCoordinator, new CompanionNarrationPolicy());
    }
}
