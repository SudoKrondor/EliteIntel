package elite.intel.vega.input;

import com.google.gson.JsonObject;
import elite.intel.vega.CompanionRuntime;
import elite.intel.vega.CompanionRuntimeGraph;
import elite.intel.vega.CompanionRuntimeTestSupport;
import elite.intel.vega.execution.ExecutionGateway;
import elite.intel.vega.llm.LlmGateway;
import elite.intel.vega.mind.CompanionState;
import elite.intel.vega.model.llm.LlmRequest;
import elite.intel.vega.model.llm.LlmResult;
import elite.intel.vega.speech.SpeechGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionSubsystemGateTest {

    private static final Set<String> OWNED_THREAD_NAMES = Set.of(
            "companion-commander",
            "companion-event",
            "companion-watchdog",
            "companion-llm",
            "companion-action",
            "companion-query",
            "companion-consolidator",
            "companion-memory-compressor");

    private CompanionSubsystemGate gate;
    private CompanionRuntimeGraph separatelyInstalledGraph;

    @AfterEach
    void cleanUpRuntime() {
        if (gate != null) {
            gate.stop();
        }
        CompanionRuntimeTestSupport.uninstall(separatelyInstalledGraph);
        CompanionRuntimeTestSupport.clearInstalledGraph();
    }

    @Test
    void repeatedRestartCyclesLeaveNoRuntimeOrOwnedThreads() throws Exception {
        long threadCountBeforeCycles = ownedThreadCount();
        gate = gateWithResourceFreeOverrides();

        for (int cycle = 0; cycle < 100; cycle++) {
            gate.start();
            assertNotNull(gate.dispatcher());
            assertNotNull(CompanionRuntime.state());

            gate.stop();
            assertNull(gate.dispatcher());
            assertThrows(IllegalStateException.class, CompanionRuntime::state);
        }

        awaitOwnedThreadCount(threadCountBeforeCycles);
        assertEquals(threadCountBeforeCycles, ownedThreadCount());
    }

    @Test
    void failedPublicationRollsBackTheNewGraphWithoutClearingTheInstalledGeneration() throws Exception {
        CompanionState installedState = new CompanionState();
        separatelyInstalledGraph = CompanionRuntimeTestSupport.install(
                null, null, null, null, null, installedState);
        long threadCountBeforeAttempt = ownedThreadCount();
        gate = gateWithResourceFreeOverrides();

        assertThrows(IllegalStateException.class, gate::start);

        assertNull(gate.dispatcher());
        assertSame(installedState, CompanionRuntime.state());
        awaitOwnedThreadCount(threadCountBeforeAttempt);
        assertEquals(threadCountBeforeAttempt, ownedThreadCount());
    }

    private static CompanionSubsystemGate gateWithResourceFreeOverrides() {
        LlmGateway llmGateway = new LlmGateway() {
            @Override
            public CompletableFuture<LlmResult> submit(LlmRequest request) {
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.INVALID_RESPONSE, java.util.List.of()));
            }

            @Override
            public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ExecutionGateway executionGateway = request -> CompletableFuture.completedFuture(new JsonObject());
        SpeechGateway speechGateway = request -> CompletableFuture.completedFuture(null);
        return new CompanionSubsystemGate(llmGateway, executionGateway, speechGateway);
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
                "companion-owned threads must return to their pre-lifecycle count");
    }
}
