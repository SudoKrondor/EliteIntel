package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.gameapi.journal.events.CommitCrimeEvent;
import elite.intel.gameapi.journal.events.SupercruiseEntryEvent;

import java.util.List;

/**
 * Two questions about a crime, answered in one place because the second depends on the first: whether to
 * interrupt the commander now ({@link CrimeAlertGate}), and what to tell him on the way out about the ones
 * that were held back ({@link CrimeSpreeTally}).
 */
public class CommitCrimeEventSubscriber {

    private final CrimeAlertGate gate = new CrimeAlertGate();
    private final CrimeSpreeTally tally = new CrimeSpreeTally();

    @Subscribe
    public void onCommitCrimeEvent(CommitCrimeEvent event) {
        // Both decisions are taken on the bus thread, before the narration is handed to a thread of its
        // own, so that two crimes a second apart cannot both pass the gate or be counted out of order.
        boolean announced = gate.admit(event);
        tally.record(event, announced);
        if (announced) {
            Thread.ofVirtual().start(() -> EventNarrator.critical(CrimeReportPhrase.of(event)));
        }
    }

    /**
     * The commander has stopped being somewhere and started travelling, so the spree is over and the bill
     * is due. Spoken with the interruptible voice: it is a reckoning, not a warning.
     */
    @Subscribe
    public void onSupercruiseEntryEvent(SupercruiseEntryEvent event) {
        List<String> reckoning = tally.close();
        if (reckoning.isEmpty()) return;
        Thread.ofVirtual().start(() -> reckoning.forEach(EventNarrator::say));
    }
}
