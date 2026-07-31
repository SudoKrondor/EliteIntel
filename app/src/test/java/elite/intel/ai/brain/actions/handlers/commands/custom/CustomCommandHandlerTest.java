package elite.intel.ai.brain.actions.handlers.commands.custom;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import elite.intel.ai.hands.KeyBindingExecutor;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandHandlerTest {

    private static final Gson GSON = new Gson();

    private final InputCapture inputCapture = new InputCapture();
    private final TestSpeakExecutor testSpeakExecutor = new TestSpeakExecutor();

    @BeforeEach
    void registerCaptures() {
        GameControllerBus.register(inputCapture);
    }

    @AfterEach
    void cleanUp() {
        GameControllerBus.unregister(inputCapture);
        inputCapture.events.clear();
        testSpeakExecutor.spoken.clear();
    }

    // --- BINDING_TAP ---

    /**
     * A Binding Tap step must reach the executor as a <em>forced</em> tap. The commander picked it over the
     * neighbouring Binding Hold step, so it has to tap even when their .binds marks that binding as a long
     * press - a plain {@code BINDING_TAP} would defer to the file and hold instead.
     */
    @Test
    void bindingTapStepPublishesGameInputWithCorrectBindingId() {
        runCustomCommand("""
                {"id":"m","name":"M","phrases":"p","steps":[
                  {"type":"BINDING_TAP","bindingId":"TestBinding"}
                ]}""");

        assertEquals(1, inputCapture.events.size());
        GameInputStep step = inputCapture.events.getFirst().getSteps().getFirst();
        assertEquals(GameInputStep.Type.BINDING_FORCED_TAP, step.getType());
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
        assertEquals(GameInputStep.Type.BINDING_FORCED_TAP, step.getType());
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
            try {
                go.await();
            } catch (InterruptedException e) {
                return;
            }
            h1.handle("m1", new JsonObject(), "");
        });
        Thread t2 = new Thread(() -> {
            ready.countDown();
            try {
                go.await();
            } catch (InterruptedException e) {
                return;
            }
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

    // --- test doubles ---

    /**
     * Captures spoken texts without blocking or publishing events.
     */
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
