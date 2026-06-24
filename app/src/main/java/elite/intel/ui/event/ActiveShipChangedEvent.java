package elite.intel.ui.event;

/** Published when the active ship loadout changes and a display name is available. */
public record ActiveShipChangedEvent(String shipName) {}
