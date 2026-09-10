package elite.intel.ai.brain.vega;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.mind.VegaState;
import elite.intel.ai.brain.vega.model.execution.ExecutionRequest;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import elite.intel.ai.brain.vega.prompt.VegaActionReducer;
import elite.intel.ai.brain.vega.speech.SpeechGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies atomic runtime-graph publication, generation-safe removal, and guarded access while no graph is live.
 * Process-global static state, so each test uninstalls the graph afterwards.
 */
class VegaRuntimeTest {

    private final SpeechGateway speech = (SpeechRequest r) -> CompletableFuture.completedFuture(null);
    private final ExecutionGateway execution = (ExecutionRequest r) -> CompletableFuture.completedFuture(new JsonObject());
    private final VegaActionReducer reducer = (categories, input) -> List.of();
    private final VegaState state = new VegaState();

    @AfterEach
    void clear() {
        VegaRuntimeTestSupport.clearInstalledGraph();
    }

    @Test
    void installedServicesAreReturned() {
        VegaRuntimeTestSupport.install(null, speech, execution, null, reducer, state);

        assertSame(speech, VegaRuntime.speech());
        assertSame(execution, VegaRuntime.execution());
        assertSame(reducer, VegaRuntime.reducer());
        assertSame(state, VegaRuntime.state());
    }

    @Test
    void accessBeforeInstallThrows() {
        assertThrows(IllegalStateException.class, VegaRuntime::state);
    }

    @Test
    void accessAfterClearThrows() {
        VegaRuntimeGraph runtimeGraph = VegaRuntimeTestSupport.install(
                null, speech, execution, null, reducer, state);
        VegaRuntimeTestSupport.uninstall(runtimeGraph);

        assertThrows(IllegalStateException.class, VegaRuntime::reducer);
    }

    @Test
    void staleGraphCannotUninstallANewerGeneration() {
        VegaRuntimeGraph firstGeneration = VegaRuntimeTestSupport.install(
                null, speech, execution, null, reducer, state);
        VegaRuntimeTestSupport.uninstall(firstGeneration);
        VegaState newerState = new VegaState();
        VegaRuntimeGraph secondGeneration = VegaRuntimeTestSupport.install(
                null, speech, execution, null, reducer, newerState);

        assertFalse(VegaRuntime.uninstallGraph(firstGeneration));
        assertSame(newerState, VegaRuntime.state());

        VegaRuntimeTestSupport.uninstall(secondGeneration);
    }

    @Test
    void closedGraphCannotBeInstalled() {
        VegaRuntimeGraph runtimeGraph = VegaRuntimeTestSupport.install(
                null, speech, execution, null, reducer, state);
        VegaRuntimeTestSupport.uninstall(runtimeGraph);

        assertThrows(IllegalStateException.class, () -> VegaRuntime.installGraph(runtimeGraph));
    }

    @Test
    void oldExecutionGenerationCannotUseANewerRuntime() throws Exception {
        CapturingNarrator newerNarrator = new CapturingNarrator();
        VegaRuntimeGraph oldGraph = VegaRuntimeTestSupport.installNarrator(VegaNarrator.NO_OP);
        long oldGenerationId = oldGraph.runtimeGeneration().generationId();

        VegaRuntime.callWithinGeneration(oldGenerationId, () -> {
            VegaRuntimeTestSupport.uninstall(oldGraph);
            VegaRuntimeTestSupport.installNarrator(newerNarrator);

            VegaRuntime.narrator().filler("late old-generation speech", false);
            assertThrows(IllegalStateException.class, VegaRuntime::state);
            return null;
        });

        VegaRuntime.narrator().filler("current-generation speech", false);
        assertEquals(1, newerNarrator.submissions.get());
    }

    private static final class CapturingNarrator implements VegaNarrator {
        private final AtomicInteger submissions = new AtomicInteger();

        @Override
        public void filler(String text, boolean urgent) {
            submissions.incrementAndGet();
        }

        @Override
        public void narrate(String data, String instructions) {
            submissions.incrementAndGet();
        }

        @Override
        public void announce(String phrase, boolean urgent) {
            submissions.incrementAndGet();
        }
    }
}
