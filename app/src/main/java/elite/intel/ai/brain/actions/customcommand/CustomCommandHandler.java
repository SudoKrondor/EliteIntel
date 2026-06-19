package elite.intel.ai.brain.actions.customcommand;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.command.CommandHandler;
import elite.intel.ai.hands.KeyBindingExecutor;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.ui.event.AppLogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A handler for executing custom commands defined by {@code CustomCommandDefinition}.
 * This class is responsible for processing a series of custom command steps,
 * managing parameter resolution, handling errors, and synchronizing the execution flow
 * using a reentrant lock to ensure thread safety.
 *
 * Responsibilities:
 * - Ensures First-In-First-Out (FIFO) execution of custom commands.
 * - Validates and resolves required parameters for each custom command execution.
 * - Supports execution of multiple step types, including key bindings, delays, speech, and nested command calls.
 * - Handles interruptions and errors gracefully during execution, logging abort or error details as necessary.
 * - Blocks cross-delegation of nested {@code CustomCommandHandler} instances to prevent recursion.
 *
 * Thread Safety and Locking:
 * - Execution is synchronized using a global {@code ReentrantLock} with fair ordering.
 * - Locking ensures that no two threads can execute custom commands simultaneously.
 *
 * Dependencies:
 * - Uses {@code CustomCommandDefinition} for defining the custom command's structure.
 * - Relies on {@code CustomCommandSpeakExecutor} for speech execution steps.
 * - Integrates with {@code EventBusManager} to log and publish events during execution.
 */
public final class CustomCommandHandler implements CommandHandler {

    private static final Logger log = LogManager.getLogger(CustomCommandHandler.class);

    /** Serializes all custom command executions. Fair ordering ensures FIFO execution when customCommands queue up. */
    private static final ReentrantLock CUSTOM_COMMAND_LOCK = new ReentrantLock(true);

    private final CustomCommandDefinition customCommand;
    private final CustomCommandSpeakExecutor speakExecutor;

    public CustomCommandHandler(CustomCommandDefinition customCommand) {
        this(customCommand, SynchronousCustomCommandSpeech.DEFAULT);
    }

    /** Package-private: allows tests to inject a fast non-blocking speak executor. */
    CustomCommandHandler(CustomCommandDefinition customCommand, CustomCommandSpeakExecutor speakExecutor) {
        this.customCommand = customCommand;
        this.speakExecutor = speakExecutor;
    }

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        CUSTOM_COMMAND_LOCK.lock();
        try {
            CustomCommandExecutionContext ctx = CustomCommandExecutionContext.fromJson(customCommand, params);
            List<String> paramErrors = ctx.validateRequiredParams();
            if (!paramErrors.isEmpty()) {
                String errorSummary = String.join(", ", paramErrors);
                log.warn("Custom command '{}' aborted: {}", customCommand.getName(), errorSummary);
                EventBusManager.publish(new AppLogEvent("Custom command '" + customCommand.getName() + "' aborted: " + errorSummary));
                return;
            }
            log.info("Executing custom command '{}' ({} step(s))", customCommand.getName(), customCommand.getSteps().size());
            PendingInputSequence pendingInput = new PendingInputSequence();
            for (int i = 0; i < customCommand.getSteps().size(); i++) {
                CustomCommandStep step = customCommand.getSteps().get(i);
                try {
                    executeStep(step, i, pendingInput, ctx);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Custom command '{}' interrupted at step {}", customCommand.getName(), i);
                    return;
                } catch (UnresolvedCustomCommandParamException e) {
                    log.error("Custom command '{}' step {} ({}): {}  step skipped",
                            customCommand.getName(), i, step.getType(), e.getMessage());
                    EventBusManager.publish(new AppLogEvent(
                            "Custom command step error: " + e.getMessage() + " (step skipped)"));
                } catch (Exception e) {
                    log.error("Custom command '{}' step {} ({}) failed: {}", customCommand.getName(), i, step.getType(), e.getMessage(), e);
                    // continue to next step rather than aborting the whole customCommand
                }
            }
            try {
                flushPendingInputSteps(pendingInput);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Custom command '{}' interrupted while flushing input sequence", customCommand.getName());
                return;
            }
            log.debug("Custom command '{}' completed", customCommand.getName());
        } finally {
            CUSTOM_COMMAND_LOCK.unlock();
        }
    }

    private void executeStep(CustomCommandStep step, int index, PendingInputSequence pendingInput,
                             CustomCommandExecutionContext ctx) throws InterruptedException {
        switch (step.getType()) {
            case BINDING_TAP -> {
                EventBusManager.publish(new AppLogEvent("Custom command step: BINDING_TAP " + step.getBindingId()));
                pendingInput.addInput(GameInputStep.bindingTap(step.getBindingId()));
            }

            case BINDING_HOLD -> {
                EventBusManager.publish(new AppLogEvent("Custom command step: BINDING_HOLD " + step.getBindingId() + " " + step.getDurationMs() + "ms"));
                pendingInput.addInput(GameInputStep.bindingHold(step.getBindingId(), step.getDurationMs()));
            }

            case DELAY -> {
                EventBusManager.publish(new AppLogEvent("Custom command step: DELAY " + step.getDurationMs() + "ms"));
                pendingInput.addDelay(GameInputStep.delay(step.getDurationMs()));
            }

            case SPEAK -> {
                // Flush accumulated input before speaking so keystrokes reach the game first.
                flushPendingInputSteps(pendingInput);
                String resolvedText = ctx.resolveString(step.getText());
                EventBusManager.publish(new AppLogEvent("Custom command step: SPEAK " + resolvedText));
                speakExecutor.speak(resolvedText);
            }

            case RAW_KEY -> {
                Integer keyCode = KeyBindingExecutor.resolveKeyCode(step.getRawKey());
                if (keyCode == null) {
                    log.warn("Custom command '{}' step {}: unknown rawKey '{}'  step skipped",
                            customCommand.getName(), index, step.getRawKey());
                    EventBusManager.publish(new AppLogEvent(
                            "Custom command step: RAW_KEY " + step.getRawKey() + " (unknown key - skipped)"));
                    break;
                }
                int modCode = 0;
                String rawMod = step.getRawKeyModifier();
                if (rawMod != null && !rawMod.isBlank()) {
                    Integer resolved = KeyBindingExecutor.resolveKeyCode(rawMod);
                    if (resolved == null) {
                        log.warn("Custom command '{}' step {}: unknown rawKeyModifier '{}'  executing without modifier",
                                customCommand.getName(), index, rawMod);
                    } else {
                        modCode = resolved;
                    }
                }
                String logSuffix = (modCode != 0 ? " + " + rawMod : "")
                        + (step.getDurationMs() > 0 ? " " + step.getDurationMs() + "ms" : "");
                EventBusManager.publish(new AppLogEvent("Custom command step: RAW_KEY " + step.getRawKey() + logSuffix));
                pendingInput.addInput(GameInputStep.rawKey(keyCode, modCode, step.getDurationMs()));
            }

            case RUN_COMMAND -> {
                // Flush before delegating so pending keystrokes are sent before the nested handler runs.
                flushPendingInputSteps(pendingInput);
                CommandHandler nested = CommandHandlerFactory.getInstance()
                        .getCommandHandlers()
                        .get(step.getActionId());
                if (nested == null) {
                    log.warn("Custom command '{}' step {}: unknown actionId '{}' - step skipped",
                            customCommand.getName(), index, step.getActionId());
                    EventBusManager.publish(new AppLogEvent("Custom command step: RUN_COMMAND " + step.getActionId() + " (unknown - skipped)"));
                } else if (nested instanceof CustomCommandHandler) {
                    // Prevent cross-customCommand delegation - customCommands must not call other customCommands.
                    log.warn("Custom command '{}' step {}: RUN_COMMAND may not target another custom command ('{}') - step skipped",
                            customCommand.getName(), index, step.getActionId());
                    EventBusManager.publish(new AppLogEvent("Custom command step: RUN_COMMAND " + step.getActionId() + " (nested custom command blocked)"));
                } else {
                    // Resolve step-level param mapping; preserves JSON types for bare ${ref} values.
                    Map<String, String> stepParamMapping = step.getStepParams();
                    JsonObject resolvedParams = ctx.resolveStepParams(stepParamMapping);
                    EventBusManager.publish(new AppLogEvent("Custom command step: RUN_COMMAND " + step.getActionId()));
                    nested.handle(step.getActionId(), resolvedParams, "");
                }
            }
        }
    }

    private void flushPendingInputSteps(PendingInputSequence pendingInput) throws InterruptedException {
        if (pendingInput.isEmpty()) {
            return;
        }
        if (pendingInput.hasInput()) {
            GameControllerBus.publish(new GameInputSequenceEvent(pendingInput.steps()));
        } else {
            for (GameInputStep step : pendingInput.steps()) {
                Thread.sleep(step.getDurationMs());
            }
        }
        pendingInput.clear();
    }

    /**
     * Accumulates input steps (bindings, raw keys) and delays before publishing them as a single
     * {@link GameInputSequenceEvent}. Delays without any real input are executed via
     * {@link Thread#sleep} instead, since the executor does not add inter-step pauses for delay-only sequences.
     */
    private static final class PendingInputSequence {
        private final List<GameInputStep> steps = new ArrayList<>();
        private boolean hasInput;

        void addInput(GameInputStep step) {
            steps.add(step);
            hasInput = true;
        }

        void addDelay(GameInputStep step) {
            steps.add(step);
        }

        boolean isEmpty() {
            return steps.isEmpty();
        }

        boolean hasInput() {
            return hasInput;
        }

        List<GameInputStep> steps() {
            return steps;
        }

        void clear() {
            steps.clear();
            hasInput = false;
        }
    }
}
