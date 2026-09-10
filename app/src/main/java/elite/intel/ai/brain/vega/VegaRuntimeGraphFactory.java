package elite.intel.ai.brain.vega;

import elite.intel.ai.brain.vega.clarify.ClarificationCoordinator;
import elite.intel.ai.brain.vega.confirm.CommandFlagDangerousActionPolicy;
import elite.intel.ai.brain.vega.confirm.ConfirmationCoordinator;
import elite.intel.ai.brain.vega.confirm.DangerousActionPolicy;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.execution.GenerationBoundExecutionGateway;
import elite.intel.ai.brain.vega.execution.VegaExecutionGateway;
import elite.intel.ai.brain.vega.input.BargeInController;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.llm.VegaLlmGatewayFactory;
import elite.intel.ai.brain.vega.memory.OversizedMemoryCompressor;
import elite.intel.ai.brain.vega.memory.SessionMemoryGateway;
import elite.intel.ai.brain.vega.mind.DispatcherVegaNarrator;
import elite.intel.ai.brain.vega.mind.ThoughtDependencies;
import elite.intel.ai.brain.vega.mind.ThoughtDispatcher;
import elite.intel.ai.brain.vega.mind.VegaState;
import elite.intel.ai.brain.vega.prompt.IntelActionAccessPolicy;
import elite.intel.ai.brain.vega.prompt.PromptComposer;
import elite.intel.ai.brain.vega.prompt.SemanticActionReducer;
import elite.intel.ai.brain.vega.prompt.VegaActionReducer;
import elite.intel.ai.brain.vega.speech.GenerationBoundSpeechGateway;
import elite.intel.ai.brain.vega.speech.SpeechGateway;
import elite.intel.ai.brain.vega.speech.VegaSpeechGateway;
import elite.intel.ai.brain.vega.tools.SystemFunctionProvider;

/**
 * Transactional assembler for one {@link VegaRuntimeGraph}. It transfers injected overrides into the same
 * lifecycle ownership as production gateways and closes every component already created if assembly fails.
 */
public final class VegaRuntimeGraphFactory {

    private VegaRuntimeGraphFactory() {
    }

    /**
     * Builds a complete but not yet started runtime graph. Non-null overrides are ownership transfers: closing
     * the graph closes the override through its gateway contract.
     */
    public static VegaRuntimeGraph create(
            LlmGateway llmOverride,
            ExecutionGateway executionOverride,
            SpeechGateway speechOverride
    ) {
        VegaRuntimeGeneration runtimeGeneration = new VegaRuntimeGeneration();
        LlmGateway llmGateway = null;
        ExecutionGateway rawExecutionGateway = null;
        GenerationBoundExecutionGateway executionGateway = null;
        GenerationBoundSpeechGateway speechGateway = null;
        SessionMemoryGateway memoryGateway = null;
        OversizedMemoryCompressor oversizedMemoryCompressor = null;
        ThoughtDispatcher thoughtDispatcher = null;

        try {
            VegaState vegaState = new VegaState();
            VegaActionReducer actionReducer = new SemanticActionReducer();
            llmGateway = llmOverride != null ? llmOverride : VegaLlmGatewayFactory.create();

            SpeechGateway rawSpeechGateway = speechOverride != null
                    ? speechOverride
                    : new VegaSpeechGateway();
            speechGateway = new GenerationBoundSpeechGateway(rawSpeechGateway, runtimeGeneration);

            rawExecutionGateway = executionOverride != null
                    ? executionOverride
                    : new VegaExecutionGateway();
            executionGateway = new GenerationBoundExecutionGateway(rawExecutionGateway, runtimeGeneration);

            memoryGateway = new SessionMemoryGateway();
            oversizedMemoryCompressor = new OversizedMemoryCompressor(
                    memoryGateway, llmGateway, runtimeGeneration);
            memoryGateway.setOversizedMemoryListener(oversizedMemoryCompressor);

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
                    vegaState,
                    dangerousActionPolicy,
                    confirmationCoordinator,
                    clarificationCoordinator,
                    runtimeGeneration);
            thoughtDispatcher = new ThoughtDispatcher(thoughtDependencies);
            VegaNarrator narrator = new DispatcherVegaNarrator(
                    thoughtDispatcher, speechGateway, runtimeGeneration);
            BargeInController bargeInController = new BargeInController(thoughtDispatcher);

            return new VegaRuntimeGraph(
                    runtimeGeneration,
                    llmGateway,
                    speechGateway,
                    executionGateway,
                    memoryGateway,
                    actionReducer,
                    vegaState,
                    narrator,
                    thoughtDispatcher,
                    confirmationCoordinator,
                    bargeInController,
                    oversizedMemoryCompressor);
        } catch (RuntimeException | Error assemblyFailure) {
            if (memoryGateway != null) {
                SessionMemoryGateway partiallyBuiltMemoryGateway = memoryGateway;
                runCleanupAfterAssemblyFailure(assemblyFailure,
                        () -> partiallyBuiltMemoryGateway.setOversizedMemoryListener(null));
            }
            closeResourceAfterAssemblyFailure(assemblyFailure, thoughtDispatcher);
            closeResourceAfterAssemblyFailure(assemblyFailure, oversizedMemoryCompressor);
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
