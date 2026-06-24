package elite.intel.ui.event;

/** Published when the in-game commander name becomes known or changes. */
public record CommanderChangedEvent(String commanderName) {}
