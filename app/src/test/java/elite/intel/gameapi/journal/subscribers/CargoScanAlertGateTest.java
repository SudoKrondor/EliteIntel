package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.subscribers.CargoScanAlertGate.Alert;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static elite.intel.gameapi.journal.subscribers.CargoScanAlertGate.Alert.MULTIPLE;
import static elite.intel.gameapi.journal.subscribers.CargoScanAlertGate.Alert.SILENT;
import static elite.intel.gameapi.journal.subscribers.CargoScanAlertGate.Alert.SINGLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The reported drop: a compromised nav beacon in Trianguli Sector GW-V a2-2, seven pirates, seven
 * {@code Scanned} events in nine seconds, seven identical callouts.
 */
class CargoScanAlertGateTest {

    /** The seven scans of the reported pack, verbatim from the journal. */
    private static final String[] THE_PACK = {
            "01:07:07", "01:07:08", "01:07:08", "01:07:10", "01:07:11", "01:07:11", "01:07:16",
    };

    private static Instant at(String time) {
        return Instant.parse("2026-11-12T" + time + "Z");
    }

    private static List<Alert> replay(CargoScanAlertGate gate, String... times) {
        List<Alert> alerts = new ArrayList<>();
        for (String time : times) {
            alerts.add(gate.admit(at(time)));
        }
        return alerts;
    }

    @Test
    void aPackIsAnnouncedOnceAndThenAsMany() {
        assertEquals(List.of(SINGLE, MULTIPLE, SILENT, SILENT, SILENT, SILENT, SILENT),
                replay(new CargoScanAlertGate(), THE_PACK));
    }

    @Test
    void aLoneScanStillSpeaks() {
        assertEquals(List.of(SINGLE), replay(new CargoScanAlertGate(), "00:57:26"));
    }

    @Test
    void theGateReArmsOnceTheHoldHasGoneQuiet() {
        CargoScanAlertGate gate = new CargoScanAlertGate();
        replay(gate, THE_PACK);
        // 31 seconds after the pack's last scan: the next beacon, not the same pack.
        assertEquals(List.of(SINGLE), replay(gate, "01:07:47"));
    }

    @Test
    void eachScanPushesTheSilenceOut() {
        // Gaps of 20 seconds never reach the quiet period, so a slow pack is still one burst.
        assertEquals(List.of(SINGLE, MULTIPLE, SILENT, SILENT),
                replay(new CargoScanAlertGate(), "01:00:00", "01:00:20", "01:00:40", "01:01:00"));
    }
}
