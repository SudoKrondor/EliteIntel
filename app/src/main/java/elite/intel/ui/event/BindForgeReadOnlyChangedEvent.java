package elite.intel.ui.event;

/**
 * Whether BindForge is read only changed - worked out at the first ask in a run, overridden, or a missing installation
 * removed - so the bar and File Manager read {@code bindforge.storage.ReadOnlyMode} again. Published on whichever
 * thread made the change.
 */
public record BindForgeReadOnlyChangedEvent() {
}
