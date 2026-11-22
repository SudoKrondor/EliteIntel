package elite.intel.ui.event;

/**
 * Something in the bindings folder was created or changed, any file - seen by {@code BindingsMonitor}'s watch loop, once
 * per batch of folder events. BindForge's check then runs again at the next click on the BindForge tab (storage.md rule
 * 5; Storage S17). Published on the monitor thread.
 * <p>
 * WHY not {@link BindingsUpdatedEvent}: that one fires only when the file the assistant reads is parsed again, so a save
 * to another binds file would never reach the check.
 */
public record BindingsFolderChangedEvent() {
}
