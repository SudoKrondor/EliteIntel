package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.devices.DeviceNames.Problem;
import elite.intel.bindforge.devices.DeviceNames.Verdict;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default-name rule and alias validation.
 * <p>
 * The first six tests are the spec's own table, row for row, so a change to the rule that breaks one of its
 * worked examples fails here by name. Clashes are checked against Frontier's real list rather than an
 * invented one, because a built-in name is the clash the spec actually describes.
 */
class DeviceNamesTest {

    private static final String WARTHOG =
            "Thrustmaster Hotas Warthog Flight Stick And Throttle Combined Edition";
    private static final String WARTHOG_CUT = "ThrustmasterHotasWarthogFlightStickAndThrottleComb";

    private final List<String> frontier = FrontierStockDevices.getInstance().names();

    // --- the spec's table -------------------------------------------------------------------------------

    @Test
    void spacesAreDropped() {
        assertEquals("VirpilControls20220720", DeviceNames.defaultName("Virpil Controls 20220720", frontier));
    }

    /** {@code TRudder} is a different XML tag from Frontier's {@code T-Rudder}, and the same name to a person. */
    @Test
    void aNameClashingWithABuiltInOnceDashesAreIgnoredGetsANumber() {
        assertEquals("TRudder2", DeviceNames.defaultName("T-Rudder", frontier));
    }

    @Test
    void aNameNotStartingWithALetterIsPrefixed() {
        assertEquals("Device3DconnexionSpaceMouse", DeviceNames.defaultName("3Dconnexion SpaceMouse", frontier));
    }

    @Test
    void aNameShorterThanThreeIsPrefixed() {
        assertEquals("DeviceG9", DeviceNames.defaultName("G9", frontier));
    }

    @Test
    void aLongNameIsCutToFifty() {
        String name = DeviceNames.defaultName(WARTHOG, frontier);

        assertEquals(WARTHOG_CUT, name);
        assertEquals(DeviceNames.MAX_LENGTH, name.length());
    }

    /** The number is not appended to a name already at the limit; the name gives way to it. */
    @Test
    void aLongNameThatClashesIsShortenedSoTheNumberFits() {
        String name = DeviceNames.defaultName(WARTHOG, List.of(WARTHOG_CUT));

        assertEquals("ThrustmasterHotasWarthogFlightStickAndThrottleCom2", name);
        assertEquals(DeviceNames.MAX_LENGTH, name.length());
    }

    // --- beyond the table -------------------------------------------------------------------------------

    @Test
    void aTwoDigitNumberShortensTheNameByTwo() {
        List<String> taken = new ArrayList<>(List.of(WARTHOG_CUT));
        for (int n = 2; n <= 9; n++) {
            taken.add(WARTHOG_CUT.substring(0, DeviceNames.MAX_LENGTH - 1) + n);
        }

        String name = DeviceNames.defaultName(WARTHOG, taken);

        assertEquals("ThrustmasterHotasWarthogFlightStickAndThrottleCo10", name);
        assertEquals(DeviceNames.MAX_LENGTH, name.length());
    }

    @Test
    void theLowestFreeNumberIsTaken() {
        assertEquals("LeftStick3", DeviceNames.defaultName("Left Stick", List.of("LeftStick", "left_stick2")));
    }

    @Test
    void nothingUsableLeftFallsBackToController() {
        assertEquals("Controller", DeviceNames.defaultName("!!! ---", List.of()));
        assertEquals("Controller", DeviceNames.defaultName("", List.of()));
        assertEquals("Controller", DeviceNames.defaultName(null, List.of()));
    }

    @Test
    void theFallbackIsNumberedToo() {
        assertEquals("Controller2", DeviceNames.defaultName("", List.of("Controller")));
    }

    /** Folded rather than dropped, so the name stays readable (Alan, 2026-10-06). */
    @Test
    void accentsAreFoldedAndOtherScriptsDropped() {
        assertEquals("ControleurX", DeviceNames.defaultName("Contrôleur X", List.of()));
        assertEquals("Device12", DeviceNames.defaultName("手柄 12", List.of()));
    }

    /** The point of steps 3 and 4: the offered name is never one the user would then be told is invalid. */
    @Test
    void everyDefaultPassesValidation() {
        List<String> reported = List.of("Virpil Controls 20220720", "T-Rudder", "3Dconnexion SpaceMouse", "G9",
                WARTHOG, "", "!!!", "Contrôleur", "x", "GamePad", "SaitekX56Joystick", "12345");
        for (String name : reported) {
            String offered = DeviceNames.defaultName(name, frontier);
            assertTrue(DeviceNames.validate(offered, frontier).valid(), name + " offered " + offered);
        }
    }

    // --- validation -------------------------------------------------------------------------------------

    @Test
    void aTypedNameMayUseDashAndUnderscore() {
        assertTrue(DeviceNames.validate("Left_Stick-2", frontier).valid());
    }

    @Test
    void lengthIsThreeToFifty() {
        assertEquals(Problem.TOO_SHORT, DeviceNames.validate("Ab", List.of()).problem());
        assertEquals(Problem.TOO_SHORT, DeviceNames.validate(null, List.of()).problem());
        assertTrue(DeviceNames.validate("Abc", List.of()).valid());
        assertTrue(DeviceNames.validate("A".repeat(50), List.of()).valid());
        assertEquals(Problem.TOO_LONG, DeviceNames.validate("A".repeat(51), List.of()).problem());
    }

    @Test
    void itMustStartWithALetter() {
        assertEquals(Problem.NOT_STARTING_WITH_LETTER, DeviceNames.validate("3D-Mouse", List.of()).problem());
        assertEquals(Problem.NOT_STARTING_WITH_LETTER, DeviceNames.validate("_Stick", List.of()).problem());
    }

    /** A space or a period is valid in neither an XML tag nor a filename stem the game expects. */
    @Test
    void spacesPeriodsAndNonAsciiLettersAreRefused() {
        assertEquals(Problem.INVALID_CHARACTER, DeviceNames.validate("My Stick", List.of()).problem());
        assertEquals(Problem.INVALID_CHARACTER, DeviceNames.validate("My.Stick", List.of()).problem());
        assertEquals(Problem.INVALID_CHARACTER, DeviceNames.validate("Contrôleur", List.of()).problem());
    }

    @Test
    void aClashNamesTheNameItCollidesWith() {
        Verdict verdict = DeviceNames.validate("trudder", frontier);

        assertEquals(Problem.CLASHES, verdict.problem());
        assertEquals("T-Rudder", verdict.clashesWith());
    }

    @Test
    void aValidVerdictCarriesNoClash() {
        Verdict verdict = DeviceNames.validate("LVWAP", List.of("RVWAP"));

        assertTrue(verdict.valid());
        assertNull(verdict.clashesWith());
    }

    @Test
    void sameNameIgnoresCaseDashAndUnderscore() {
        assertTrue(DeviceNames.sameName("T-Rudder", "t_rudder"));
        assertTrue(DeviceNames.sameName("LVWAP", "lvwap"));
        assertFalse(DeviceNames.sameName("LVWAP", "RVWAP"));
    }

    // --- who onboarding asks ----------------------------------------------------------------------------

    @Test
    void onlyAttachedControllersNamedNowhereNeedAName() {
        MyDevice newStick = new MyDevice("VIRPIL Controls 20220720", null, "3344", "83F4", true, null, false);
        MyDevice hasEntry = new MyDevice("VIRPIL", "LVWAP", "3344", "03F5", true, null, false);
        MyDevice hasAlias = new MyDevice("VIRPIL", null, "3344", "0001", true, "RVWAP", false);
        MyDevice builtIn = new MyDevice("T-Rudder", null, "044F", "B679", true, null, true);
        MyDevice unplugged = new MyDevice(null, null, "3344", "0002", false, null, false);

        List<MyDevice> asked = DeviceNames.needingName(
                List.of(newStick, hasEntry, hasAlias, builtIn, unplugged));

        assertEquals(List.of(newStick), asked);
    }

    @Test
    void theOrderOfTheListIsKept() {
        MyDevice first = new MyDevice("A", null, "0001", "0001", true, null, false);
        MyDevice second = new MyDevice("B", null, "0002", "0002", true, null, false);

        assertEquals(List.of(first, second), DeviceNames.needingName(List.of(first, second)));
        assertTrue(DeviceNames.needingName(List.of()).isEmpty());
    }
}
