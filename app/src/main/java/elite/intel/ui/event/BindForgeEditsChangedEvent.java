package elite.intel.ui.event;

/**
 * BindForge's edits not applied changed - an edit made, discarded or written, or a write started or ended - so the
 * APPLY / DISCARD bar reads {@code bindforge.storage.Edits} again. Published on whichever thread made the change.
 */
public record BindForgeEditsChangedEvent() {
}
