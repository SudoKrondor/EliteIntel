package elite.intel.companion.execution;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.tools.SystemFunction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies tool-call routing and result shaping with fake handler maps and a synchronous executor:
 * queries return their data payload on the query lane, commands return a dispatch-status object on the
 * action lane, unknown tools fail the future, and a future cancelled before its task starts skips
 * execution. Handlers are invoked directly, so no legacy auto-speech is involved.
 */
class CompanionExecutionGatewayTest {

    private static final Executor SYNC = Runnable::run;

    /** Records the last handle() invocation so the test can assert routing and arguments. */
    private static final class RecordingCommand implements IntelAction {
        volatile boolean invoked;
        volatile JsonObject seenArgs;
        @Override public String id() { return "nav"; }
        @Override public JsonObject handle(String action, JsonObject params, String text) {
            invoked = true;
            seenArgs = params;
            return null; // commands are side-effect only
        }
    }

    private static IntelQuery query(String id, JsonObject payload) {
        return new IntelQuery() {
            @Override public String id() { return id; }
            @Override public JsonObject handle(String action, JsonObject params, String text) { return payload; }
        };
    }

    private static SystemFunction systemFunction(String id, JsonObject payload) {
        return new SystemFunction() {
            @Override public String id() { return id; }
            @Override public java.util.Set<ThoughtSource> sources() { return EnumSet.of(ThoughtSource.COMMANDER); }
            @Override public JsonObject handle(String action, JsonObject params, String text) { return payload; }
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
}
