package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.brain.actions.ActionParameterSpec;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.command.CommandHandler;
import elite.intel.ai.hands.KeyBindingExecutor;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandHandlerTest {

    private static final Gson GSON = new Gson();

    private final InputCapture inputCapture = new InputCapture();
    private final TestSpeakExecutor testSpeakExecutor = new TestSpeakExecutor();

    // Keys added to CommandHandlerFactory in individual tests - cleaned up in @AfterEach.
    private final List<String> addedHandlerKeys = new ArrayList<>();

    @BeforeEach
    void registerCaptures() {
        GameControllerBus.register(inputCapture);
    }

    @AfterEach
    void cleanUp() {
        GameControllerBus.unregister(inputCapture);
        inputCapture.events.clear();
        testSpeakExecutor.spoken.clear();
        for (String key : addedHandlerKeys) {
            CommandHandlerFactory.getInstance().getCommandHandlers().remove(key);
        }
        addedHandlerKeys.clear();
    }

    // --- BINDING_TAP ---

    @Test
    void bindingTapStepPublishesGameInputWithCorrectBindingId() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"TestBinding"}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        GameInputStep step = inputCapture.events.getFirst().getSteps().getFirst();
        assertEquals(GameInputStep.Type.BINDING_TAP, step.getType());
        assertEquals("TestBinding", step.getBindingId());
    }

    // --- BINDING_HOLD ---

    @Test
    void bindingHoldStepPreservesBindingIdAndDuration() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"BINDING_HOLD","bindingId":"HoldBinding","durationMs":300}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        GameInputStep step = inputCapture.events.getFirst().getSteps().getFirst();
        assertEquals(GameInputStep.Type.BINDING_HOLD, step.getType());
        assertEquals("HoldBinding", step.getBindingId());
        assertEquals(300, step.getDurationMs());
    }

    // --- DELAY ---

    @Test
    void delayStepProducesNoInputEvents() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"DELAY","durationMs":0}
                ]}""");

        assertTrue(inputCapture.events.isEmpty());
        assertTrue(testSpeakExecutor.spoken.isEmpty());
    }

    // --- SPEAK ---

    @Test
    void speakStepCallsSpeakExecutorWithCorrectText() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"SPEAK","text":"Hello pilot"}
                ]}""");

        assertEquals(1, testSpeakExecutor.spoken.size());
        assertEquals("Hello pilot", testSpeakExecutor.spoken.getFirst());
        assertTrue(inputCapture.events.isEmpty());
    }

    @Test
    void speakBlocksUntilExecutorCompletes() throws InterruptedException {
        CountDownLatch speakStarted = new CountDownLatch(1);
        CountDownLatch speakRelease = new CountDownLatch(1);

        CustomCommandSpeakExecutor blockingExecutor = text -> {
            speakStarted.countDown();
            speakRelease.await();
        };

        CustomCommandDefinition customCommand = deserialize("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"SPEAK","text":"Wait for me"},
                  {"type":"BINDING_TAP","bindingId":"AfterSpeak"}
                ]}""");
        CustomCommandHandler handler = new CustomCommandHandler(customCommand, blockingExecutor);

        Thread t = new Thread(() -> handler.handle("m", new JsonObject(), ""));
        t.start();

        assertTrue(speakStarted.await(2, TimeUnit.SECONDS), "SPEAK must start within 2s");
        assertTrue(inputCapture.events.isEmpty(), "BINDING_TAP must not fire while SPEAK is executing");

        speakRelease.countDown();
        t.join(2000);

        assertEquals(1, inputCapture.events.size(), "BINDING_TAP must fire after SPEAK completes");
        assertEquals("AfterSpeak", inputCapture.events.getFirst().getSteps().getFirst().getBindingId());
    }

    // --- RUN_COMMAND ---

    @Test
    void runCommandStepDelegatesToRegisteredBuiltinHandler() {
        AtomicBoolean called = new AtomicBoolean(false);
        CommandHandler fakeBuiltin = (a, p, r) -> { called.set(true); return null; };
        registerHandler("builtin_action", fakeBuiltin);

        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"RUN_COMMAND","actionId":"builtin_action"}
                ]}""");

        assertTrue(called.get(), "Builtin handler must be called");
    }

    @Test
    void runCommandStepSkipsWhenTargetHandlerIsCustomCommandHandler() {
        // Register a second customCommand handler as the RUN_COMMAND target.
        CustomCommandDefinition nested = deserialize("""
                {"id":"custom_command_nested","name":"Nested","phrases":"p",
                 "steps":[{"type":"SPEAK","text":"nested called"}]}""");
        CustomCommandHandler nestedHandler = new CustomCommandHandler(nested, testSpeakExecutor);
        registerHandler("custom_command_nested", nestedHandler);

        // CustomCommand that tries to call another custom command.
        runCustomCommand("""
                {"id":"custom_command_caller","name":"Caller","phrases":"p","steps":[
                  {"type":"RUN_COMMAND","actionId":"custom_command_nested"}
                ]}""");

        // Guard must prevent the nested customCommand from running - no speech from nested.
        assertTrue(testSpeakExecutor.spoken.isEmpty(), "Cross-customCommand call must be blocked");
    }

    @Test
    void runCommandStepSkipsUnknownActionId() {
        // No exception - just logs a warning.
        assertDoesNotThrow(() -> runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"RUN_COMMAND","actionId":"does_not_exist_12345"}
                ]}"""));
        assertTrue(inputCapture.events.isEmpty());
    }

    // --- RAW_KEY ---

    @Test
    void rawKeyStepPublishesGameInputWithCorrectKeyCode() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"RAW_KEY","rawKey":"KEY_F5","durationMs":0}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        GameInputStep step = inputCapture.events.getFirst().getSteps().getFirst();
        assertEquals(GameInputStep.Type.RAW_KEY, step.getType());
        assertEquals(KeyBindingExecutor.resolveKeyCode("KEY_F5"), step.getKeyCode());
        assertEquals(0, step.getModifierKeyCode());
        assertEquals(0, step.getDurationMs());
    }

    @Test
    void rawKeyStepWithModifierSetsModifierKeyCode() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"RAW_KEY","rawKey":"KEY_W","rawKeyModifier":"KEY_LEFTCONTROL","durationMs":0}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        GameInputStep step = inputCapture.events.getFirst().getSteps().getFirst();
        assertEquals(GameInputStep.Type.RAW_KEY, step.getType());
        assertEquals(KeyBindingExecutor.resolveKeyCode("KEY_W"), step.getKeyCode());
        assertEquals(KeyBindingExecutor.resolveKeyCode("KEY_LEFTCONTROL"), step.getModifierKeyCode());
        assertEquals(0, step.getDurationMs());
    }

    @Test
    void rawKeyStepWithHoldDurationPreservesDuration() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"RAW_KEY","rawKey":"KEY_SPACE","durationMs":500}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        GameInputStep step = inputCapture.events.getFirst().getSteps().getFirst();
        assertEquals(GameInputStep.Type.RAW_KEY, step.getType());
        assertEquals(KeyBindingExecutor.resolveKeyCode("KEY_SPACE"), step.getKeyCode());
        assertEquals(500, step.getDurationMs());
    }

    @Test
    void rawKeyStepWithUnknownKeyIsSkipped() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"RAW_KEY","rawKey":"KEY_DOES_NOT_EXIST_99999","durationMs":0}
                ]}""");

        assertTrue(inputCapture.events.isEmpty());
    }

    @Test
    void rawKeyStepWithUnknownModifierFallsBackToNoModifier() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"RAW_KEY","rawKey":"KEY_W","rawKeyModifier":"KEY_BAD_MODIFIER","durationMs":0}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        GameInputStep step = inputCapture.events.getFirst().getSteps().getFirst();
        assertEquals(GameInputStep.Type.RAW_KEY, step.getType());
        // Unknown modifier resolves to 0 (no modifier) rather than skipping the step
        assertEquals(0, step.getModifierKeyCode());
        assertEquals(KeyBindingExecutor.resolveKeyCode("KEY_W"), step.getKeyCode());
    }

    @Test
    void rawKeyStepCaseInsensitiveKeyName() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"RAW_KEY","rawKey":"Key_LeftControl","durationMs":0}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        GameInputStep step = inputCapture.events.getFirst().getSteps().getFirst();
        assertEquals(KeyBindingExecutor.resolveKeyCode("KEY_LEFTCONTROL"), step.getKeyCode());
    }

    // --- multi-step ordering ---

    @Test
    void multiStepCustomCommandProducesEventsInOrder() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"FirstBinding"},
                  {"type":"DELAY","durationMs":0},
                  {"type":"SPEAK","text":"sequence done"}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        List<GameInputStep> steps = inputCapture.events.getFirst().getSteps();
        assertEquals(2, steps.size());
        assertEquals("FirstBinding", steps.getFirst().getBindingId());
        assertEquals(GameInputStep.Type.DELAY, steps.get(1).getType());
        assertEquals(1, testSpeakExecutor.spoken.size());
        assertEquals("sequence done", testSpeakExecutor.spoken.getFirst());
    }

    @Test
    void speakDelayBindingSequenceStillPublishesFinalBindingTap() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"SPEAK","text":"Hello pilot"},
                  {"type":"DELAY","durationMs":0},
                  {"type":"BINDING_TAP","bindingId":"GalaxyMapOpen"}
                ]}""");

        assertEquals(1, testSpeakExecutor.spoken.size());
        assertEquals("Hello pilot", testSpeakExecutor.spoken.getFirst());
        assertEquals(1, inputCapture.events.size());
        List<GameInputStep> steps = inputCapture.events.getFirst().getSteps();
        assertEquals(2, steps.size());
        assertEquals(GameInputStep.Type.DELAY, steps.getFirst().getType());
        GameInputStep step = steps.get(1);
        assertEquals(GameInputStep.Type.BINDING_TAP, step.getType());
        assertEquals("GalaxyMapOpen", step.getBindingId());
    }

    @Test
    void bindingDelayBindingPublishesSingleSequenceInOrder() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"FirstBinding"},
                  {"type":"DELAY","durationMs":250},
                  {"type":"BINDING_TAP","bindingId":"SecondBinding"}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        List<GameInputStep> steps = inputCapture.events.getFirst().getSteps();
        assertEquals(3, steps.size());
        assertEquals("FirstBinding", steps.get(0).getBindingId());
        assertEquals(GameInputStep.Type.DELAY, steps.get(1).getType());
        assertEquals(250, steps.get(1).getDurationMs());
        assertEquals("SecondBinding", steps.get(2).getBindingId());
    }

    // --- customCommand atomicity: two customCommands must not interleave ---

    @Test
    void twoCustomCommandsDoNotInterleaveInputSteps() throws InterruptedException {
        CustomCommandDefinition customCommand1 = deserialize("""
                {"id":"m1","name":"M1","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"CustomCommand1Step1"},
                  {"type":"DELAY","durationMs":0},
                  {"type":"BINDING_TAP","bindingId":"CustomCommand1Step2"}
                ]}""");
        CustomCommandDefinition customCommand2 = deserialize("""
                {"id":"m2","name":"M2","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"CustomCommand2Step1"},
                  {"type":"DELAY","durationMs":0},
                  {"type":"BINDING_TAP","bindingId":"CustomCommand2Step2"}
                ]}""");

        CustomCommandHandler h1 = new CustomCommandHandler(customCommand1, testSpeakExecutor);
        CustomCommandHandler h2 = new CustomCommandHandler(customCommand2, testSpeakExecutor);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            ready.countDown();
            try { go.await(); } catch (InterruptedException e) { return; }
            h1.handle("m1", new JsonObject(), "");
        });
        Thread t2 = new Thread(() -> {
            ready.countDown();
            try { go.await(); } catch (InterruptedException e) { return; }
            h2.handle("m2", new JsonObject(), "");
        });

        t1.start();
        t2.start();
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        go.countDown();
        t1.join(5000);
        t2.join(5000);

        assertEquals(2, inputCapture.events.size(), "Each customCommand must produce exactly one GameInputSequenceEvent");

        // No interleaving: each event's bindings must all belong to the same customCommand
        List<String> first = inputCapture.events.get(0).getSteps().stream()
                .filter(s -> s.getType() != GameInputStep.Type.DELAY)
                .map(GameInputStep::getBindingId)
                .toList();
        List<String> second = inputCapture.events.get(1).getSteps().stream()
                .filter(s -> s.getType() != GameInputStep.Type.DELAY)
                .map(GameInputStep::getBindingId)
                .toList();

        boolean firstIsCustomCommand1 = first.stream().allMatch(b -> b.startsWith("CustomCommand1"));
        boolean firstIsCustomCommand2 = first.stream().allMatch(b -> b.startsWith("CustomCommand2"));
        assertTrue(firstIsCustomCommand1 || firstIsCustomCommand2, "First event must belong entirely to one customCommand");

        if (firstIsCustomCommand1) {
            assertTrue(second.stream().allMatch(b -> b.startsWith("CustomCommand2")), "Second event must be CustomCommand2's steps");
        } else {
            assertTrue(second.stream().allMatch(b -> b.startsWith("CustomCommand1")), "Second event must be CustomCommand1's steps");
        }
    }

    // --- parameterized customCommands ---

    @Test
    void runCommandStepPassesResolvedStepParamsToNestedHandler() {
        AtomicReference<JsonObject> capturedParams = new AtomicReference<>();
        CommandHandler fakeBuiltin = (a, p, r) -> { capturedParams.set(p); return null; };
        registerHandler("builtin_with_params", fakeBuiltin);

        CustomCommandDefinition customCommand = new CustomCommandDefinition(
                "m", "M", "", "phrase",
                List.of(new ActionParameterSpec("commodity", "string", true, "", null, null)),
                List.of(CustomCommandStep.runCommandWithParams("builtin_with_params", Map.of("key", "${commodity}")))
        );
        CustomCommandHandler handler = new CustomCommandHandler(customCommand, testSpeakExecutor);
        JsonObject params = JsonParser.parseString("{\"commodity\": \"gold\"}").getAsJsonObject();
        handler.handle("m", params, "");

        assertNotNull(capturedParams.get(), "Nested handler must be called with resolved params");
        assertEquals("gold", capturedParams.get().get("key").getAsString());
    }

    @Test
    void runCommandStepPreservesJsonNumberType() {
        AtomicReference<JsonObject> capturedParams = new AtomicReference<>();
        CommandHandler fakeBuiltin = (a, p, r) -> { capturedParams.set(p); return null; };
        registerHandler("navigate_fake", fakeBuiltin);

        CustomCommandDefinition customCommand = new CustomCommandDefinition(
                "m", "M", "", "phrase",
                List.of(
                        new ActionParameterSpec("lat", "number", true, "", null, null),
                        new ActionParameterSpec("lon", "number", true, "", null, null)
                ),
                List.of(CustomCommandStep.runCommandWithParams("navigate_fake", Map.of("lat", "${lat}", "lon", "${lon}")))
        );
        CustomCommandHandler handler = new CustomCommandHandler(customCommand, testSpeakExecutor);
        JsonObject params = JsonParser.parseString("{\"lat\": -10.5, \"lon\": 45.2}").getAsJsonObject();
        handler.handle("m", params, "");

        assertNotNull(capturedParams.get());
        assertEquals(-10.5, capturedParams.get().get("lat").getAsDouble(), 0.001);
        assertEquals(45.2, capturedParams.get().get("lon").getAsDouble(), 0.001);
    }

    @Test
    void abortsCustomCommandWhenRequiredParamIsMissing() {
        AtomicBoolean called = new AtomicBoolean(false);
        CommandHandler fakeBuiltin = (a, p, r) -> { called.set(true); return null; };
        registerHandler("cmd_fake", fakeBuiltin);

        CustomCommandDefinition customCommand = new CustomCommandDefinition(
                "m", "M", "", "phrase",
                List.of(new ActionParameterSpec("speed", "string", true, "", null, null)),
                List.of(CustomCommandStep.runCommandWithParams("cmd_fake", Map.of("key", "${speed}")))
        );
        CustomCommandHandler handler = new CustomCommandHandler(customCommand, testSpeakExecutor);
        // No params provided — required "speed" is missing.
        handler.handle("m", new JsonObject(), "");

        assertFalse(called.get(), "CustomCommand must be aborted when required param is missing");
    }

    @Test
    void speakStepResolvesParamTemplate() {
        CustomCommandDefinition customCommand = new CustomCommandDefinition(
                "m", "M", "", "phrase",
                List.of(new ActionParameterSpec("target", "string", true, "", null, null)),
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "Targeting ${target}", null))
        );
        CustomCommandHandler handler = new CustomCommandHandler(customCommand, testSpeakExecutor);
        JsonObject params = JsonParser.parseString("{\"target\": \"drive\"}").getAsJsonObject();
        handler.handle("m", params, "");

        assertEquals(1, testSpeakExecutor.spoken.size());
        assertEquals("Targeting drive", testSpeakExecutor.spoken.getFirst());
    }

    @Test
    void optionalParamAbsentDoesNotAbortCustomCommand() {
        AtomicBoolean called = new AtomicBoolean(false);
        CommandHandler fakeBuiltin = (a, p, r) -> { called.set(true); return null; };
        registerHandler("cmd_optional", fakeBuiltin);

        CustomCommandDefinition customCommand = new CustomCommandDefinition(
                "m", "M", "", "phrase",
                List.of(new ActionParameterSpec("hint", "string", false, "", null, null)),
                // step doesn't use the optional param at all
                List.of(new CustomCommandStep(CustomCommandStep.Type.RUN_COMMAND, null, 0, null, "cmd_optional"))
        );
        CustomCommandHandler handler = new CustomCommandHandler(customCommand, testSpeakExecutor);
        handler.handle("m", new JsonObject(), "");

        assertTrue(called.get(), "CustomCommand with absent optional param must still execute");
    }

    @Test
    void customCommandLockReleasedAfterInterrupt() throws InterruptedException {
        CustomCommandDefinition slowCustomCommand = deserialize("""
                {"id":"slow","name":"Slow","phrases":"p","steps":[
                  {"type":"DELAY","durationMs":5000}
                ]}""");
        CustomCommandDefinition fastCustomCommand = deserialize("""
                {"id":"fast","name":"Fast","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"FastBinding"}
                ]}""");

        CustomCommandHandler slowHandler = new CustomCommandHandler(slowCustomCommand, testSpeakExecutor);
        CustomCommandHandler fastHandler = new CustomCommandHandler(fastCustomCommand, testSpeakExecutor);

        Thread slowThread = new Thread(() -> slowHandler.handle("slow", new JsonObject(), ""));
        slowThread.start();
        Thread.sleep(50); // let slow customCommand acquire lock and enter DELAY
        slowThread.interrupt();
        slowThread.join(2000);

        // The lock must be released - the fast customCommand must now complete
        Thread fastThread = new Thread(() -> fastHandler.handle("fast", new JsonObject(), ""));
        fastThread.start();
        fastThread.join(2000);

        assertFalse(fastThread.isAlive(), "Fast customCommand must complete after slow customCommand is interrupted");
        assertEquals(1, inputCapture.events.size());
        assertEquals("FastBinding", inputCapture.events.getFirst().getSteps().getFirst().getBindingId());
    }

    // --- interrupted thread ---

    @Test
    void interruptedThreadStopsAfterCurrentStep() throws InterruptedException {
        CustomCommandDefinition customCommand = deserialize("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"DELAY","durationMs":5000},
                  {"type":"SPEAK","text":"should not reach here"}
                ]}""");
        CustomCommandHandler handler = new CustomCommandHandler(customCommand, testSpeakExecutor);

        Thread t = new Thread(() -> handler.handle("m", new JsonObject(), ""));
        t.start();
        // Give the handler time to enter the sleep, then interrupt.
        Thread.sleep(50);
        t.interrupt();
        t.join(2000);

        assertFalse(t.isAlive(), "Handler thread must exit after interrupt");
        assertTrue(testSpeakExecutor.spoken.isEmpty(), "SPEAK after interrupted DELAY must not fire");
    }

    // --- helpers ---

    private void runCustomCommand(String json) {
        CustomCommandHandler handler = new CustomCommandHandler(deserialize(json), testSpeakExecutor);
        handler.handle("test_action", new JsonObject(), "");
    }

    private CustomCommandDefinition deserialize(String json) {
        return GSON.fromJson(json, CustomCommandDefinition.class);
    }

    private void registerHandler(String key, CommandHandler handler) {
        CommandHandlerFactory.getInstance().getCommandHandlers().put(key, handler);
        addedHandlerKeys.add(key);
    }

    // --- test doubles ---

    /** Captures spoken texts without blocking or publishing events. */
    static class TestSpeakExecutor implements CustomCommandSpeakExecutor {
        final List<String> spoken = new ArrayList<>();

        @Override
        public void speak(String text) {
            spoken.add(text);
        }
    }

    private static class InputCapture {
        final List<GameInputSequenceEvent> events = new ArrayList<>();

        @Subscribe
        public void on(GameInputSequenceEvent e) {
            events.add(e);
        }
    }
}
