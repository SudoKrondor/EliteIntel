package elite.intel.ui.event;

/**
 * Asks for Elite-Intel to restart itself: the main window settles BindForge's edits not applied first, exactly as
 * closing the window does, and only then starts the new copy and exits. Go back cancels the restart, with nothing
 * started.
 * <p>
 * WHY through the window: relaunching first and asking second would leave a new copy already waiting on this one
 * when the user chooses Go back.
 *
 * @param relaunchFailed runs on the EDT when the new copy could not be started, so the caller can say to restart by
 *                       hand
 */
public record RelaunchRequestedEvent(Runnable relaunchFailed) {
}
