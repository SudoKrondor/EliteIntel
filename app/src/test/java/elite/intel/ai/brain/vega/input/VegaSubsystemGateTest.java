package elite.intel.ai.brain.vega.input;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.ai.brain.vega.VegaRuntimeGraph;
import elite.intel.ai.brain.vega.VegaRuntimeTestSupport;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.mind.VegaState;
import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.LlmResult;
import elite.intel.ai.brain.vega.speech.SpeechGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class VegaSubsystemGateTest {

    private static final Set<String> OWNED_THREAD_NAMES = Set.of(
            "vega-commander",
            "vega-event",
            "vega-watchdog",
            "vega-llm",
            "vega-action",
            "vega-query",
            "vega-memory-compressor");

    private VegaSubsystemGate gate;
    private VegaRuntimeGraph separatelyInstalledGraph;

    @AfterEach
    void cleanUpRuntime() {
        if (gate != null) {
            gate.stop();
        }
        VegaRuntimeTestSupport.uninstall(separatelyInstalledGraph);
        VegaRuntimeTestSupport.clearInstalledGraph();
    }

    @Test
    void repeatedRestartCyclesLeaveNoRuntimeOrOwnedThreads() throws Exception {
        long threadCountBeforeCycles = ownedThreadCount();
        gate = gateWithResourceFreeOverrides();

        for (int cycle = 0; cycle < 100; cycle++) {
            gate.start();
            assertNotNull(gate.dispatcher());
            assertNotNull(VegaRuntime.state());

            gate.stop();
            assertNull(gate.dispatcher());
            assertThrows(IllegalStateException.class, VegaRuntime::state);
        }

        awaitOwnedThreadCount(threadCountBeforeCycles);
        assertEquals(threadCountBeforeCycles, ownedThreadCount());
    }

    @Test
    void failedPublicationRollsBackTheNewGraphWithoutClearingTheInstalledGeneration() throws Exception {
        VegaState installedState = new VegaState();
        separatelyInstalledGraph = VegaRuntimeTestSupport.install(
                null, null, null, null, null, installedState);
        long threadCountBeforeAttempt = ownedThreadCount();
        gate = gateWithResourceFreeOverrides();

        assertThrows(IllegalStateException.class, gate::start);

        assertNull(gate.dispatcher());
        assertSame(installedState, VegaRuntime.state());
        awaitOwnedThreadCount(threadCountBeforeAttempt);
        assertEquals(threadCountBeforeAttempt, ownedThreadCount());
    }

    private static VegaSubsystemGate gateWithResourceFreeOverrides() {
        LlmGateway llmGateway = new LlmGateway() {
            @Override
            public CompletableFuture<LlmResult> submit(LlmRequest request) {
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.INVALID_RESPONSE, java.util.List.of()));
            }

            @Override
            public CompletableFuture<String> completePlainText(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ExecutionGateway executionGateway = request -> CompletableFuture.completedFuture(new JsonObject());
        SpeechGateway speechGateway = request -> CompletableFuture.completedFuture(null);
        return new VegaSubsystemGate(llmGateway, executionGateway, speechGateway);
    }

    private static long ownedThreadCount() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(thread -> OWNED_THREAD_NAMES.contains(thread.getName()))
                .count();
    }

    private static void awaitOwnedThreadCount(long expectedCount) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
        while (ownedThreadCount() != expectedCount && System.nanoTime() < deadlineNanos) {
            Thread.sleep(5);
        }
        assertTrue(ownedThreadCount() <= expectedCount,
                "vega-owned threads must return to their pre-lifecycle count");
    }
}
