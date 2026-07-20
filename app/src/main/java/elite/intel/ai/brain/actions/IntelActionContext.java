package elite.intel.ai.brain.actions;

/**
 * Invocation surfaces that expose built-in actions. An action can opt out of a surface whose runtime
 * cannot satisfy its contract or whose delegation would bypass an ownership boundary.
 */
public enum IntelActionContext {
    COMPANION_COMMANDER,
    LEGACY_ACTION_MAP,
    CUSTOM_COMMAND,
    GUI
}
