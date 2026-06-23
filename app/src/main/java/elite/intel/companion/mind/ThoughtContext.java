package elite.intel.companion.mind;

import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.speech.SpeechGateway;

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
        ConfirmationCoordinator confirmationCoordinator
) {}
