package elite.intel.ui.event;

/**
 * Published after each AI connection check ({@link elite.intel.ai.brain.health.AiServiceCheck}): by the startup
 * check and its 30s retries, and by the self diagnostic the commander can ask for.
 * Subscribers should apply UI updates via {@code SwingUtilities.invokeLater}.
 */
public record LlmConnectionStatusEvent(boolean connected) {
}
