package elite.intel.junit.gameapi;

import com.google.gson.JsonObject;
import elite.intel.gameapi.FsdTargetPreScanAccumulator;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.journal.events.FSDTargetEvent;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The live parser skips everything written before app start, so a target the game selected while the app was closed
 * never replaced the stored one. The pre-scan restores the last one the journal names.
 */
class FsdTargetPreScanAccumulatorTest {

    /**
     * Comfortably before app start, so these fixtures count as replays.
     */
    private static final String T1 = "2026-01-01T19:29:50Z";
    private static final String T2 = "2026-01-01T19:31:48Z";

    private final PlayerSession session = PlayerSession.getInstance();
    private FsdTarget saved;

    @BeforeEach
    void rememberTarget() {
        saved = session.getFsdTarget();
        session.setFsdTarget(new FsdTarget("Wangakwan", "K", null, null, null, null, "Refuel Possible"));
    }

    @AfterEach
    void restoreTarget() {
        if (saved != null) session.setFsdTarget(saved);
    }

    /**
     * The support-bundle case: two targets replayed, the newer one selected after the app was closed.
     */
    @Test
    void theLastReplayedTargetReplacesTheStoredOne() {
        FsdTargetPreScanAccumulator acc = new FsdTargetPreScanAccumulator();

        acc.onFsdTarget(target(T1, "Trianguli Sector EQ-Y b2", "M"));
        acc.onFsdTarget(target(T2, "Trianguli Sector RO-Q a5-0", "TTS"));
        acc.persist();

        assertEquals("Trianguli Sector RO-Q a5-0", session.getFsdTarget().getName());
        assertEquals("TTS", session.getFsdTarget().getStarClass());
        assertEquals("TTS", session.getFsdTarget().getLocation().getStarClass());
    }

    @Test
    void journalsWithNoTargetLeaveTheStoredOneAlone() {
        new FsdTargetPreScanAccumulator().persist();

        assertEquals("Wangakwan", session.getFsdTarget().getName());
    }

    @Test
    void liveTargetsAreLeftToTheLiveSubscriber() {
        FsdTargetPreScanAccumulator acc = new FsdTargetPreScanAccumulator();

        acc.onFsdTarget(target(Instant.now().plusSeconds(60).toString(), "Sol", "G"));
        acc.persist();

        assertEquals("Wangakwan", session.getFsdTarget().getName());
    }

    private static FSDTargetEvent target(String timestamp, String name, String starClass) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", timestamp);
        json.addProperty("event", "FSDTarget");
        json.addProperty("Name", name);
        json.addProperty("SystemAddress", 5373540715288L);
        json.addProperty("StarClass", starClass);
        json.addProperty("RemainingJumpsInRoute", 5);
        return new FSDTargetEvent(json);
    }
}
