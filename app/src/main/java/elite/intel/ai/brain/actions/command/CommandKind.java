package elite.intel.ai.brain.actions.command;

/**
 * Self-described catalog display kind of a built-in command.
 * <p>
 * Lives in the {@code command} package so {@link IntelCommand} can return it without
 * depending on the {@code catalog} package (which already depends on {@code command});
 * the catalog maps this to its own {@code CommandCatalogEntryType}.
 */
public enum CommandKind {
    /** Single game-binding tap (the SimpleTapCommand cluster). */
    BINDING,
    /** Handler-driven action (everything else). */
    ACTION
}
