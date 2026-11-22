package elite.intel.gameapi.journal.subscribers;

import java.time.Duration;
import java.time.Instant;

/**
 * How many cargo-scan callouts a burst of scans earns.
 *
 * <p>WHY: a compromised nav beacon hosts a pirate pack, and every pirate scans the hold on arrival. The
 * reported drop had seven of them finish within nine seconds, and each {@code Scanned} event spoke
 * "Cargo scan detected!" on the voice that cannot be interrupted - seven identical lines that drowned the
 * answer to the question the commander had just asked. The first scan is news, a second one says the
 * commander faces a pack rather than a lone pirate, and the rest add nothing.
 *
 * <p>So the first scan of a burst is {@link Alert#SINGLE}, the second is {@link Alert#MULTIPLE}, and every
 * further one is {@link Alert#SILENT}. The window slides: each scan pushes the silence out again, and the
 * gate re-arms only after {@link #QUIET_PERIOD} with no scan at all.
 */
final class CargoScanAlertGate {

    /** What to say about one cargo scan. */
    enum Alert { SINGLE, MULTIPLE, SILENT }

    /**
     * How long the hold must go unscanned before the next scan is news again.
     *
     * <p>The reported pack's longest gap between two scans was five seconds. Thirty keeps one pack one
     * burst with room to spare, while a lone scan at the next beacon a supercruise hop later still speaks.
     */
    static final Duration QUIET_PERIOD = Duration.ofSeconds(30);

    private Instant lastScan;
    private int scansInBurst;

    /**
     * The callout this scan earns. Timed on the journal's own clock, which measures the burst and stays
     * right when the app is busy.
     */
    synchronized Alert admit(Instant scanned) {
        if (lastScan == null || Duration.between(lastScan, scanned).compareTo(QUIET_PERIOD) > 0) {
            scansInBurst = 0;
        }
        lastScan = scanned;
        scansInBurst++;
        return switch (scansInBurst) {
            case 1 -> Alert.SINGLE;
            case 2 -> Alert.MULTIPLE;
            default -> Alert.SILENT;
        };
    }
}
