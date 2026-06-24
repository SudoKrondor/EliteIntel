package elite.intel.ui.event;

/**
 * Published by
 * {@link elite.intel.ai.brain.actions.handlers.query.ConnectionCheckQueryCommand}
 * after each LLM connection check (startup and retry).
 * The event is published on every call to {@code verifyConnection()},
 * including recurring 30s retries, not just at startup.
 * Subscribers should apply UI updates via {@code SwingUtilities.invokeLater}.
 */
public record LlmConnectionStatusEvent(boolean connected) {
}
