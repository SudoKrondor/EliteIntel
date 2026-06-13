package elite.intel.ui.event;

/**
 * Published when the working-copy sync state relative to the game {@code .binds} file changes.
 * {@code inSync = true} means the working copy matches the game file; {@code false} means
 * there are unapplied editor changes.
 */
public record KeymapSyncStateChangedEvent(boolean inSync) {}
