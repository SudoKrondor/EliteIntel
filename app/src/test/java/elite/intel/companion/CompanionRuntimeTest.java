package elite.intel.companion;

import com.google.gson.JsonObject;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.speech.SpeechGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies atomic runtime-graph publication, generation-safe removal, and guarded access while no graph is live.
 * Process-global static state, so each test uninstalls the graph afterwards.
 */
class CompanionRuntimeTest {

    private final SpeechGateway speech = (SpeechRequest r) -> CompletableFuture.completedFuture(null);
    private final ExecutionGateway execution = (ExecutionRequest r) -> CompletableFuture.completedFuture(new JsonObject());
    private final CompanionActionReducer reducer = (categories, input) -> List.of();
    private final CompanionState state = new CompanionState();

    @AfterEach
    void clear() {
        CompanionRuntimeTestSupport.clearInstalledGraph();
    }

    @Test
    void installedServicesAreReturned() {
        CompanionRuntimeTestSupport.install(null, speech, execution, null, reducer, state);

        assertSame(speech, CompanionRuntime.speech());
        assertSame(execution, CompanionRuntime.execution());
        assertSame(reducer, CompanionRuntime.reducer());
        assertSame(state, CompanionRuntime.state());
    }

    @Test
    void accessBeforeInstallThrows() {
        assertThrows(IllegalStateException.class, CompanionRuntime::state);
    }

    @Test
    void accessAfterClearThrows() {
        CompanionRuntimeGraph runtimeGraph = CompanionRuntimeTestSupport.install(
                null, speech, execution, null, reducer, state);
        CompanionRuntimeTestSupport.uninstall(runtimeGraph);

        assertThrows(IllegalStateException.class, CompanionRuntime::reducer);
    }

    @Test
    void staleGraphCannotUninstallANewerGeneration() {
        CompanionRuntimeGraph firstGeneration = CompanionRuntimeTestSupport.install(
                null, speech, execution, null, reducer, state);
        CompanionRuntimeTestSupport.uninstall(firstGeneration);
        CompanionState newerState = new CompanionState();
        CompanionRuntimeGraph secondGeneration = CompanionRuntimeTestSupport.install(
                null, speech, execution, null, reducer, newerState);

        assertFalse(CompanionRuntime.uninstallGraph(firstGeneration));
        assertSame(newerState, CompanionRuntime.state());

        CompanionRuntimeTestSupport.uninstall(secondGeneration);
    }

    @Test
    void closedGraphCannotBeInstalled() {
        CompanionRuntimeGraph runtimeGraph = CompanionRuntimeTestSupport.install(
                null, speech, execution, null, reducer, state);
        CompanionRuntimeTestSupport.uninstall(runtimeGraph);

        assertThrows(IllegalStateException.class, () -> CompanionRuntime.installGraph(runtimeGraph));
    }

    @Test
    void oldExecutionGenerationCannotUseANewerRuntime() throws Exception {
        CapturingNarrator newerNarrator = new CapturingNarrator();
        CompanionRuntimeGraph oldGraph = CompanionRuntimeTestSupport.installNarrator(CompanionNarrator.NO_OP);
        long oldGenerationId = oldGraph.runtimeGeneration().generationId();

        CompanionRuntime.callWithinGeneration(oldGenerationId, () -> {
            CompanionRuntimeTestSupport.uninstall(oldGraph);
            CompanionRuntimeTestSupport.installNarrator(newerNarrator);

            CompanionRuntime.narrator().filler("late old-generation speech", false);
            assertThrows(IllegalStateException.class, CompanionRuntime::state);
            return null;
        });

        CompanionRuntime.narrator().filler("current-generation speech", false);
        assertEquals(1, newerNarrator.submissions.get());
    }

    private static final class CapturingNarrator implements CompanionNarrator {
        private final AtomicInteger submissions = new AtomicInteger();

        @Override public void filler(String text, boolean urgent) { submissions.incrementAndGet(); }
        @Override public void narrate(String data, String instructions, String topic) { submissions.incrementAndGet(); }
        @Override public void announce(String sourceId, String phrase, String topic, boolean urgent) {
            submissions.incrementAndGet();
        }
    }
}
