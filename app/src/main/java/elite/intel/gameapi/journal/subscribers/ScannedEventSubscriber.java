package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.gameapi.journal.events.ScannedEvent;

import java.time.Instant;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class ScannedEventSubscriber {

    /** One callout per burst of scans, not one per pirate: see {@link CargoScanAlertGate}. */
    private final CargoScanAlertGate cargoScans = new CargoScanAlertGate();

    @Subscribe
    public void onScannedEvent(ScannedEvent event) {
        if (!"cargo".equalsIgnoreCase(event.getScanType())) {
            return;
        }
        // Parsing is safe here: the parser already read this timestamp to decide the event was not a replay.
        switch (cargoScans.admit(Instant.parse(event.getTimestamp()))) {
            case SINGLE -> EventNarrator.critical(localizedEvent("event.cargo.scanDetected"));
            case MULTIPLE -> EventNarrator.critical(localizedEvent("event.cargo.multipleScansDetected"));
            case SILENT -> {
            }
        }
    }
}
