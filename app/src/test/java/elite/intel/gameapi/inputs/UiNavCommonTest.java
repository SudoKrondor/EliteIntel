package elite.intel.gameapi.inputs;

import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The map toggle has to name the binding for the vehicle the commander is actually in: Elite's ship
 * map binding is inert in an SRV and on foot, and a tap on it fails silently. Reported 2026-09-09,
 * where a galaxy map EliteIntel had opened from an SRV could not be closed again by it.
 */
class UiNavCommonTest {

    private static final long IN_MAIN_SHIP = 16777216L;
    private static final long IN_SRV = 67108864L;
    private static final long ON_FOOT = 1L;

    private final long savedFlags = Status.getInstance().getStatus().getFlags();
    private final long savedFlags2 = Status.getInstance().getStatus().getFlags2();

    @AfterEach
    void restore() {
        setStatus(savedFlags, savedFlags2);
    }

    @Test
    void galaxyMapTogglePicksTheBindingForTheCurrentVehicle() {
        setStatus(IN_MAIN_SHIP, 0L);
        assertEquals("GalaxyMapOpen", binding(UiNavCommon.galaxyMapToggleStep()));

        setStatus(IN_SRV, 0L);
        assertEquals("GalaxyMapOpen_Buggy", binding(UiNavCommon.galaxyMapToggleStep()));

        setStatus(0L, ON_FOOT);
        assertEquals("GalaxyMapOpen_Humanoid", binding(UiNavCommon.galaxyMapToggleStep()));
    }

    @Test
    void systemMapTogglePicksTheBindingForTheCurrentVehicle() {
        setStatus(IN_MAIN_SHIP, 0L);
        assertEquals("SystemMapOpen", binding(UiNavCommon.systemMapToggleStep()));

        setStatus(IN_SRV, 0L);
        assertEquals("SystemMapOpen_Buggy", binding(UiNavCommon.systemMapToggleStep()));

        setStatus(0L, ON_FOOT);
        assertEquals("SystemMapOpen_Humanoid", binding(UiNavCommon.systemMapToggleStep()));
    }

    @Test
    void anUnrecognisedVehicleStateFallsBackToTheShipBinding() {
        // The old per-command chains tested ship/fighter, then SRV, then on foot, and tapped nothing at
        // all when none matched. A map key that does nothing is worse than one aimed at the wrong vehicle.
        setStatus(0L, 0L);
        assertEquals("GalaxyMapOpen", binding(UiNavCommon.galaxyMapToggleStep()));
        assertEquals("SystemMapOpen", binding(UiNavCommon.systemMapToggleStep()));
    }

    private static String binding(GameInputStep step) {
        return step.getBindingId();
    }

    private static void setStatus(long flags, long flags2) {
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        snapshot.setFlags(flags);
        snapshot.setFlags2(flags2);
        Status.getInstance().setStatus(snapshot);
    }
}
