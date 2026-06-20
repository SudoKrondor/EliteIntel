package elite.intel.ai.brain.commons;

import elite.intel.eventbus.GameEventBus;

/**
 * Published on {@link GameEventBus} immediately before a command or query
 * handler's {@code handle()} method is invoked.
 * <p>
 * In production no subscriber listens to this event - zero overhead.
 * A test harness subscribes to it to assert that the correct handler was routed to
 * without letting the handler's side-effects (keystrokes, TTS) actually execute.
 */
public class HandlerDispatchedEvent {
    private final String action;
    private final String handlerClass;
    private final boolean command;

    public HandlerDispatchedEvent(String action, String handlerClass, boolean command) {
        this.action = action;
        this.handlerClass = handlerClass;
        this.command = command;
    }

    /**
     * The action name as returned by the LLM (matches the key in the handler registry).
     */
    public String getAction() {
        return action;
    }

    /**
     * Simple class name of the handler that was selected, e.g. {@code "DeployLandingGearHandler"}.
     */
    public String getHandlerClass() {
        return handlerClass;
    }

    /**
     * {@code true} for command handlers, {@code false} for query handlers.
     */
    public boolean isCommand() {
        return command;
    }
}
