package elite.intel.ui.event;

/**
 * Published after bindings are loaded or reloaded.
 *
 * @param missing  number of EliteIntel-required bindings without a keyboard assignment
 * @param connected number of EliteIntel-required bindings with a usable keyboard assignment
 */
public record BindingsSummaryChangedEvent(int missing, int connected) {}
