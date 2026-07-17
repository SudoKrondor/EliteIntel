package elite.intel.ui.support;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.IntelActionContext;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Centralizes GUI-triggered command dispatch, both for game-facing actions that first transfer
 * foreground focus and for app-side actions that should leave the current window open.
 */
public final class GuiCommandRunner {

    private static final Logger log = LogManager.getLogger(GuiCommandRunner.class);
    private static final int GUI_COMMAND_DISPATCH_DELAY_MS = 3000;

    private GuiCommandRunner() {
    }

    /**
     * Closes the source window, activates the Elite Dangerous window if it is running, then
     * dispatches the command after {@value #GUI_COMMAND_DISPATCH_DELAY_MS} ms.
     * <p>
     * The delay is intentionally long: Elite Dangerous resets the audio device when it gains
     * foreground focus, which would cut off any TTS speech started immediately after the switch.
     * The pause lets the audio subsystem stabilise before the command (and any SPEAK steps) runs.
     * If the game window is not found the application owner is iconified instead, and the command
     * dispatches to whatever window holds focus at that point.
     */
    public static void runAfterClosingWindow(Window sourceWindow, String action, JsonObject params, boolean speakAffirmation) {
        Objects.requireNonNull(action, "action");
        JsonObject safeParams = params == null ? new JsonObject() : params;

        Window owner = sourceWindow == null ? null : sourceWindow.getOwner();
        if (sourceWindow != null) {
            sourceWindow.dispose();
        }
        boolean gameActivated = GameWindowActivator.activateEliteDangerousWindow();
        if (!gameActivated) {
            moveOwnerOutOfForeground(owner);
        }

        scheduleDispatch(action, safeParams, speakAffirmation);
    }

    /**
     * Activates Elite Dangerous without closing the application and schedules the command only when
     * foreground activation succeeds. Returns {@code false} without dispatching any game input when
     * the game window cannot be activated. Call from the Swing EDT.
     */
    public static boolean runAfterActivatingGame(String action, JsonObject params, boolean speakAffirmation) {
        return runAfterActivatingGame(
                action, params, speakAffirmation, GameWindowActivator::activateEliteDangerousWindow);
    }

    /** Test seam for verifying that failed foreground activation never schedules command input. */
    static boolean runAfterActivatingGame(
            String action,
            JsonObject params,
            boolean speakAffirmation,
            BooleanSupplier gameActivator
    ) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(gameActivator, "gameActivator");
        JsonObject safeParams = params == null ? new JsonObject() : params;
        if (!gameActivator.getAsBoolean()) {
            return false;
        }
        scheduleDispatch(action, safeParams, speakAffirmation);
        return true;
    }

    private static void scheduleDispatch(String action, JsonObject params, boolean speakAffirmation) {
        Timer dispatchTimer = new Timer(
                GUI_COMMAND_DISPATCH_DELAY_MS,
                event -> dispatchCommand(action, params, speakAffirmation, null)
        );
        dispatchTimer.setRepeats(false);
        dispatchTimer.start();
    }

    /**
     * Dispatches an existing command without closing windows, changing foreground focus, or delaying execution.
     * The handler runs asynchronously; {@code onComplete} is queued exactly once on the Swing EDT after success,
     * a handler failure, or an unavailable action.
     */
    public static void runInApp(String action, JsonObject params, boolean speakAffirmation, Runnable onComplete) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(onComplete, "onComplete");
        JsonObject safeParams = params == null ? new JsonObject() : params;

        dispatchCommand(action, safeParams, speakAffirmation, onComplete);
    }

    /**
     * Dispatches a GUI-selected command straight to its handler, bypassing STT/LLM classification: the UI
     * already resolved the action id and any params. Uses the same shared handler map as the companion
     * execution gateway ({@link CommandHandlerFactory}, built-ins + custom commands), so it is independent
     * of the companion runtime lifecycle (the button can fire while services are stopped). Commands that opt out
     * of the GUI context are rejected before acknowledgement. Built-in commands speak an affirmative preamble;
     * custom commands do not (see {@code CommandDetailsDialog#runCommand}).
     */
    private static void dispatchCommand(
            String action,
            JsonObject params,
            boolean speakAffirmation,
            Runnable onComplete
    ) {
        Runnable complete = edtCompletion(onComplete);
        IntelAction handler = CommandHandlerFactory.getInstance().registerCommandHandlers().get(action);
        if (handler == null || !handler.isAvailableIn(IntelActionContext.GUI)) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("command not found"));
            complete.run();
            return;
        }
        if (speakAffirmation) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.affirmative()));
        }
        new Thread(() -> {
            try {
                publishSpokenResponse(handler.handle(action, params, ""));
            } catch (Exception e) {
                GameEventBus.publish(new AiVoxResponseEvent("Error processing command for action " + action + " see logs."));
                log.error("GUI command dispatch failed for action {}: {}", action, e.getMessage(), e);
            } finally {
                complete.run();
            }
        }, "GuiCommandDispatch").start();
    }

    private static void publishSpokenResponse(JsonObject response) {
        if (response == null || !response.has(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE)) {
            return;
        }
        JsonElement spoken = response.get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE);
        if (spoken == null || spoken.isJsonNull() || !spoken.isJsonPrimitive()) {
            return;
        }
        String text = spoken.getAsString();
        if (!text.isBlank()) {
            GameEventBus.publish(new AiVoxResponseEvent(text));
        }
    }

    private static Runnable edtCompletion(Runnable onComplete) {
        if (onComplete == null) {
            return () -> {
            };
        }
        AtomicBoolean scheduled = new AtomicBoolean();
        return () -> {
            if (scheduled.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(onComplete);
            }
        };
    }

    private static void moveOwnerOutOfForeground(Window owner) {
        if (owner == null) {
            return;
        }
        if (owner instanceof Frame frame) {
            frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED);
            return;
        }
        owner.toBack();
    }
}
