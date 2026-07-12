package elite.intel.companion.mind;

import elite.intel.companion.CompanionRuntimeGeneration;
import elite.intel.companion.clarify.ClarificationCoordinator;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.tools.IntelActionTypeResolver;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.SystemFunctionProvider;

/**
 * Shared collaborators handed to every {@code Thought} by the {@code ThoughtDispatcher}. Bundles the
 * gateways, prompt/tool selection services, shared runtime state and the dangerous-action safety pair so
 * a thought has a single, stable dependency surface (and stays unit-testable without the static
 * {@code CompanionRuntime}). Per-thought input and routing data belongs to {@link ThoughtContext}, not here.
 */
public record ThoughtDependencies(
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
        ClarificationCoordinator clarificationCoordinator,
        CompanionRuntimeGeneration runtimeGeneration,
        IntelActionTypeResolver actionTypeResolver
) {
    /**
     * Backward-compatible constructor for call sites predating the action-type resolver; defaults it to the
     * registry-backed {@link IntelActionTypeResolver}. New code may pass an explicit resolver (e.g. a test seam).
     */
    public ThoughtDependencies(
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
                dangerousActionPolicy, confirmationCoordinator, new ClarificationCoordinator(),
                new CompanionRuntimeGeneration(),
                new IntelActionTypeResolver());
    }

    /** Constructor for call sites that share an explicit clarification coordinator but use default runtime seams. */
    public ThoughtDependencies(
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
            ClarificationCoordinator clarificationCoordinator) {
        this(llmGateway, speechGateway, executionGateway, memoryGateway, promptComposer,
                intelActionAccessPolicy, systemFunctionProvider, reducer, state,
                dangerousActionPolicy, confirmationCoordinator, clarificationCoordinator,
                new CompanionRuntimeGeneration(), new IntelActionTypeResolver());
    }

    /** Backward-compatible constructor for call sites that supply only an explicit action-type resolver. */
    public ThoughtDependencies(
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
            IntelActionTypeResolver actionTypeResolver) {
        this(llmGateway, speechGateway, executionGateway, memoryGateway, promptComposer,
                intelActionAccessPolicy, systemFunctionProvider, reducer, state,
                dangerousActionPolicy, confirmationCoordinator, new ClarificationCoordinator(),
                new CompanionRuntimeGeneration(),
                actionTypeResolver);
    }

    /** Constructor for tests that share both coordinators and supply an explicit action-type resolver. */
    public ThoughtDependencies(
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
            ClarificationCoordinator clarificationCoordinator,
            IntelActionTypeResolver actionTypeResolver) {
        this(llmGateway, speechGateway, executionGateway, memoryGateway, promptComposer,
                intelActionAccessPolicy, systemFunctionProvider, reducer, state,
                dangerousActionPolicy, confirmationCoordinator, clarificationCoordinator,
                new CompanionRuntimeGeneration(), actionTypeResolver);
    }

    /** Production constructor that binds every thought to one runtime generation. */
    public ThoughtDependencies(
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
            CompanionRuntimeGeneration runtimeGeneration) {
        this(llmGateway, speechGateway, executionGateway, memoryGateway, promptComposer,
                intelActionAccessPolicy, systemFunctionProvider, reducer, state,
                dangerousActionPolicy, confirmationCoordinator, new ClarificationCoordinator(), runtimeGeneration,
                new IntelActionTypeResolver());
    }

    /** Production constructor sharing the runtime-owned confirmation and clarification coordinators. */
    public ThoughtDependencies(
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
            ClarificationCoordinator clarificationCoordinator,
            CompanionRuntimeGeneration runtimeGeneration) {
        this(llmGateway, speechGateway, executionGateway, memoryGateway, promptComposer,
                intelActionAccessPolicy, systemFunctionProvider, reducer, state,
                dangerousActionPolicy, confirmationCoordinator, clarificationCoordinator, runtimeGeneration,
                new IntelActionTypeResolver());
    }
}
