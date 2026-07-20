package elite.intel.ai.brain.vega.model;

import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateSnapshotTest {

    @Test
    void remainsStableWhenTheSourceStatusChanges() {
        Status source = Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE);
        source.setFighterOut(true);
        GameStateSnapshot snapshot = GameStateSnapshot.capture(source);

        source.getStatus().setFlags(0L);
        source.getStatus().setFlags2(1L); // on foot
        source.setFighterOut(false);

        Status visibility = snapshot.visibilityStatus();
        assertTrue(visibility.isInMainShip(), "the captured ship context must not follow later source changes");
        assertFalse(visibility.isOnFoot(), "flags2 must also remain frozen");
        assertTrue(visibility.isFighterOut(), "non-flag visibility state is part of the same snapshot");
    }
}
