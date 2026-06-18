package elite.intel.ai.brain.actions.catalog;

/**
 * High-level command catalog categories.
 */
public enum CommandCatalogEntryType {
    /**
     * Built-in command that taps a single game binding (non-null binding).
     */
    BUILT_IN_BINDING,

    /**
     * Built-in handler-driven action. This is not a user-editable customCommand.
     */
    BUILT_IN_ACTION,

    /**
     * Reserved for future user-defined editable customCommands.
     */
    CUSTOM_COMMAND
}
