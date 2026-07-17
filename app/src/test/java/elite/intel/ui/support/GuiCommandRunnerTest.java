package elite.intel.ui.support;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiCommandRunnerTest {

    private static final String SENTINEL_ACTION = "gui_command_runner_test_sentinel";
    private static final String SUCCESS_ACTION = "gui_command_runner_test_success";
    private static final String FAILING_ACTION = "gui_command_runner_test_failure";
    private static final String MISSING_ACTION = "gui_command_runner_test_missing";
    private static final String RESPONSE_ACTION = "gui_command_runner_test_response";
    private static final String ACTIVATION_ACTION = "gui_command_runner_test_activation";

    private final Map<String, IntelAction> handlers = CommandHandlerFactory.getInstance().getCommandHandlers();
    private final Map<String, IntelAction> previousHandlers = new HashMap<>();

    @BeforeEach
    void keepFactoryFromLoadingApplicationCommands() {
        install(SENTINEL_ACTION, action(SENTINEL_ACTION, params -> {
        }));
    }

    @AfterEach
    void restoreHandlers() {
        previousHandlers.forEach((id, previous) -> {
            if (previous == null) {
                handlers.remove(id);
            } else {
                handlers.put(id, previous);
            }
        });
    }

    @Test
    void runInAppDispatchesImmediatelyAndCompletesOnceOnEdt() throws Exception {
        CountDownLatch completion = new CountDownLatch(1);
        AtomicBoolean handlerRanOnEdt = new AtomicBoolean();
        AtomicBoolean callbackRanOnEdt = new AtomicBoolean();
        AtomicInteger callbackCount = new AtomicInteger();
        AtomicReference<JsonObject> receivedParams = new AtomicReference<>();
        JsonObject params = new JsonObject();
        params.addProperty("system", "Sol");
        install(SUCCESS_ACTION, action(SUCCESS_ACTION, received -> {
            handlerRanOnEdt.set(SwingUtilities.isEventDispatchThread());
            receivedParams.set(received);
        }));

        GuiCommandRunner.runInApp(SUCCESS_ACTION, params, false, () -> {
            callbackRanOnEdt.set(SwingUtilities.isEventDispatchThread());
            callbackCount.incrementAndGet();
            completion.countDown();
        });

        assertTrue(completion.await(2, TimeUnit.SECONDS), "in-app dispatch must not use the three-second delay");
        SwingUtilities.invokeAndWait(() -> {
        });
        assertFalse(handlerRanOnEdt.get());
        assertSame(params, receivedParams.get());
        assertTrue(callbackRanOnEdt.get());
        assertEquals(1, callbackCount.get());
    }

    @Test
    void runInAppCompletesOnceOnEdtWhenHandlerFails() throws Exception {
        CountDownLatch completion = new CountDownLatch(1);
        AtomicBoolean callbackRanOnEdt = new AtomicBoolean();
        AtomicInteger callbackCount = new AtomicInteger();
        install(FAILING_ACTION, action(FAILING_ACTION, params -> {
            throw new IllegalStateException("expected test failure");
        }));

        GuiCommandRunner.runInApp(FAILING_ACTION, null, false, () -> {
            callbackRanOnEdt.set(SwingUtilities.isEventDispatchThread());
            callbackCount.incrementAndGet();
            completion.countDown();
        });

        assertTrue(completion.await(2, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
        });
        assertTrue(callbackRanOnEdt.get());
        assertEquals(1, callbackCount.get());
    }

    @Test
    void runInAppCompletesOnceOnEdtWhenActionIsMissing() throws Exception {
        CountDownLatch completion = new CountDownLatch(1);
        AtomicBoolean callbackRanOnEdt = new AtomicBoolean();
        AtomicInteger callbackCount = new AtomicInteger();

        GuiCommandRunner.runInApp(MISSING_ACTION, null, false, () -> {
            callbackRanOnEdt.set(SwingUtilities.isEventDispatchThread());
            callbackCount.incrementAndGet();
            completion.countDown();
        });

        assertTrue(completion.await(2, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
        });
        assertTrue(callbackRanOnEdt.get());
        assertEquals(1, callbackCount.get());
    }

    @Test
    void runInAppPublishesTheHandlersSpokenResponse() throws Exception {
        CountDownLatch completion = new CountDownLatch(1);
        ResponseListener listener = new ResponseListener();
        JsonObject response = new JsonObject();
        response.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, "Route calculated");
        install(RESPONSE_ACTION, resultAction(RESPONSE_ACTION, response));
        GameEventBus.register(listener);
        try {
            GuiCommandRunner.runInApp(RESPONSE_ACTION, null, false, completion::countDown);

            assertTrue(completion.await(2, TimeUnit.SECONDS));
            assertTrue(listener.responses.contains("Route calculated"));
        } finally {
            GameEventBus.unregister(listener);
        }
    }

    @Test
    void failedGameActivationDoesNotScheduleTheCommand() {
        AtomicInteger handlerCalls = new AtomicInteger();
        install(ACTIVATION_ACTION, action(ACTIVATION_ACTION, params -> handlerCalls.incrementAndGet()));

        boolean scheduled = GuiCommandRunner.runAfterActivatingGame(
                ACTIVATION_ACTION, null, false, () -> false);

        assertFalse(scheduled);
        assertEquals(0, handlerCalls.get());
    }

    private void install(String id, IntelAction handler) {
        if (!previousHandlers.containsKey(id)) {
            previousHandlers.put(id, handlers.get(id));
        }
        handlers.put(id, handler);
    }

    private static IntelAction action(String id, ActionBody body) {
        return new IntelAction() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public JsonObject handle(String action, JsonObject params, String text) throws Exception {
                body.run(params);
                return null;
            }
        };
    }

    private static IntelAction resultAction(String id, JsonObject result) {
        return new IntelAction() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public JsonObject handle(String action, JsonObject params, String text) {
                return result;
            }
        };
    }

    private static final class ResponseListener {
        private final CopyOnWriteArrayList<String> responses = new CopyOnWriteArrayList<>();

        @Subscribe
        public void onResponse(AiVoxResponseEvent event) {
            responses.add(event.getText());
        }
    }

    @FunctionalInterface
    private interface ActionBody {
        void run(JsonObject params) throws Exception;
    }
}
