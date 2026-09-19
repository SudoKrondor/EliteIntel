package elite.intel.ui.event;

/**
 * UI-layer signal that the Exo-Mastery ledger changed - a body was sampled out, the catalogue was
 * loaded or unloaded - so the stats on the Commander tab re-read. Carries nothing: the panel reads
 * the totals back from the manager, off the EDT.
 */
public class ExoMasteryChangedEvent {
}
