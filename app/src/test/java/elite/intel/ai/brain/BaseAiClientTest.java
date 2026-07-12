package elite.intel.ai.brain;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseAiClientTest {

    private static final class TrackingFuture extends CompletableFuture<HttpResponse<String>> {
        private final AtomicBoolean cancelCalled = new AtomicBoolean();

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelCalled.set(true);
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static final class ControlledClient extends BaseAiClient {
        private final CountDownLatch exchangeStarted = new CountDownLatch(1);
        private final TrackingFuture exchange = new TrackingFuture();

        @Override
        protected CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest request) {
            exchangeStarted.countDown();
            return exchange;
        }
    }

    @Test
    void interruptCancelsThePhysicalHttpFuture() throws Exception {
        ControlledClient client = new ControlledClient();
        AtomicReference<JsonObject> response = new AtomicReference<>();
        Thread worker = new Thread(() -> response.set(client.sendJsonRequest(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost/unused"))
                .build())), "base-ai-client-test");
        worker.setDaemon(true);

        worker.start();
        assertTrue(client.exchangeStarted.await(2, TimeUnit.SECONDS));
        client.cancelCurrentRequest();
        worker.join(2_000);

        assertFalse(worker.isAlive(), "interrupt must release the synchronous provider wrapper");
        assertTrue(client.exchange.cancelCalled.get(), "interrupt must cancel the retained HTTP future");
        assertEquals("LLM Call Failed",
                response.get().get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE).getAsString());
    }
}
