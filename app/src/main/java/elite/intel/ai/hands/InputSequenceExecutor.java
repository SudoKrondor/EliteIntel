package elite.intel.ai.hands;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Serializes all game input sequences through one worker so command handlers cannot interleave input steps.
 * Input-producing steps receive a small default post-input delay; explicit DELAY and WAIT_UNTIL steps receive only
 * the time they ask for.
 * Nested publishes from the worker are executed inline to avoid self-deadlock on the single-worker queue.
 */
public class InputSequenceExecutor {

    private static final Logger log = LogManager.getLogger(InputSequenceExecutor.class);
    /**
     * Width of the randomised window above the configured delay floor. The floor is the commander's
     * pacing setting ({@link SystemSession#getKeyInputDelayMs()}); the spread on top of it keeps the
     * keystrokes from landing on a fixed rhythm, as they always have.
     * <p>
     * It is a fixed width rather than a fraction of the floor: the jitter exists to break up the
     * rhythm, and a commander who drags the slider to the slow end is asking for a longer pause, not
     * a less predictable one. Scaling it would make the pacing they picked the least accurate at
     * exactly the end where they picked it to fix dropped keystrokes.
     */
    private static final int POST_INPUT_DELAY_SPREAD_MS = 50;
    private static final int WAIT_UNTIL_POLL_MS = 50;

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
        // Read the pacing once per sequence: it is a settings read, and the steps of one sequence
        // belong together anyway - a slider moved mid-sequence takes effect on the next command.
        int delayFloorMs = configuredDelayFloorMs();
        for (GameInputStep step : event.getSteps()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            boolean executed = executeStep(step);
            if (step.isInputProducing() && executed) {
                sleep(postInputDelayMs(random, delayFloorMs));
            }
        }
    }

    private boolean executeStep(GameInputStep step) {
        return switch (step.getType()) {
            case BINDING_TAP -> executeBindingPress(step.getBindingId());
            case BINDING_FORCED_TAP -> executeForcedTap(step.getBindingId());
            case BINDING_HOLD -> executeBindingHold(step.getBindingId(), step.getDurationMs());
            case BINDING_DOWN -> executeBindingDown(step.getBindingId());
            case BINDING_UP -> executeBindingUp(step.getBindingId());
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
            case WAIT_UNTIL -> {
                if (!awaitCondition(step.getCondition(), step.getDurationMs(), WAIT_UNTIL_POLL_MS)) {
                    // WHY: a timeout is a deliberate degradation, not a failure. The steps behind this one used
                    // to run after a blind delay that could expire just as early, so continuing keeps the old
                    // worst case while the log says the game never got where the sequence expected it.
                    log.warn("Timed out after {}ms waiting for: {}", step.getDurationMs(), step.getConditionDescription());
                }
                yield false;
            }
        };
    }

    private boolean executeBindingPress(String bindingId) {
        KeyBindingsParser.KeyBinding binding = resolveBinding(bindingId);
        if (binding == null) {
            return false;
        }
        log.debug("Press binding: key={}, hold={}", binding.key, binding.hold);
        bindingExecutor.executeBinding(binding);
        return true;
    }

    private boolean executeForcedTap(String bindingId) {
        KeyBindingsParser.KeyBinding binding = resolveBinding(bindingId);
        if (binding == null) {
            return false;
        }
        log.debug("Forced tap binding: key={}, ignoring hold flag={}", binding.key, binding.hold);
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

    private boolean executeBindingDown(String bindingId) {
        KeyBindingsParser.KeyBinding binding = resolveBinding(bindingId);
        if (binding == null) {
            return false;
        }
        bindingExecutor.holdBindingDown(binding);
        return true;
    }

    private boolean executeBindingUp(String bindingId) {
        KeyBindingsParser.KeyBinding binding = resolveBinding(bindingId);
        if (binding == null) {
            return false;
        }
        bindingExecutor.releaseBinding(binding);
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

    /**
     * Polls {@code condition} until it holds or {@code timeoutMs} elapses.
     *
     * @return true if the condition held before the deadline, false on timeout or interruption
     */
    static boolean awaitCondition(BooleanSupplier condition, int timeoutMs, int pollMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(Math.min(pollMs, Math.max(1, deadline - System.currentTimeMillis())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /**
     * The commander's pacing setting, or the shipped floor if it cannot be read. WHY the fallback:
     * a settings read that fails must not abort a sequence half-executed, leaving the game in a
     * panel the commander did not ask for.
     */
    private int configuredDelayFloorMs() {
        try {
            return SystemSession.getInstance().getKeyInputDelayMs();
        } catch (RuntimeException e) {
            log.warn("Could not read the key input pacing setting, using {}ms: {}",
                    SystemSession.KEY_INPUT_DELAY_MIN_MS, e.getMessage());
            return SystemSession.KEY_INPUT_DELAY_MIN_MS;
        }
    }

    /**
     * The pause after one keystroke: the configured floor plus a draw from the randomised spread.
     */
    static int postInputDelayMs(Random random, int delayFloorMs) {
        return delayFloorMs + random.nextInt(POST_INPUT_DELAY_SPREAD_MS);
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
