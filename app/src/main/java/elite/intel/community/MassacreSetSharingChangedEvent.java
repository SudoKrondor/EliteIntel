package elite.intel.community;

/**
 * The commander turned sharing of massacre sets on or off. Published on the game bus, so the sync uploads or
 * withdraws this install's report straight away instead of at its next hourly check.
 */
public record MassacreSetSharingChangedEvent(boolean on) {
}
