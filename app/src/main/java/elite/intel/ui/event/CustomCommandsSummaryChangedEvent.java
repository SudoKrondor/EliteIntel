package elite.intel.ui.event;

/** Published when the active custom command count changes (load or edit). */
public record CustomCommandsSummaryChangedEvent(int count) {}
