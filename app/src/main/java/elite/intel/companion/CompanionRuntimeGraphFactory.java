package elite.intel.companion;

import elite.intel.companion.clarify.ClarificationCoordinator;
import elite.intel.companion.confirm.CommandFlagDangerousActionPolicy;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.execution.CompanionExecutionGateway;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.execution.GenerationBoundExecutionGateway;
import elite.intel.companion.input.BargeInController;
import elite.intel.companion.llm.CompanionLlmGatewayFactory;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MidTermToLongTermConsolidator;
import elite.intel.companion.memory.SessionMemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.DispatcherCompanionNarrator;
import elite.intel.companion.mind.ThoughtDependencies;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.prompt.SemanticActionReducer;
import elite.intel.companion.speech.CompanionSpeechGateway;
import elite.intel.companion.speech.GenerationBoundSpeechGateway;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.SystemFunctionProvider;

/**
 * Transactional assembler for one {@link CompanionRuntimeGraph}. It transfers injected overrides into the same
 * lifecycle ownership as production gateways and closes every component already created if assembly fails.
 */
public final class CompanionRuntimeGraphFactory {

    private CompanionRuntimeGraphFactory() {
    }

    /**
     * Builds a complete but not yet started runtime graph. Non-null overrides are ownership transfers: closing
     * the graph closes the override through its gateway contract.
     */
    public static CompanionRuntimeGraph create(
            LlmGateway llmOverride,
            ExecutionGateway executionOverride,
            SpeechGateway speechOverride
    ) {
        CompanionRuntimeGeneration runtimeGeneration = new CompanionRuntimeGeneration();
        LlmGateway llmGateway = null;
        ExecutionGateway rawExecutionGateway = null;
        GenerationBoundExecutionGateway executionGateway = null;
        GenerationBoundSpeechGateway speechGateway = null;
        SessionMemoryGateway memoryGateway = null;
        MidTermToLongTermConsolidator memoryConsolidator = null;
        ThoughtDispatcher thoughtDispatcher = null;

        try {
            CompanionState companionState = new CompanionState();
            CompanionActionReducer actionReducer = new SemanticActionReducer();
            llmGateway = llmOverride != null ? llmOverride : CompanionLlmGatewayFactory.create();

            SpeechGateway rawSpeechGateway = speechOverride != null
                    ? speechOverride
                    : new CompanionSpeechGateway();
            speechGateway = new GenerationBoundSpeechGateway(rawSpeechGateway, runtimeGeneration);

            rawExecutionGateway = executionOverride != null
                    ? executionOverride
                    : new CompanionExecutionGateway();
            executionGateway = new GenerationBoundExecutionGateway(rawExecutionGateway, runtimeGeneration);

            memoryGateway = new SessionMemoryGateway();
            memoryConsolidator = new MidTermToLongTermConsolidator(
                    memoryGateway, llmGateway, speechGateway, runtimeGeneration);
            memoryGateway.setPendingConsolidationListener(memoryConsolidator);

            DangerousActionPolicy dangerousActionPolicy = new CommandFlagDangerousActionPolicy();
            ConfirmationCoordinator confirmationCoordinator = new ConfirmationCoordinator();
            ClarificationCoordinator clarificationCoordinator = new ClarificationCoordinator();
            ThoughtDependencies thoughtDependencies = new ThoughtDependencies(
                    llmGateway,
                    speechGateway,
                    executionGateway,
                    memoryGateway,
                    new PromptComposer(),
                    new IntelActionAccessPolicy(),
                    new SystemFunctionProvider(),
                    actionReducer,
                    companionState,
                    dangerousActionPolicy,
                    confirmationCoordinator,
                    clarificationCoordinator,
                    runtimeGeneration);
            thoughtDispatcher = new ThoughtDispatcher(thoughtDependencies);
            CompanionNarrator narrator = new DispatcherCompanionNarrator(
                    thoughtDispatcher, speechGateway, runtimeGeneration);
            BargeInController bargeInController = new BargeInController(thoughtDispatcher);

            return new CompanionRuntimeGraph(
                    runtimeGeneration,
                    llmGateway,
                    speechGateway,
                    executionGateway,
                    memoryGateway,
                    actionReducer,
                    companionState,
                    narrator,
                    thoughtDispatcher,
                    confirmationCoordinator,
                    bargeInController,
                    memoryConsolidator);
        } catch (RuntimeException | Error assemblyFailure) {
            if (memoryGateway != null) {
                SessionMemoryGateway partiallyBuiltMemoryGateway = memoryGateway;
                runCleanupAfterAssemblyFailure(assemblyFailure,
                        () -> partiallyBuiltMemoryGateway.setPendingConsolidationListener(null));
            }
            closeResourceAfterAssemblyFailure(assemblyFailure, thoughtDispatcher);
            closeResourceAfterAssemblyFailure(assemblyFailure, memoryConsolidator);
            closeResourceAfterAssemblyFailure(assemblyFailure, speechGateway);
            closeResourceAfterAssemblyFailure(assemblyFailure,
                    executionGateway != null ? executionGateway : rawExecutionGateway);
            closeResourceAfterAssemblyFailure(assemblyFailure, llmGateway);
            throw assemblyFailure;
        }
    }

    private static void closeResourceAfterAssemblyFailure(Throwable assemblyFailure, Object resource) {
        if (resource == null) {
            return;
        }
        try {
            if (resource instanceof ThoughtDispatcher dispatcher) {
                dispatcher.stop();
            } else if (resource instanceof AutoCloseable closeable) {
                closeable.close();
            }
        } catch (Exception | Error cleanupFailure) {
            assemblyFailure.addSuppressed(cleanupFailure);
        }
    }

    private static void runCleanupAfterAssemblyFailure(Throwable assemblyFailure, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | Error cleanupFailure) {
            assemblyFailure.addSuppressed(cleanupFailure);
        }
    }
}
