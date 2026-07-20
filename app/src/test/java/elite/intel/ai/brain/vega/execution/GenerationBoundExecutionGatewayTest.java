package elite.intel.ai.brain.vega.execution;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.CompanionRuntimeGeneration;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.execution.GenerationBoundExecutionGateway;
import elite.intel.ai.brain.vega.model.execution.ExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationBoundExecutionGatewayTest {

    @Test
    void closeCancelsOwnedResultClosesDelegateAndRejectsNewWork() {
        RecordingExecutionGateway delegate = new RecordingExecutionGateway();
        GenerationBoundExecutionGateway gateway = new GenerationBoundExecutionGateway(
                delegate, new CompanionRuntimeGeneration());

        CompletableFuture<JsonObject> pendingResult = gateway.submit(
                new ExecutionRequest("request-1", "ship_status", new JsonObject()));
        gateway.close();

        assertTrue(pendingResult.isCancelled());
        assertEquals(1, delegate.closeCalls);
        ExecutionException rejected = assertThrows(ExecutionException.class, () -> gateway.submit(
                new ExecutionRequest("request-2", "ship_status", new JsonObject())).get());
        assertInstanceOf(RejectedExecutionException.class, rejected.getCause());
    }

    @Test
    void closeIsIdempotent() {
        RecordingExecutionGateway delegate = new RecordingExecutionGateway();
        GenerationBoundExecutionGateway gateway = new GenerationBoundExecutionGateway(
                delegate, new CompanionRuntimeGeneration());

        gateway.close();
        gateway.close();

        assertEquals(1, delegate.closeCalls);
    }

    private static final class RecordingExecutionGateway implements ExecutionGateway {
        private final CompletableFuture<JsonObject> pendingResult = new CompletableFuture<>();
        private int closeCalls;

        @Override
        public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
            return pendingResult;
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
