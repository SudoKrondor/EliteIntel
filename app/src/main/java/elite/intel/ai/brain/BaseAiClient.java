package elite.intel.ai.brain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.SystemSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BaseAiClient {
    private static final Logger log = LogManager.getLogger(BaseAiClient.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private volatile Thread currentRequestThread = null;
    private final SystemSession systemSession = SystemSession.getInstance();

    public BaseAiClient() {
    }

    protected static double wallClockTps(long elapsedNs, int completionTokens) {
        return elapsedNs > 0 ? completionTokens * 1_000_000_000.0 / elapsedNs : 0.0;
    }

    public JsonObject createErrorResponse(String message) {
        JsonObject err = new JsonObject();
        err.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, message);
        return err;
    }

    public void cancelCurrentRequest() {
        Thread t = currentRequestThread;
        if (t != null) {
            t.interrupt();
        }
    }

    public JsonObject sendJsonRequest(HttpRequest request) {
        currentRequestThread = Thread.currentThread();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code != 200) {
                String body = response.body();
                log.error("HTTP {} – response: {}", code, body);
                if (code == 400 && !systemSession.useLocalCommandLlm()) {
                    GameEventBus.publish(new AiVoxResponseEvent("Bad Request. Unsupported request format or invalid API key"));
                } else if (code == 429) {
                    GameEventBus.publish(new AiVoxResponseEvent("Too Many Requests. Please try again later."));
                } else if (code == 401) {
                    GameEventBus.publish(new AiVoxResponseEvent("Invalid API Key. Please check your API Key and try again."));
                } else if (code == 500) {
                    GameEventBus.publish(new AiVoxResponseEvent("Internal Server Error. Please try again later."));
                }
                return createErrorResponse("HTTP " + code);
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return createErrorResponse("LLM Call Failed");
        } catch (IOException e) {
            return createErrorResponse("Request failed: " + e.getMessage());
        } finally {
            currentRequestThread = null;
        }
    }
}