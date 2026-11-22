package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ExoMasteryManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.gamestate.status_events.BioSurveyCompletedEvent;
import elite.intel.gameapi.journal.events.ScanOrganicEvent;
import elite.intel.ui.event.ExoMasteryChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ticks bodies off the Exo-Mastery catalogue as the commander samples them.
 * <p>
 * Two signals, both silent - the scan itself is announced by {@code ScanOrganicSubscriber}, and the
 * catalogue is bookkeeping, not news:
 * <ul>
 *   <li>the survey-complete latch flipping on a location row, whoever flipped it, which is the game's
 *       own word that the body is sampled out;</li>
 *   <li>the third scan of a species, which ticks that species off and completes the body when it was
 *       the last one the catalogue listed there. The catalogue lists only the species worth the trip,
 *       so a body can be finished here while the game still has cheaper organisms on it - the money
 *       the commander came for has been collected, which is what the list is for.</li>
 * </ul>
 * The survey latch is not looked at when the catalogue is not loaded: {@link ExoMasteryManager#isEnabled()} is a
 * cached flag. A final scan is recorded either way - one insert, at most a few times an hour - because it is the
 * record a catalogue loaded later is checked against.
 */
public class ExoMasterySubscriber {

    private static final Logger log = LogManager.getLogger(ExoMasterySubscriber.class);

    /**
     * The journal's ScanType for the third and final scan of a species.
     */
    private static final String FINAL_SCAN = "Analyse";

    private final ExoMasteryManager exoMastery = ExoMasteryManager.getInstance();

    @Subscribe
    public void onBioSurveyCompleted(BioSurveyCompletedEvent event) {
        if (!exoMastery.isEnabled()) return;
        if (exoMastery.setBodyCompleted(event.systemAddress(), event.bodyId(), event.completed())) {
            log.debug("Exo-Mastery body {}/{} {}", event.systemAddress(), event.bodyId(),
                    event.completed() ? "completed by survey" : "reopened");
            UiBus.publish(new ExoMasteryChangedEvent());
        }
    }

    @Subscribe
    public void onScanOrganic(ScanOrganicEvent event) {
        if (!FINAL_SCAN.equalsIgnoreCase(event.getScanType()) || event.getBody() == null) return;
        // Recorded with the catalogue unloaded too: the game will not let this species be sampled here again, and a
        // catalogue loaded next month must not send the commander back for it.
        String species = BioForms.normalizeSpecies(event.getSpecies());
        if (exoMastery.recordSampled(event.getSystemAddress(), event.getBody(), species)) {
            log.debug("Exo-Mastery body {}/{} completed: last listed species {} sampled",
                    event.getSystemAddress(), event.getBody(), species);
            UiBus.publish(new ExoMasteryChangedEvent());
        }
    }
}
