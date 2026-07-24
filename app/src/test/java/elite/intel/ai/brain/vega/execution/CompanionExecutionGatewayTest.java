package elite.intel.ai.brain.vega.execution;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.handlers.queries.IntelQuery;
import elite.intel.ai.brain.vega.CompanionNarrator;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.brain.vega.CompanionRuntimeGraph;
import elite.intel.ai.brain.vega.CompanionRuntimeTestSupport;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.ai.brain.vega.model.execution.ExecutionRequest;
import elite.intel.ai.brain.vega.tools.SystemFunction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies tool-call routing and result shaping with fake handler maps and a synchronous executor:
 * queries return their data payload on the query lane, commands return a dispatch-status object on the
 * action lane, unknown tools fail the future, and a future cancelled before its task starts skips
 * execution. Handlers are invoked directly, so no legacy auto-speech is involved.
 */
class CompanionExecutionGatewayTest {

    private static final Executor SYNC = Runnable::run;

    /**
     * Records the last handle() invocation so the test can assert routing and arguments.
     */
    private static class RecordingCommand implements IntelAction {
        volatile boolean invoked;
        volatile JsonObject seenArgs;
        volatile String seenText;

        @Override
        public String id() {
            return "nav";
        }

        @Override
        public JsonObject handle(String action, JsonObject params, String text) {
            invoked = true;
            seenArgs = params;
            seenText = text;
            return null; // commands are side-effect only
        }
    }

    private static IntelQuery query(String id, JsonObject payload) {
        return new IntelQuery() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public JsonObject handle(String action, JsonObject params, String text) {
                return payload;
            }
        };
    }

    private static SystemFunction systemFunction(String id, JsonObject payload) {
        return new SystemFunction() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public java.util.Set<ThoughtSource> sources() {
                return EnumSet.of(ThoughtSource.COMMANDER);
            }

            @Override
            public JsonObject handle(String action, JsonObject params, String text) {
                return payload;
            }
        };
    }

    @Test
    void queryReturnsItsPayload() throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("fuel", 0.42);
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of(), Map.of("ship_status", query("ship_status", payload)), Map.of(), SYNC, SYNC);

        CompletableFuture<JsonObject> future = gateway.submit(new ExecutionRequest("r1", "ship_status", new JsonObject()));

        assertEquals(payload, future.get());
    }

    @Test
    void commandReturnsDispatchStatusAndRunsHandler() throws Exception {
        RecordingCommand command = new RecordingCommand();
        JsonObject args = new JsonObject();
        args.addProperty("speed", 50);
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of("nav", command), Map.of(), Map.of(), SYNC, SYNC);

        JsonObject result = gateway.submit(new ExecutionRequest("r1", "nav", args)).get();

        assertTrue(command.invoked);
        assertEquals(args, command.seenArgs);
        assertEquals("completed_by_executor", result.get("status").getAsString());
        assertEquals("nav", result.get("tool").getAsString());
    }

    @Test
    void systemFunctionRoutedThroughHandle() throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", "spoken");
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of(), Map.of(), Map.of("speak", systemFunction("speak", payload)), SYNC, SYNC);

        assertEquals(payload, gateway.submit(new ExecutionRequest("r1", "speak", new JsonObject())).get());
    }

    @Test
    void commanderUtteranceReachesHandleAsOriginalUserInput() throws Exception {
        // Regression: the gateway used to pass "" as originalUserInput, so handlers that match a spoken
        // body name (e.g. "is B 1 landable") never saw the words and fell back to a whole-system answer.
        RecordingCommand command = new RecordingCommand();
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of("nav", command), Map.of(), Map.of(), SYNC, SYNC);

        gateway.submit(new ExecutionRequest("r1", "nav", new JsonObject(), "is planet b one landable")).get();

        assertEquals("is planet b one landable", command.seenText);
    }

    @Test
    void actionMaySelectTheCanonicalModelVisibleInput() throws Exception {
        RecordingCommand command = new RecordingCommand() {
            @Override
            public String executionInput(String originalInput, String matchInput) {
                return matchInput;
            }
        };
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of("nav", command), Map.of(), Map.of(), SYNC, SYNC);

        gateway.submit(new ExecutionRequest(
                "r1", "nav", new JsonObject(),
                "remember my career", "remember my carrier", 0L)).get();

        assertEquals("remember my carrier", command.seenText);
    }

    @Test
    void unknownToolFailsTheFuture() {
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(Map.of(), Map.of(), Map.of(), SYNC, SYNC);

        CompletableFuture<JsonObject> future = gateway.submit(new ExecutionRequest("r1", "ghost", new JsonObject()));

        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void cancelBeforeStartSkipsExecution() {
        RecordingCommand command = new RecordingCommand();
        // Deferred lane: capture the task instead of running it, so we can cancel before it starts.
        AtomicReference<Runnable> pending = new AtomicReference<>();
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of("nav", command), Map.of(), Map.of(), pending::set, SYNC);

        CompletableFuture<JsonObject> future = gateway.submit(new ExecutionRequest("r1", "nav", new JsonObject()));
        future.cancel(true);
        pending.get().run(); // task starts after cancellation

        assertFalse(command.invoked, "cancelled-before-start work must be skipped");
        assertTrue(future.isCancelled());
    }

    @Test
    void closeCancelsQueuedWorkAndRejectsLaterSubmissions() {
        RecordingCommand command = new RecordingCommand();
        AtomicReference<Runnable> pending = new AtomicReference<>();
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of("nav", command), Map.of(), Map.of(), pending::set, SYNC);

        CompletableFuture<JsonObject> queued = gateway.submit(
                new ExecutionRequest("r1", "nav", new JsonObject()));
        gateway.close();
        pending.get().run();

        assertTrue(queued.isCancelled());
        assertFalse(command.invoked);
        ExecutionException rejected = assertThrows(ExecutionException.class, () -> gateway.submit(
                new ExecutionRequest("r2", "nav", new JsonObject())).get());
        assertInstanceOf(java.util.concurrent.RejectedExecutionException.class, rejected.getCause());
    }

    @Test
    void closeDoesNotInterruptAnAlreadyStartedAction() throws Exception {
        CountDownLatch handlerStarted = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);
        AtomicBoolean handlerInterrupted = new AtomicBoolean();
        IntelAction blockingCommand = new IntelAction() {
            @Override
            public String id() {
                return "nav";
            }

            @Override
            public JsonObject handle(String action, JsonObject params, String text) {
                handlerStarted.countDown();
                try {
                    allowCompletion.await();
                } catch (InterruptedException interrupted) {
                    handlerInterrupted.set(true);
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        };
        ExecutorService actionLane = Executors.newSingleThreadExecutor();
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of("nav", blockingCommand), Map.of(), Map.of(), actionLane, SYNC);

        CompletableFuture<JsonObject> result = gateway.submit(
                new ExecutionRequest("r1", "nav", new JsonObject()));
        assertTrue(handlerStarted.await(1, TimeUnit.SECONDS));
        CompletableFuture<Void> closing = CompletableFuture.runAsync(gateway::close);
        allowCompletion.countDown();

        assertEquals("completed_by_executor", result.get(1, TimeUnit.SECONDS).get("status").getAsString());
        closing.get(1, TimeUnit.SECONDS);
        assertFalse(handlerInterrupted.get(), "started game actions must not be force-interrupted");
    }

    @Test
    void aRunningCommandDoesNotBlockALaterCommand() throws Exception {
        // Regression: the action lane used to be single-threaded, so a multi-second route/trade calculation
        // stalled every later command. A running command must never block a subsequent one (land, deploy, boost).
        CountDownLatch slowStarted = new CountDownLatch(1);
        CountDownLatch releaseSlow = new CountDownLatch(1);
        IntelAction slowCommand = new IntelAction() {
            @Override
            public String id() {
                return "slow";
            }

            @Override
            public JsonObject handle(String action, JsonObject params, String text) {
                slowStarted.countDown();
                try {
                    releaseSlow.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        };
        RecordingCommand fastCommand = new RecordingCommand();
        ExecutorService actionLane = Executors.newVirtualThreadPerTaskExecutor();
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of("slow", slowCommand, "fast", fastCommand), Map.of(), Map.of(), actionLane, SYNC);

        try {
            CompletableFuture<JsonObject> slow = gateway.submit(new ExecutionRequest("r1", "slow", new JsonObject()));
            assertTrue(slowStarted.await(1, TimeUnit.SECONDS), "slow command should be running");

            // The fast command must complete while the slow one is still in flight.
            JsonObject fast = gateway.submit(new ExecutionRequest("r2", "fast", new JsonObject()))
                    .get(1, TimeUnit.SECONDS);
            assertTrue(fastCommand.invoked, "later command must run without waiting for the slow one");
            assertEquals("completed_by_executor", fast.get("status").getAsString());
            assertFalse(slow.isDone(), "slow command should still be blocked");

            releaseSlow.countDown();
            assertEquals("completed_by_executor", slow.get(1, TimeUnit.SECONDS).get("status").getAsString());
        } finally {
            releaseSlow.countDown();
            gateway.close();
        }
    }

    @Test
    void oldRequestGenerationCannotRouteStaticSpeechIntoTheCurrentRuntime() throws Exception {
        CompanionRuntimeGraph oldRuntime = CompanionRuntimeTestSupport.installNarrator(CompanionNarrator.NO_OP);
        long oldGenerationId = oldRuntime.runtimeGeneration().generationId();
        CompanionRuntimeTestSupport.uninstall(oldRuntime);
        AtomicInteger currentNarratorCalls = new AtomicInteger();
        CompanionNarrator currentNarrator = new CompanionNarrator() {
            @Override
            public void filler(String text, boolean urgent) {
                currentNarratorCalls.incrementAndGet();
            }

            @Override
            public void narrate(String data, String instructions) {
            }

            @Override
            public void announce(String phrase, boolean urgent) {
            }
        };
        CompanionRuntimeGraph currentRuntime = CompanionRuntimeTestSupport.installNarrator(currentNarrator);
        IntelAction oldHandler = new IntelAction() {
            @Override
            public String id() {
                return "old_handler";
            }

            @Override
            public JsonObject handle(String action, JsonObject params, String text) {
                CompanionRuntime.narrator().filler("late result", false);
                return null;
            }
        };
        CompanionExecutionGateway gateway = new CompanionExecutionGateway(
                Map.of("old_handler", oldHandler), Map.of(), Map.of(), SYNC, SYNC);

        try {
            gateway.submit(new ExecutionRequest(
                    "r1", "old_handler", new JsonObject(), "", oldGenerationId)).get();
            assertEquals(0, currentNarratorCalls.get());
        } finally {
            gateway.close();
            CompanionRuntimeTestSupport.uninstall(currentRuntime);
        }
    }
}
