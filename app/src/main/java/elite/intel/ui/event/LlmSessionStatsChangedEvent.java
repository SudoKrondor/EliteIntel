package elite.intel.ui.event;

import elite.intel.ui.telemetry.LlmSessionStatsSnapshot;

/**
 * Published by {@link elite.intel.ui.telemetry.LlmSessionStatsTracker} after every stats change or
 * session reset. Subscribers should apply UI updates via {@code SwingUtilities.invokeLater}.
 */
public record LlmSessionStatsChangedEvent(LlmSessionStatsSnapshot snapshot) {}
