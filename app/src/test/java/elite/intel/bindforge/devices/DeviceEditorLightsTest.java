package elite.intel.bindforge.devices;

import elite.intel.devices.events.DeviceHatEvent;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Live highlighting: lit only while the input is active, axes by which axis they are. */
class DeviceEditorLightsTest {

    /** SDL's axes 0, 1 and 2 on a stick reporting X, Y and RZ. */
    private static final DeviceDescription TWIST_ON_RZ = new DeviceDescription(true,
            List.of(GameAxis.X, GameAxis.Y, GameAxis.RZ), 0, List.of(GameAxis.X, GameAxis.Y, GameAxis.RZ), 1);

    @Test
    void aButtonIsLitWhileHeld() {
        DeviceEditorLights lights = new DeviceEditorLights(TWIST_ON_RZ, false);

        assertTrue(lights.button(13, true));
        assertEquals(Set.of("Joy_14"), lights.lit(), "SDL's 0-based index is the game's 1-based button");
        assertTrue(lights.button(13, false));
        assertTrue(lights.lit().isEmpty());
    }

    /** SDL's axis 2 is RZ here, not Z. */
    @Test
    void anAxisIsLitByWhichAxisItIs() {
        DeviceEditorLights lights = new DeviceEditorLights(TWIST_ON_RZ, false);

        lights.axis(2, 0.0f);
        lights.axis(2, 0.5f);

        assertEquals(Set.of("Joy_RZAxis"), lights.lit());
    }

    @Test
    void gamepadTriggerReturnsToItsNegativeRestValue() {
        DeviceEditorLights lights = new DeviceEditorLights(DeviceDescription.gamepad(6, 0), true);

        assertTrue(lights.axis(2, -0.2f));
        assertEquals(Set.of("Joy_ZAxis"), lights.lit());
        assertTrue(lights.axis(2, -1.0f));
        assertTrue(lights.lit().isEmpty());
    }

    @Test
    void gamepadStickRestsAtCentreEvenWhenFirstSeenDisplaced() {
        DeviceEditorLights lights = new DeviceEditorLights(DeviceDescription.gamepad(6, 0), true);

        assertTrue(lights.axis(0, 0.6f));
        assertEquals(Set.of("Joy_XAxis"), lights.lit());
    }

    /** Rest is the first value seen, so a throttle parked at one end does not light the moment the dialog opens. */
    @Test
    void anAxisRestsWhereItWasFirstSeenAndJitterNeverShows() {
        DeviceEditorLights lights = new DeviceEditorLights(TWIST_ON_RZ, false);

        assertFalse(lights.axis(0, -1.0f));
        assertFalse(lights.axis(0, -0.95f), "within 5% of its travel from rest");
        assertTrue(lights.axis(0, -0.85f));
        assertEquals(Set.of("Joy_XAxis"), lights.lit());
        assertTrue(lights.axis(0, -0.98f), "back near rest, it goes out");
        assertTrue(lights.lit().isEmpty());
    }

    @Test
    void noAxisIsLitWhenSdlsNumberingIsNotMatched() {
        DeviceEditorLights lights = new DeviceEditorLights(DeviceDescription.axesUnknown(0), false);

        lights.axis(0, -1.0f);
        assertFalse(lights.axis(0, 1.0f));
        assertTrue(lights.lit().isEmpty());
    }

    /** An 8-way hat's diagonal lights two directions (testing item 20). */
    @Test
    void aHatLightsEachDirectionPushedAndGoesOutAtCentre() {
        DeviceEditorLights lights = new DeviceEditorLights(TWIST_ON_RZ, false);

        lights.hat(0, DeviceHatEvent.UP | DeviceHatEvent.RIGHT);
        assertEquals(Set.of("Joy_POV1Up", "Joy_POV1Right"), lights.lit());

        lights.hat(0, DeviceHatEvent.RIGHT);
        assertEquals(Set.of("Joy_POV1Right"), lights.lit());

        lights.hat(0, 0);
        assertTrue(lights.lit().isEmpty());
    }

    @Test
    void clearingPutsEverythingOutAndForgetsWhereAxesRest() {
        DeviceEditorLights lights = new DeviceEditorLights(TWIST_ON_RZ, false);
        lights.button(0, true);
        lights.axis(1, 0.0f);

        lights.clear();

        assertTrue(lights.lit().isEmpty());
        assertFalse(lights.axis(1, 0.6f), "the first value after clearing is the new rest");
    }
}
