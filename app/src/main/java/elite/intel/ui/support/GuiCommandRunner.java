package elite.intel.ui.support;


import com.google.gson.JsonObject;
import elite.intel.ai.brain.commons.ResponseRouter;

import javax.swing.Timer;
import java.awt.Frame;
import java.awt.Window;
import java.util.Objects;

/**
 * Centralizes GUI-triggered command dispatch that must leave the application window before sending input.
 */
public final class GuiCommandRunner {

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
                event -> ResponseRouter.getInstance().executeCommandFromGUI(action, safeParams, speakAffirmation)
        );
        dispatchTimer.setRepeats(false);
        dispatchTimer.start();
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
