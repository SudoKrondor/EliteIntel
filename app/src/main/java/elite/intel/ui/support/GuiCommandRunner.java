package elite.intel.ui.support;


import com.google.gson.JsonObject;
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

/**
 * Centralizes GUI-triggered command dispatch that must leave the application window before sending input.
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

        Timer dispatchTimer = new Timer(
                GUI_COMMAND_DISPATCH_DELAY_MS,
                event -> dispatchCommand(action, safeParams, speakAffirmation)
        );
        dispatchTimer.setRepeats(false);
        dispatchTimer.start();
    }

    /**
     * Dispatches a GUI-selected command straight to its handler, bypassing STT/LLM classification: the UI
     * already resolved the action id and any params. Uses the same shared handler map as the companion
     * execution gateway ({@link CommandHandlerFactory}, built-ins + custom commands), so it is independent
     * of the companion runtime lifecycle (the button can fire while services are stopped). Commands that opt out
     * of the GUI context are rejected before acknowledgement. Built-in commands speak an affirmative preamble;
     * custom commands do not (see {@code CommandDetailsDialog#runCommand}).
     */
    private static void dispatchCommand(String action, JsonObject params, boolean speakAffirmation) {
        IntelAction handler = CommandHandlerFactory.getInstance().registerCommandHandlers().get(action);
        if (handler == null || !handler.isAvailableIn(IntelActionContext.GUI)) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("command not found"));
            return;
        }
        if (speakAffirmation) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.affirmative()));
        }
        new Thread(() -> {
            try {
                handler.handle(action, params, "");
            } catch (Exception e) {
                GameEventBus.publish(new AiVoxResponseEvent("Error processing command for action " + action + " see logs."));
                log.error("GUI command dispatch failed for action {}: {}", action, e.getMessage(), e);
            }
        }, "GuiCommandDispatch").start();
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
