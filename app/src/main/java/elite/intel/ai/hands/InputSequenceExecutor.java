package elite.intel.ai.hands;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Serializes all game input sequences through one worker so command handlers cannot interleave input steps.
 * Input-producing steps receive a small default post-input delay; explicit DELAY steps receive only their requested delay.
 * Nested publishes from the worker are executed inline to avoid self-deadlock on the single-worker queue.
 */
public class InputSequenceExecutor {

    private static final Logger log = LogManager.getLogger(InputSequenceExecutor.class);
    private static final int DEFAULT_POST_INPUT_DELAY_MIN_MS = 99;
    private static final int DEFAULT_POST_INPUT_DELAY_MAX_MS = 201;

    private final BindingsMonitor monitor = BindingsMonitor.getInstance();
    private final KeyBindingExecutor bindingExecutor = KeyBindingExecutor.getInstance();
    private final KeyProcessor keyProcessor = KeyProcessor.getInstance();
    private final Random random = new Random();
    private final AtomicReference<Thread> workerThread = new AtomicReference<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(new InputSequenceThreadFactory(workerThread));

    public InputSequenceExecutor() {
        GameControllerBus.register(this);
    }

    @Subscribe
    public void onGameInputSequence(GameInputSequenceEvent event) {
        // Guard against a nested publish from the sequence worker submitting to itself and blocking forever on Future.get().
        if (Thread.currentThread() == workerThread.get()) {
            executeSafely(event);
            return;
        }

        Future<?> future = worker.submit(() -> execute(event));
        try {
            future.get();
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error executing input sequence: {}", e.getMessage(), e);
        }
    }

    private void executeSafely(GameInputSequenceEvent event) {
        try {
            execute(event);
        } catch (Exception e) {
            log.error("Error executing input sequence: {}", e.getMessage(), e);
        }
    }

    public void shutdown() {
        GameControllerBus.unregister(this);
        worker.shutdownNow();
        try {
            if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("InputSequenceExecutor did not stop within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void execute(GameInputSequenceEvent event) {
        for (GameInputStep step : event.getSteps()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            boolean executed = executeStep(step);
            if (step.isInputProducing() && executed) {
                sleep(defaultPostInputDelayMs());
            }
        }
    }

    private boolean executeStep(GameInputStep step) {
        return switch (step.getType()) {
            case BINDING_TAP -> executeBindingTap(step.getBindingId());
            case BINDING_HOLD -> executeBindingHold(step.getBindingId(), step.getDurationMs());
            case RAW_KEY -> {
                int mod = step.getModifierKeyCode();
                if (mod != 0) {
                    keyProcessor.holdKey(mod);
                }
                try {
                    if (step.getDurationMs() > 0) {
                        keyProcessor.pressAndHoldKey(step.getKeyCode(), step.getDurationMs());
                    } else {
                        keyProcessor.pressKey(step.getKeyCode());
                    }
                } finally {
                    if (mod != 0) {
                        keyProcessor.releaseKey(mod);
                    }
                }
                yield true;
            }
            case TEXT -> {
                keyProcessor.enterText(step.getText());
                yield true;
            }
            case DELAY -> {
                sleep(step.getDurationMs());
                yield false;
            }
        };
    }

    private boolean executeBindingTap(String bindingId) {
        KeyBindingsParser.KeyBinding binding = resolveBinding(bindingId);
        if (binding == null) {
            return false;
        }
        log.debug("Tap binding: key={}, ignoring hold flag={}", binding.key, binding.hold);
        bindingExecutor.executeTap(binding);
        return true;
    }

    private boolean executeBindingHold(String bindingId, int holdMs) {
        KeyBindingsParser.KeyBinding binding = resolveBinding(bindingId);
        if (binding == null) {
            return false;
        }
        bindingExecutor.executeBindingWithHold(binding, holdMs);
        return true;
    }

    private KeyBindingsParser.KeyBinding resolveBinding(String bindingId) {
        Map<String, KeyBindingsParser.KeyBinding> bindings = monitor.getBindings();
        if (bindings == null) {
            return null;
        }
        KeyBindingsParser.KeyBinding binding = bindings.get(bindingId);
        if (binding == null) {
            handleNoKeyBindingFound(bindingId);
        }
        return binding;
    }

    private void handleNoKeyBindingFound(String bindingId) {
        log.warn("No binding found for action: {}", bindingId);
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedSpeech("speech.keyBindingNotFound", bindingId)));
    }

    private int defaultPostInputDelayMs() {
        return DEFAULT_POST_INPUT_DELAY_MIN_MS
                + random.nextInt(DEFAULT_POST_INPUT_DELAY_MAX_MS - DEFAULT_POST_INPUT_DELAY_MIN_MS + 1);
    }

    private void sleep(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class InputSequenceThreadFactory implements ThreadFactory {
        private final AtomicReference<Thread> workerThread;

        private InputSequenceThreadFactory(AtomicReference<Thread> workerThread) {
            this.workerThread = workerThread;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "InputSequenceExecutorThread");
            workerThread.set(thread);
            return thread;
        }
    }
}
